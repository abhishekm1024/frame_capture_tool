package com.sfm.scanner.data.ar

/**
 * Internal state of the AR measurement pipeline.
 * Not exposed outside data-ar; [ARMeasurement] is the public output.
 */
internal sealed class MeasurementState {
    data object Idle : MeasurementState()
    data object AwaitingPointA : MeasurementState()
    data class PointACaptured(
        val pointA: ArPoint,
        val poseA: CameraPose,
        val hitType: HitType,
    ) : MeasurementState()
    data class Complete(val measurement: ARMeasurement) : MeasurementState()
}

/**
 * Extracted hit data with no ARCore SDK types.
 * Populated by [ArMeasurementPipeline] from [com.google.ar.core.HitResult].
 */
internal data class HitTypeAndPosition(
    val hitType: HitType,
    val position: ArPoint,
    val cameraPose: CameraPose,
)
