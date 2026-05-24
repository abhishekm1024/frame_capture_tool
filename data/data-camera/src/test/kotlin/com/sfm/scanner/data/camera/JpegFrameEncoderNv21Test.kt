package com.sfm.scanner.data.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests [JpegFrameEncoder.Companion.buildNv21] NV21 byte-construction logic.
 *
 * These tests run on the JVM without Android runtime because [buildNv21] operates
 * purely on [ByteArray] values. The full JPEG encoding test ([JpegFrameEncoderTest])
 * runs on device/emulator because [android.graphics.YuvImage] requires Android runtime.
 */
class JpegFrameEncoderNv21Test {

    // 4×2 image: W=4, H=2 → Y=8 bytes, U=2 bytes, V=2 bytes, NV21=8+4=12 bytes
    private val w = 4
    private val h = 2
    private val ySize = w * h      // 8
    private val uvSize = ySize / 2 // 4

    // ── Planar (pixelStride == 1) ────────────────────────────────────────────

    @Test
    fun `planar - Y plane copied correctly to NV21 head`() {
        val yuv = planarFrame(
            y = ByteArray(ySize) { (it + 10).toByte() },
            u = ByteArray(2) { 0x11 },
            v = ByteArray(2) { 0x22 },
        )
        val nv21 = JpegFrameEncoder.buildNv21(yuv)
        for (i in 0 until ySize) {
            assertEquals("Y[$i]", (i + 10).toByte(), nv21[i])
        }
    }

    @Test
    fun `planar - UV interleaved as VU in NV21`() {
        val yuv = planarFrame(
            y = ByteArray(ySize) { 128.toByte() },
            u = byteArrayOf(0xAA.toByte(), 0xBB.toByte()),
            v = byteArrayOf(0xCC.toByte(), 0xDD.toByte()),
        )
        val nv21 = JpegFrameEncoder.buildNv21(yuv)
        // NV21 UV area: V₀ U₀ V₁ U₁ ...
        assertEquals(0xCC.toByte(), nv21[ySize + 0])  // V₀
        assertEquals(0xAA.toByte(), nv21[ySize + 1])  // U₀
        assertEquals(0xDD.toByte(), nv21[ySize + 2])  // V₁
        assertEquals(0xBB.toByte(), nv21[ySize + 3])  // U₁
    }

    @Test
    fun `planar - output has correct total length`() {
        val yuv = planarFrame(
            y = ByteArray(ySize),
            u = ByteArray(2),
            v = ByteArray(2),
        )
        val nv21 = JpegFrameEncoder.buildNv21(yuv)
        assertEquals(ySize + uvSize, nv21.size)
    }

    // ── Semi-planar (pixelStride == 2) ───────────────────────────────────────

    @Test
    fun `semi-planar - Y plane copied correctly to NV21 head`() {
        val yuv = semiPlanarFrame(
            y = ByteArray(ySize) { (it + 20).toByte() },
            vBuffer = ByteArray(uvSize) { 0x55 },
        )
        val nv21 = JpegFrameEncoder.buildNv21(yuv)
        for (i in 0 until ySize) {
            assertEquals("Y[$i]", (i + 20).toByte(), nv21[i])
        }
    }

    @Test
    fun `semi-planar - vBytes pasted directly as NV21 UV component`() {
        // vBytes IS the full VU interleaved buffer in NV21 layout
        val vuInterleaved = byteArrayOf(0xC1.toByte(), 0xA1.toByte(), 0xC2.toByte(), 0xA2.toByte())
        val yuv = semiPlanarFrame(
            y = ByteArray(ySize) { 128.toByte() },
            vBuffer = vuInterleaved,
        )
        val nv21 = JpegFrameEncoder.buildNv21(yuv)
        for (i in 0 until uvSize) {
            assertEquals("UV[$i]", vuInterleaved[i], nv21[ySize + i])
        }
    }

    @Test
    fun `semi-planar - output has correct total length`() {
        val yuv = semiPlanarFrame(
            y = ByteArray(ySize),
            vBuffer = ByteArray(uvSize),
        )
        val nv21 = JpegFrameEncoder.buildNv21(yuv)
        assertEquals(ySize + uvSize, nv21.size)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun planarFrame(y: ByteArray, u: ByteArray, v: ByteArray) = YuvFrame(
        yBytes = y,
        uBytes = u,
        vBytes = v,
        pixelStride = 1,
        width = w,
        height = h,
    )

    private fun semiPlanarFrame(y: ByteArray, vBuffer: ByteArray) = YuvFrame(
        yBytes = y,
        uBytes = ByteArray(0),  // not used for semi-planar
        vBytes = vBuffer,
        pixelStride = 2,
        width = w,
        height = h,
    )
}
