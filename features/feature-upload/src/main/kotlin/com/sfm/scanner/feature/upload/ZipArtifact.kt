package com.sfm.scanner.feature.upload

import kotlinx.serialization.Serializable

@Serializable
data class ZipArtifact(
    val sessionUUID: String,
    val zipFilename: String,
    val absolutePath: String,
    val fileSizeBytes: Long,
)
