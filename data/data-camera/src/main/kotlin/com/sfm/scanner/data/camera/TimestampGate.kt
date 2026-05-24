package com.sfm.scanner.data.camera

import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe gate that accepts at most one event per [intervalMs] milliseconds.
 *
 * Uses a CAS on [AtomicLong] for correctness under concurrent callers. In practice,
 * [STRATEGY_KEEP_ONLY_LATEST] on ImageAnalysis serialises calls to [shouldAccept],
 * so the CAS always succeeds on the fast path.
 */
internal class TimestampGate(private val intervalMs: Long = FRAME_INTERVAL_MS) {

    // Initialise to -intervalMs so the very first frame (even at t=0) is accepted.
    private val lastAcceptedMs = AtomicLong(-intervalMs)

    fun shouldAccept(nowMs: Long): Boolean {
        val last = lastAcceptedMs.get()
        if (nowMs - last < intervalMs) return false
        return lastAcceptedMs.compareAndSet(last, nowMs)
    }

    fun reset() {
        lastAcceptedMs.set(-intervalMs)
    }
}
