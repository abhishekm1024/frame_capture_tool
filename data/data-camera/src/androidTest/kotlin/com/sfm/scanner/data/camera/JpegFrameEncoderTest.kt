package com.sfm.scanner.data.camera

import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests [JpegFrameEncoder.encode] end-to-end.
 * Requires Android runtime for [android.graphics.YuvImage.compressToJpeg].
 */
@RunWith(AndroidJUnit4::class)
class JpegFrameEncoderTest {

    private val encoder = JpegFrameEncoder(quality = JPEG_QUALITY)

    // Small 4×4 image to keep test fast
    private val w = 4
    private val h = 4

    @Test
    fun encode_produces_non_empty_bytes() {
        val bytes = encoder.encode(makeGrayFrame(w, h))
        assertTrue("JPEG output must be non-empty", bytes.isNotEmpty())
    }

    @Test
    fun encode_output_starts_with_jpeg_soi_marker() {
        val bytes = encoder.encode(makeGrayFrame(w, h))
        assertTrue("First byte must be 0xFF (JPEG SOI)", bytes[0] == 0xFF.toByte())
        assertTrue("Second byte must be 0xD8 (JPEG SOI)", bytes[1] == 0xD8.toByte())
    }

    @Test
    fun encode_output_ends_with_jpeg_eoi_marker() {
        val bytes = encoder.encode(makeGrayFrame(w, h))
        val last = bytes.size - 1
        assertTrue("Second-to-last byte must be 0xFF (JPEG EOI)", bytes[last - 1] == 0xFF.toByte())
        assertTrue("Last byte must be 0xD9 (JPEG EOI)", bytes[last] == 0xD9.toByte())
    }

    @Test
    fun encode_frame_size_is_within_expected_bounds() {
        // 4×4 JPEG at Q95 should be well under 1KB
        val bytes = encoder.encode(makeGrayFrame(w, h))
        assertTrue("JPEG output must be positive size", bytes.size > 0)
        assertTrue("JPEG output must be less than 1KB for 4×4 frame", bytes.size < 1024)
    }

    /**
     * Constructs a planar I420 (pixelStride=1) YuvFrame filled with mid-gray.
     */
    private fun makeGrayFrame(width: Int, height: Int): YuvFrame {
        val ySize = width * height
        val uvSize = ySize / 4
        return YuvFrame(
            yBytes = ByteArray(ySize) { 128.toByte() },
            uBytes = ByteArray(uvSize) { 128.toByte() },
            vBytes = ByteArray(uvSize) { 128.toByte() },
            pixelStride = 1,
            width = width,
            height = height,
        )
    }
}
