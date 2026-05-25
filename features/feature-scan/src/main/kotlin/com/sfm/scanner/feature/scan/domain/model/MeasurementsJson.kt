package com.sfm.scanner.feature.scan.domain.model

import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArPoint
import com.sfm.scanner.data.ar.CameraPose
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Exact `measurements.json` schema per data_contracts.md §2.2.
 *
 * Nullable fields (pointB, distanceMeters, cameraPoseB, pointA, cameraPoseA) MUST appear as
 * JSON `null` rather than be omitted, satisfied by including them in this @Serializable class.
 */
@Serializable
internal data class MeasurementsJson(
    val pointA: PointJson?,
    val pointB: PointJson?,
    val distanceMeters: Float?,
    val cameraPoseA: PoseJson?,
    val cameraPoseB: PoseJson?,
    val trackingState: String,
    val hitType: String,
)

@Serializable
internal data class PointJson(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
)

@Serializable
internal data class PoseJson(
    val tx: Float,
    val ty: Float,
    val tz: Float,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val qw: Float,
)

internal fun ArPoint.toJson(): PointJson = PointJson(x = x, y = y, z = z, timestamp = timestampMs)

internal fun CameraPose.toJson(): PoseJson =
    PoseJson(tx = tx, ty = ty, tz = tz, qx = qx, qy = qy, qz = qz, qw = qw)

internal fun ARMeasurement.toJson(): MeasurementsJson = MeasurementsJson(
    pointA = pointA?.toJson(),
    pointB = pointB?.toJson(),
    distanceMeters = distanceMeters,
    cameraPoseA = cameraPoseA?.toJson(),
    cameraPoseB = cameraPoseB?.toJson(),
    trackingState = trackingState.name,
    hitType = hitType.name,
)
