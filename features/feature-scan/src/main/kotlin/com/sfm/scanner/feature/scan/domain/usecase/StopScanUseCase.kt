package com.sfm.scanner.feature.scan.domain.usecase

import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArTrackingState
import com.sfm.scanner.data.ar.HitType
import com.sfm.scanner.data.camera.FrameRecord
import com.sfm.scanner.feature.scan.domain.model.FormDataSnapshot
import com.sfm.scanner.feature.scan.domain.model.ScanSession
import java.io.File
import javax.inject.Inject

/**
 * Pure assembly of [ScanSession] from accumulated scan data.
 *
 * The use case has no side effects — all data is supplied by the caller (ScanViewModel).
 * Fully unit-testable on the JVM with no Android dependencies.
 */
internal class StopScanUseCase @Inject constructor() {

    fun buildSession(
        sessionUuid: String,
        formData: FormDataSnapshot,
        frames: List<FrameRecord>,
        measurement: ARMeasurement?,
        scanStartTimestampMs: Long,
        appVersion: String,
        deviceInfo: String,
        sessionDir: File,
    ): ScanSession {
        // measurement may be null if AR never produced a single update (Tracking never reached).
        // Fall back to an "empty" measurement so the ZIP always contains a valid measurements.json
        // (data_contracts §3.4: details.json and measurements.json always present).
        val nonNullMeasurement = measurement ?: ARMeasurement(
            trackingState = ArTrackingState.PAUSED,
            hitType = HitType.NONE,
        )
        return ScanSession(
            sessionUUID = sessionUuid,
            formData = formData,
            frames = frames,
            measurement = nonNullMeasurement,
            scanStartTimestampMs = scanStartTimestampMs,
            appVersion = appVersion,
            deviceInfo = deviceInfo,
            sessionDir = sessionDir,
        )
    }
}
