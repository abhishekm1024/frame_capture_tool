package com.sfm.scanner.data.camera

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow

/**
 * Contract for continuous image frame capture.
 *
 * Design deviations from data_contracts.md §6.1:
 *  D-1: Interface lives in data-camera (not feature-scan) to satisfy architecture.md §12.
 *  D-2: [startCapture] accepts [LifecycleOwner] because CameraX requires it for
 *       [androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle].
 *  D-3: `FrameRecord.absolutePath` from camera layer is the filename only; full path
 *       is filled in by the consumer (ScanViewModel) after disk write.
 *  D-5: [startCapture] accepts an optional [Preview.SurfaceProvider] so the consumer
 *       can render a live camera preview alongside frame analysis (required by ScanScreen).
 *  D-6: [setTorch] added for runtime torch control during scan (screen_specs §5.7).
 */
interface CameraRepository {
    /**
     * Starts CameraX capture bound to [lifecycleOwner].
     *
     * If [previewSurfaceProvider] is non-null, a `Preview` use case is also bound and
     * its surface provider is set, enabling a live preview alongside frame analysis.
     */
    fun startCapture(
        lifecycleOwner: LifecycleOwner,
        config: CameraConfig,
        previewSurfaceProvider: Preview.SurfaceProvider? = null,
    ): Flow<FrameResult>

    suspend fun stopCapture()

    /**
     * Toggles the camera torch. Returns true if the call was issued (camera is bound),
     * false if no camera is currently bound.
     *
     * Per screen_specs §5.7 / TD-15: if torch is ON at scan start, the caller (ScanViewModel)
     * is responsible for not calling [setTorch] during a scan to maintain torch state.
     */
    suspend fun setTorch(on: Boolean): Boolean
}

data class CameraConfig(
    val targetFps: Int = TARGET_FPS,
    val preferredWidth: Int = PREFERRED_WIDTH,
    val preferredHeight: Int = PREFERRED_HEIGHT,
    val jpegQuality: Int = JPEG_QUALITY,
    val lockFocus: Boolean = true,
    val lockExposure: Boolean = true,
    /**
     * D-7: ARCore SharedCamera wiring (feature-scan M9). When non-null,
     * CameraX selects the camera with this Camera2 camera ID instead of the default
     * back camera. Required so CameraX shares the camera session opened by ARCore.
     */
    val preferredCameraId: String? = null,
)

sealed class FrameResult {
    data class Frame(val record: FrameRecord, val jpegBytes: ByteArray) : FrameResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Frame) return false
            return record == other.record && jpegBytes.contentEquals(other.jpegBytes)
        }
        override fun hashCode(): Int = 31 * record.hashCode() + jpegBytes.contentHashCode()
    }
    data class Error(val cause: Throwable) : FrameResult()
}
