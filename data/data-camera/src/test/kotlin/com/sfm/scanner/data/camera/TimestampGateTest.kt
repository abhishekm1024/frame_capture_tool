package com.sfm.scanner.data.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimestampGateTest {

    private lateinit var gate: TimestampGate

    @Before
    fun setUp() {
        gate = TimestampGate(intervalMs = FRAME_INTERVAL_MS)
    }

    @Test
    fun `first call is always accepted regardless of timestamp`() {
        // Works at t=0 because lastAcceptedMs is initialised to -intervalMs
        assertTrue(gate.shouldAccept(0L))
    }

    @Test
    fun `first call at arbitrary positive timestamp is accepted`() {
        assertTrue(gate.shouldAccept(1_000L))
    }

    @Test
    fun `call within interval is rejected`() {
        gate.shouldAccept(1_000L)
        assertFalse(gate.shouldAccept(1_199L))
    }

    @Test
    fun `call at exact interval boundary is accepted`() {
        gate.shouldAccept(1_000L)
        assertTrue(gate.shouldAccept(1_200L))
    }

    @Test
    fun `call one millisecond after boundary is accepted`() {
        gate.shouldAccept(1_000L)
        assertTrue(gate.shouldAccept(1_201L))
    }

    @Test
    fun `reset allows immediate acceptance`() {
        gate.shouldAccept(1_000L)
        gate.reset()
        assertTrue(gate.shouldAccept(1_001L))
    }

    @Test
    fun `simulates exact 5fps from 30fps stream — accepts one per 200ms window`() {
        // Simulate a 30 FPS stream delivering 30 frames over 1000 ms (every ~33 ms).
        // A 200 ms gate should accept exactly 5 of them.
        val frameIntervalMs = 33L
        var accepted = 0
        for (frame in 0 until 30) {
            val nowMs = frame * frameIntervalMs
            if (gate.shouldAccept(nowMs)) accepted++
        }
        assertEquals(5, accepted)
    }

    @Test
    fun `sequential calls one interval apart are all accepted`() {
        var accepted = 0
        for (i in 0 until 10) {
            val nowMs = (i * FRAME_INTERVAL_MS) + 500L
            if (gate.shouldAccept(nowMs)) accepted++
        }
        assertEquals(10, accepted)
    }
}
