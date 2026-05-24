package com.sfm.scanner.data.ar

/**
 * Pure state machine reducer for AR measurement progression.
 *
 * All parameters are data-ar domain types; zero ARCore SDK dependency.
 * Fully testable on JVM without device or ARCore runtime.
 *
 * Returns [ReducerOutput] containing the next state, an optional [ARMeasurement] to emit,
 * and the updated [newConsecutiveNoHitCount] for the caller to store.
 */
internal object MeasurementStateReducer {

    data class ReducerOutput(
        val nextState: MeasurementState,
        val emission: ARMeasurement?,
        val newConsecutiveNoHitCount: Int,
    )

    fun reduce(
        current: MeasurementState,
        trackingState: ArTrackingState,
        hit: HitTypeAndPosition?,
        currentCameraPose: CameraPose,
        consecutiveNoHitCount: Int,
        nowMs: Long,
    ): ReducerOutput = when (current) {

        is MeasurementState.Idle ->
            ReducerOutput(current, null, consecutiveNoHitCount)

        is MeasurementState.AwaitingPointA ->
            reduceAwaitingPointA(trackingState, hit, consecutiveNoHitCount)

        is MeasurementState.PointACaptured ->
            reducePointACaptured(current, trackingState, currentCameraPose, nowMs)

        is MeasurementState.Complete ->
            ReducerOutput(current, current.measurement, 0)
    }

    private fun reduceAwaitingPointA(
        trackingState: ArTrackingState,
        hit: HitTypeAndPosition?,
        consecutiveNoHitCount: Int,
    ): ReducerOutput {
        if (trackingState != ArTrackingState.TRACKING) {
            return ReducerOutput(
                nextState = MeasurementState.AwaitingPointA,
                emission = ARMeasurement(trackingState = trackingState, hitType = HitType.NONE),
                newConsecutiveNoHitCount = 0,
            )
        }

        if (hit == null) {
            val newCount = consecutiveNoHitCount + 1
            return if (newCount >= AR_RAYCAST_RETRY_BUDGET) {
                // Budget exhausted; callers should log this condition.
                ReducerOutput(
                    nextState = MeasurementState.AwaitingPointA,
                    emission = ARMeasurement(trackingState = ArTrackingState.TRACKING, hitType = HitType.NONE),
                    newConsecutiveNoHitCount = 0,
                )
            } else {
                ReducerOutput(
                    nextState = MeasurementState.AwaitingPointA,
                    emission = null,
                    newConsecutiveNoHitCount = newCount,
                )
            }
        }

        val next = MeasurementState.PointACaptured(
            pointA = hit.position,
            poseA = hit.cameraPose,
            hitType = hit.hitType,
        )
        return ReducerOutput(
            nextState = next,
            emission = ARMeasurement(
                pointA = hit.position,
                cameraPoseA = hit.cameraPose,
                trackingState = ArTrackingState.TRACKING,
                hitType = hit.hitType,
            ),
            newConsecutiveNoHitCount = 0,
        )
    }

    private fun reducePointACaptured(
        current: MeasurementState.PointACaptured,
        trackingState: ArTrackingState,
        currentCameraPose: CameraPose,
        nowMs: Long,
    ): ReducerOutput {
        if (trackingState != ArTrackingState.TRACKING) {
            return ReducerOutput(
                nextState = current,
                emission = ARMeasurement(
                    pointA = current.pointA,
                    cameraPoseA = current.poseA,
                    trackingState = trackingState,
                    hitType = current.hitType,
                ),
                newConsecutiveNoHitCount = 0,
            )
        }

        if (!TranslationCalculator.exceedsThreshold(current.poseA, currentCameraPose)) {
            return ReducerOutput(
                nextState = current,
                emission = ARMeasurement(
                    pointA = current.pointA,
                    cameraPoseA = current.poseA,
                    trackingState = ArTrackingState.TRACKING,
                    hitType = current.hitType,
                ),
                newConsecutiveNoHitCount = 0,
            )
        }

        val pointB = ArPoint(
            x = currentCameraPose.tx,
            y = currentCameraPose.ty,
            z = currentCameraPose.tz,
            timestampMs = nowMs,
        )
        val distance = TranslationCalculator.compute(current.poseA, currentCameraPose)
        val measurement = ARMeasurement(
            pointA = current.pointA,
            pointB = pointB,
            distanceMeters = distance,
            cameraPoseA = current.poseA,
            cameraPoseB = currentCameraPose,
            trackingState = ArTrackingState.TRACKING,
            hitType = current.hitType,
        )
        return ReducerOutput(
            nextState = MeasurementState.Complete(measurement),
            emission = measurement,
            newConsecutiveNoHitCount = 0,
        )
    }
}
