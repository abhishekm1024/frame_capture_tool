package com.sfm.scanner.data.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class TranslationCalculatorTest {

    private fun pose(tx: Float, ty: Float, tz: Float) = CameraPose(tx, ty, tz, 0f, 0f, 0f, 1f)

    @Test
    fun `compute returns zero for identical poses`() {
        val pose = pose(1f, 2f, 3f)
        assertEquals(0f, TranslationCalculator.compute(pose, pose), 1e-6f)
    }

    @Test
    fun `compute returns correct Euclidean distance for X-only translation`() {
        val poseA = pose(0f, 0f, 0f)
        val poseB = pose(0.30f, 0f, 0f)
        assertEquals(0.30f, TranslationCalculator.compute(poseA, poseB), 1e-5f)
    }

    @Test
    fun `compute returns correct distance for 3D translation`() {
        // 3-4-5 right triangle in XY; z=0
        val poseA = pose(0f, 0f, 0f)
        val poseB = pose(0.3f, 0.4f, 0f)
        val expected = sqrt(0.3f * 0.3f + 0.4f * 0.4f)
        assertEquals(expected, TranslationCalculator.compute(poseA, poseB), 1e-5f)
    }

    @Test
    fun `compute is symmetric`() {
        val poseA = pose(1f, 2f, 3f)
        val poseB = pose(4f, 5f, 6f)
        assertEquals(
            TranslationCalculator.compute(poseA, poseB),
            TranslationCalculator.compute(poseB, poseA),
            1e-6f,
        )
    }

    @Test
    fun `exceedsThreshold returns false when below 0_30m`() {
        val poseA = pose(0f, 0f, 0f)
        val poseB = pose(0.29f, 0f, 0f)
        assertFalse(TranslationCalculator.exceedsThreshold(poseA, poseB))
    }

    @Test
    fun `exceedsThreshold returns true at exactly 0_30m`() {
        val poseA = pose(0f, 0f, 0f)
        val poseB = pose(0.30f, 0f, 0f)
        assertTrue(TranslationCalculator.exceedsThreshold(poseA, poseB))
    }

    @Test
    fun `exceedsThreshold returns true above threshold`() {
        val poseA = pose(0f, 0f, 0f)
        val poseB = pose(0.5f, 0.5f, 0.5f)
        assertTrue(TranslationCalculator.exceedsThreshold(poseA, poseB))
    }
}
