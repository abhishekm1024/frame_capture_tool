package com.sfm.scanner.feature.scan.infra

import android.util.Log
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.data.camera.FrameRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Bounded asynchronous frame writer for JPEG frames.
 *
 * Architectural requirements (M9):
 *   - Capture cadence must NOT depend on disk write latency (exact 5 FPS preserved).
 *   - Bounded queue ([QUEUE_CAPACITY] = 4): no unbounded buffering / no OOM risk.
 *   - Bounded writer pool ([WORKER_COUNT] = 2): parallel writes for higher sustained
 *     throughput than a single sequential writer.
 *   - Explicit backpressure: [BufferOverflow.DROP_OLDEST] — when the queue is full,
 *     the oldest queued frame is dropped (preserves more recent frames).
 *   - Drops are silent at the channel level; observable via the gap between
 *     [submittedCount] and emissions of [frameWritten].
 *
 * Memory budget (peak):
 *   - In queue: up to QUEUE_CAPACITY × ~0.6 MB JPEG = ~2.4 MB
 *   - In-flight in workers: up to WORKER_COUNT × ~0.6 MB = ~1.2 MB
 *   - Total: ~3.6 MB above baseline. Negligible for OOM (well under the per-app heap).
 *
 * Lifecycle:
 *   - [start] launches workers on the given [CoroutineScope] (caller owns the scope —
 *     typically `viewModelScope`).
 *   - [submit] is non-blocking (safe to call from camera-flow collector hot path).
 *   - [stopAndDrain] closes the channel, drains remaining items, and joins workers.
 */
internal class FrameWriter(
    private val sessionDir: File,
    private val dispatchers: AppDispatchers,
) {

    private val queue = Channel<FrameToWrite>(
        capacity = QUEUE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val workerJobs = mutableListOf<Job>()
    private val started = AtomicInteger(0)

    private val _frameWritten = MutableSharedFlow<FrameRecord>(extraBufferCapacity = 16)
    val frameWritten: SharedFlow<FrameRecord> = _frameWritten.asSharedFlow()

    private val _submittedCount = AtomicInteger(0)
    /** Number of frames offered to [submit] (regardless of whether they were dropped). */
    val submittedCount: Int get() = _submittedCount.get()

    private val _writtenCount = AtomicInteger(0)
    /** Number of frames actually written to disk. */
    val writtenCount: Int get() = _writtenCount.get()

    fun start(scope: CoroutineScope) {
        check(started.compareAndSet(0, 1)) { "FrameWriter already started" }
        repeat(WORKER_COUNT) {
            workerJobs += scope.launch(dispatchers.io) {
                for (frameToWrite in queue) {
                    runCatching { writeFrame(frameToWrite) }
                        .onFailure { e ->
                            Log.e(LOG_TAG, "Frame write failed: ${e.message}", e)
                        }
                }
            }
        }
    }

    /**
     * Submits a frame for asynchronous writing. Non-blocking.
     *
     * If the bounded queue is full, the oldest queued frame is silently dropped
     * (DROP_OLDEST policy). The caller's hot path (camera-flow collector) is never blocked.
     */
    fun submit(record: FrameRecord, jpegBytes: ByteArray) {
        _submittedCount.incrementAndGet()
        queue.trySend(FrameToWrite(record, jpegBytes))
    }

    /**
     * Closes the queue (no more submissions accepted) and waits for all workers to
     * drain remaining items. Safe to call once.
     */
    suspend fun stopAndDrain() {
        queue.close()
        workerJobs.joinAll()
    }

    private suspend fun writeFrame(frameToWrite: FrameToWrite) {
        val file = File(sessionDir, frameToWrite.record.filename)
        file.writeBytes(frameToWrite.jpegBytes)
        val fullRecord = frameToWrite.record.copy(absolutePath = file.absolutePath)
        _writtenCount.incrementAndGet()
        _frameWritten.tryEmit(fullRecord)
    }

    private data class FrameToWrite(val record: FrameRecord, val jpegBytes: ByteArray)

    companion object {
        const val QUEUE_CAPACITY = 4
        const val WORKER_COUNT = 2
        private const val LOG_TAG = "FrameWriter"
    }
}
