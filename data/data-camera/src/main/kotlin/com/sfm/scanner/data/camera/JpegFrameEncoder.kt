package com.sfm.scanner.data.camera

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import java.io.ByteArrayOutputStream

/**
 * Encodes a [YuvFrame] (YUV_420_888) to a JPEG [ByteArray] at the specified quality.
 *
 * Conversion path: YuvFrame → NV21 byte array → [YuvImage.compressToJpeg].
 *
 * [buildNv21] is exposed as an internal companion function so its byte-manipulation
 * logic can be unit-tested on the JVM without requiring Android runtime.
 */
internal class JpegFrameEncoder(private val quality: Int = JPEG_QUALITY) {

    fun encode(yuv: YuvFrame): ByteArray {
        val nv21 = buildNv21(yuv)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, yuv.width, yuv.height, null)
        val out = ByteArrayOutputStream()
        check(yuvImage.compressToJpeg(Rect(0, 0, yuv.width, yuv.height), quality, out)) {
            "YuvImage.compressToJpeg returned false for ${yuv.width}×${yuv.height} at Q$quality"
        }
        return out.toByteArray()
    }

    internal companion object {

        /**
         * Converts a [YuvFrame] to a compact NV21 byte array (Y + interleaved VU).
         *
         * Two paths based on [YuvFrame.pixelStride]:
         *
         * - pixelStride == 2 (semi-planar NV21/NV12): [YuvFrame.vBytes] IS the full
         *   interleaved VU buffer from the V-plane position.  On virtually all Android
         *   camera hardware that delivers NV21, this buffer starts at the V byte, so
         *   concatenating yBytes + vBytes produces valid NV21.
         *
         * - pixelStride == 1 (planar I420): manually interleave V and U values to form
         *   the NV21 VU plane.
         */
        fun buildNv21(yuv: YuvFrame): ByteArray {
            val ySize = yuv.width * yuv.height
            val uvSize = ySize / 2
            val nv21 = ByteArray(ySize + uvSize)

            // Y plane — already compact (row-stride padding stripped during copy)
            System.arraycopy(yuv.yBytes, 0, nv21, 0, minOf(yuv.yBytes.size, ySize))

            if (yuv.pixelStride == 2) {
                // Semi-planar: vBytes buffer = full VU interleaved area (NV21 V-first layout)
                System.arraycopy(yuv.vBytes, 0, nv21, ySize, minOf(yuv.vBytes.size, uvSize))
            } else {
                // Planar I420: interleave V then U for NV21 output
                val uvPairs = uvSize / 2
                for (i in 0 until uvPairs) {
                    nv21[ySize + i * 2] = if (i < yuv.vBytes.size) yuv.vBytes[i] else 0
                    nv21[ySize + i * 2 + 1] = if (i < yuv.uBytes.size) yuv.uBytes[i] else 0
                }
            }
            return nv21
        }
    }
}
