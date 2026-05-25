package com.sfm.scanner.feature.scan.domain.model

import kotlinx.serialization.Serializable

/**
 * Output of [PackageSessionUseCase] — describes the on-disk ZIP ready for upload.
 *
 * Design deviation E-1 (same rationale as [FormDataSnapshot]): this type mirrors the JSON
 * shape of [com.sfm.scanner.feature.upload.ZipArtifact] because architecture §12 forbids
 * cross-feature dependencies (`feature-scan → feature-upload` not allowed). The serialized
 * shape is identical so the same `zipArtifactJson` nav arg deserialises into either type.
 *
 * Consolidation into core-common deferred to M11 hardening.
 */
@Serializable
internal data class PackagedSession(
    val sessionUUID: String,
    val zipFilename: String,
    val absolutePath: String,
    val fileSizeBytes: Long,
)
