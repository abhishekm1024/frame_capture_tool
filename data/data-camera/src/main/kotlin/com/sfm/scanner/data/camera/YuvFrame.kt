package com.sfm.scanner.data.camera

/**
 * Holds copied YUV plane data extracted from an [androidx.camera.core.ImageProxy].
 *
 * The ImageProxy is closed immediately after copying, releasing the camera buffer.
 * This struct owns the data until JPEG encoding completes.
 *
 * [pixelStride] of the U/V planes indicates layout:
 *   1 = planar I420 (separate Y, U, V planes)
 *   2 = semi-planar NV21/NV12 (Y plane + interleaved UV plane)
 *
 * For semi-planar, [vBytes] is the full interleaved VU buffer from the V-plane position,
 * which is directly the NV21 UV component on most Android camera hardware.
 */
internal data class YuvFrame(
    val yBytes: ByteArray,
    val uBytes: ByteArray,
    val vBytes: ByteArray,
    val pixelStride: Int,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is YuvFrame) return false
        return pixelStride == other.pixelStride &&
            width == other.width &&
            height == other.height &&
            yBytes.contentEquals(other.yBytes) &&
            uBytes.contentEquals(other.uBytes) &&
            vBytes.contentEquals(other.vBytes)
    }

    override fun hashCode(): Int {
        var result = yBytes.contentHashCode()
        result = 31 * result + uBytes.contentHashCode()
        result = 31 * result + vBytes.contentHashCode()
        result = 31 * result + pixelStride
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
