package com.sfm.scanner.data.camera

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow

/**
 * Contract for continuous image frame capture.
 *
 * Design deviation from data_contracts.md §6.1:
 * - Interface lives in data-camera (not feature-scan) to satisfy architecture.md §12
 *   (data-camera cannot depend on feature-scan).
 * - [startCapture] accepts [LifecycleOwner] because CameraX requires it for
 *   [androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle].
 */
interface CameraRepository {
    fun startCapture(lifecycleOwner: LifecycleOwner, config: CameraConfig): Flow<FrameResult>
    suspend fun stopCapture()
}

data class CameraConfig(
    val targetFps: Int = TARGET_FPS,
    val preferredWidth: Int = PREFERRED_WIDTH,
    val preferredHeight: Int = PREFERRED_HEIGHT,
    val jpegQuality: Int = JPEG_QUALITY,
    val lockFocus: Boolean = true,
    val lockExposure: Boolean = true,
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
