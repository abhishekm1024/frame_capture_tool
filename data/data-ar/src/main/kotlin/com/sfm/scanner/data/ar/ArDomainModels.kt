package com.sfm.scanner.data.ar

/**
 * World-space 3D point captured by ARCore.
 * Module: data-ar (deviation D-1 from data_contracts.md §1.5 which lists feature-scan;
 * architecture §12 forbids data-ar → feature-scan).
 */
data class ArPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampMs: Long,
)

/**
 * Camera world-space pose at a moment in time.
 * Quaternion (qx, qy, qz, qw) in ARCore world space; norm ≈ 1.0.
 */
data class CameraPose(
    val tx: Float,
    val ty: Float,
    val tz: Float,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val qw: Float,
)

/**
 * Hit type from the center-pixel raycast, ordered by accuracy.
 */
enum class HitType {
    DEPTH,
    PLANE,
    INSTANT_PLACEMENT,
    FEATURE_POINT,
    NONE,
}

/**
 * Maps to ARCore's [com.google.ar.core.TrackingState].
 * Stored as string in measurements.json.
 */
enum class ArTrackingState {
    TRACKING,
    PAUSED,
    STOPPED,
}

/**
 * Accumulated AR measurement result emitted by [ArRepository.getMeasurementFlow].
 *
 * Fields are progressively filled: pointA after Point A capture, pointB + distanceMeters
 * after the camera has moved ≥ AR_TRANSLATION_THRESHOLD_METERS from poseA.
 */
data class ARMeasurement(
    val pointA: ArPoint? = null,
    val pointB: ArPoint? = null,
    val distanceMeters: Float? = null,
    val cameraPoseA: CameraPose? = null,
    val cameraPoseB: CameraPose? = null,
    val trackingState: ArTrackingState = ArTrackingState.PAUSED,
    val hitType: HitType = HitType.NONE,
)
