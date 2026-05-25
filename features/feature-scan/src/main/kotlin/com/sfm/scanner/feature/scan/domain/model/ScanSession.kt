package com.sfm.scanner.feature.scan.domain.model

import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.camera.FrameRecord
import java.io.File

/**
 * Complete scan session — handed from [ScanViewModel] to [PackagingViewModel] via [ScanSessionHolder].
 *
 * Field mapping to data_contracts.md §1.9:
 *  - [sessionUUID]: matches spec
 *  - [formData]: spec uses `FormData` (feature-form); we use [FormDataSnapshot] per deviation E-1
 *  - [frames]: spec uses [FrameRecord]; same type — imported from data-camera (deviation D-1/L)
 *  - [measurement]: spec uses [ARMeasurement]; same type — imported from data-ar (deviation D-1/R)
 *  - [scanStartTimestampMs]: matches spec
 *
 * Additional fields not in §1.9 but required for `details.json` (§6.2):
 *  - [appVersion]: from `PackageManager.getPackageInfo(...).versionName`
 *  - [deviceInfo]: from `Build.MANUFACTURER + Build.MODEL + Build.VERSION.RELEASE`
 *  - [sessionDir]: temp directory containing the frame JPEGs (deleted after packaging)
 */
internal data class ScanSession(
    val sessionUUID: String,
    val formData: FormDataSnapshot,
    val frames: List<FrameRecord>,
    val measurement: ARMeasurement,
    val scanStartTimestampMs: Long,
    val appVersion: String,
    val deviceInfo: String,
    val sessionDir: File,
)
