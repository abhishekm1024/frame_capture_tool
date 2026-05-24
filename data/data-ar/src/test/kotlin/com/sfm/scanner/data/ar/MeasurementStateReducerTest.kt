package com.sfm.scanner.data.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementStateReducerTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun pose(tx: Float = 0f, ty: Float = 0f, tz: Float = 0f) =
        CameraPose(tx, ty, tz, 0f, 0f, 0f, 1f)

    private fun point(x: Float = 0f, y: Float = 0f, z: Float = 0f) =
        ArPoint(x, y, z, timestampMs = 1000L)

    private fun hit(
        hitType: HitType = HitType.DEPTH,
        position: ArPoint = point(),
        cameraPose: CameraPose = pose(),
    ) = HitTypeAndPosition(hitType, position, cameraPose)

    private fun reduce(
        current: MeasurementState,
        trackingState: ArTrackingState = ArTrackingState.TRACKING,
        hit: HitTypeAndPosition? = null,
        currentPose: CameraPose = pose(),
        noHitCount: Int = 0,
    ) = MeasurementStateReducer.reduce(
        current = current,
        trackingState = trackingState,
        hit = hit,
        currentCameraPose = currentPose,
        consecutiveNoHitCount = noHitCount,
        nowMs = 1000L,
    )

    // ── Idle state ────────────────────────────────────────────────────────────

    @Test
    fun `idle state emits nothing and stays idle`() {
        val out = reduce(MeasurementState.Idle, hit = hit())
        assertTrue(out.nextState is MeasurementState.Idle)
        assertNull(out.emission)
    }

    // ── AwaitingPointA: tracking lost ─────────────────────────────────────────

    @Test
    fun `awaiting point A with tracking paused emits partial measurement and stays awaiting`() {
        val out = reduce(MeasurementState.AwaitingPointA, trackingState = ArTrackingState.PAUSED)
        assertTrue(out.nextState is MeasurementState.AwaitingPointA)
        assertNotNull(out.emission)
        assertEquals(ArTrackingState.PAUSED, out.emission!!.trackingState)
        assertEquals(0, out.newConsecutiveNoHitCount)
    }

    // ── AwaitingPointA: no hit within budget ──────────────────────────────────

    @Test
    fun `awaiting point A with no hit below budget emits nothing`() {
        val out = reduce(MeasurementState.AwaitingPointA, hit = null, noHitCount = 5)
        assertTrue(out.nextState is MeasurementState.AwaitingPointA)
        assertNull(out.emission)
        assertEquals(6, out.newConsecutiveNoHitCount)
    }

    @Test
    fun `awaiting point A with no hit at budget boundary emits NONE and resets counter`() {
        // count = AR_RAYCAST_RETRY_BUDGET - 1 means next frame (count+1) exhausts budget
        val out = reduce(
            MeasurementState.AwaitingPointA,
            hit = null,
            noHitCount = AR_RAYCAST_RETRY_BUDGET - 1,
        )
        assertTrue(out.nextState is MeasurementState.AwaitingPointA)
        assertNotNull(out.emission)
        assertEquals(HitType.NONE, out.emission!!.hitType)
        assertEquals(0, out.newConsecutiveNoHitCount)
    }

    @Test
    fun `retry counter resets on tracking loss`() {
        val out = reduce(
            MeasurementState.AwaitingPointA,
            trackingState = ArTrackingState.PAUSED,
            noHitCount = 25,
        )
        assertEquals(0, out.newConsecutiveNoHitCount)
    }

    // ── AwaitingPointA: hit found ─────────────────────────────────────────────

    @Test
    fun `awaiting point A with depth hit transitions to PointACaptured`() {
        val hitData = hit(hitType = HitType.DEPTH, position = point(0.1f, 0.2f, 1.0f))
        val out = reduce(MeasurementState.AwaitingPointA, hit = hitData)

        assertTrue(out.nextState is MeasurementState.PointACaptured)
        val captured = out.nextState as MeasurementState.PointACaptured
        assertEquals(HitType.DEPTH, captured.hitType)
        assertEquals(0.1f, captured.pointA.x)
        assertEquals(0, out.newConsecutiveNoHitCount)

        assertNotNull(out.emission)
        assertEquals(HitType.DEPTH, out.emission!!.hitType)
        assertNotNull(out.emission!!.pointA)
        assertNull(out.emission!!.pointB)
    }

    @Test
    fun `awaiting point A with plane hit transitions to PointACaptured with PLANE type`() {
        val hitData = hit(hitType = HitType.PLANE)
        val out = reduce(MeasurementState.AwaitingPointA, hit = hitData)
        val captured = out.nextState as MeasurementState.PointACaptured
        assertEquals(HitType.PLANE, captured.hitType)
    }

    // ── PointACaptured: tracking lost ─────────────────────────────────────────

    @Test
    fun `point A captured with tracking paused stays in PointACaptured and emits partial`() {
        val captured = MeasurementState.PointACaptured(
            pointA = point(0f, 0f, 0f),
            poseA = pose(0f, 0f, 0f),
            hitType = HitType.DEPTH,
        )
        val out = reduce(captured, trackingState = ArTrackingState.PAUSED)

        assertTrue(out.nextState is MeasurementState.PointACaptured)
        assertNotNull(out.emission)
        assertEquals(ArTrackingState.PAUSED, out.emission!!.trackingState)
        assertNotNull(out.emission!!.pointA)
        assertNull(out.emission!!.pointB)
    }

    // ── PointACaptured: below translation threshold ───────────────────────────

    @Test
    fun `point A captured below threshold stays in PointACaptured and emits partial`() {
        val captured = MeasurementState.PointACaptured(
            pointA = point(),
            poseA = pose(0f, 0f, 0f),
            hitType = HitType.DEPTH,
        )
        val out = reduce(captured, currentPose = pose(0.10f, 0f, 0f))

        assertTrue(out.nextState is MeasurementState.PointACaptured)
        assertNotNull(out.emission)
        assertNull(out.emission!!.pointB)
        assertNull(out.emission!!.distanceMeters)
    }

    // ── PointACaptured: threshold crossed ─────────────────────────────────────

    @Test
    fun `point A captured above threshold transitions to Complete with distanceMeters`() {
        val captured = MeasurementState.PointACaptured(
            pointA = point(),
            poseA = pose(0f, 0f, 0f),
            hitType = HitType.DEPTH,
        )
        val out = reduce(captured, currentPose = pose(0.40f, 0f, 0f))

        assertTrue(out.nextState is MeasurementState.Complete)
        assertNotNull(out.emission)
        assertNotNull(out.emission!!.pointB)
        assertNotNull(out.emission!!.distanceMeters)
        assertEquals(0.40f, out.emission!!.distanceMeters!!, 1e-5f)
    }

    // ── Complete state ────────────────────────────────────────────────────────

    @Test
    fun `complete state re-emits final measurement unchanged`() {
        val measurement = ARMeasurement(
            pointA = point(0f, 0f, 0f),
            pointB = point(0.4f, 0f, 0f),
            distanceMeters = 0.4f,
            trackingState = ArTrackingState.TRACKING,
            hitType = HitType.DEPTH,
        )
        val out = reduce(MeasurementState.Complete(measurement))

        assertTrue(out.nextState is MeasurementState.Complete)
        assertEquals(measurement, out.emission)
    }

    // ── Hit type priority ─────────────────────────────────────────────────────

    @Test
    fun `FEATURE_POINT hit type is preserved on transition`() {
        val hitData = hit(hitType = HitType.FEATURE_POINT)
        val out = reduce(MeasurementState.AwaitingPointA, hit = hitData)
        val captured = out.nextState as MeasurementState.PointACaptured
        assertEquals(HitType.FEATURE_POINT, captured.hitType)
    }

    @Test
    fun `hit type from Point A is preserved in completion measurement`() {
        val captured = MeasurementState.PointACaptured(
            pointA = point(),
            poseA = pose(0f, 0f, 0f),
            hitType = HitType.INSTANT_PLACEMENT,
        )
        val out = reduce(captured, currentPose = pose(0.5f, 0f, 0f))
        assertEquals(HitType.INSTANT_PLACEMENT, out.emission!!.hitType)
    }
}
