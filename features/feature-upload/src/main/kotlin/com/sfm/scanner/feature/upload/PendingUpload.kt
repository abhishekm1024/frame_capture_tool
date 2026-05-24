package com.sfm.scanner.feature.upload

data class PendingUpload(
    val id: String,
    val sessionUUID: String,
    val zipFilename: String,
    val absolutePath: String,
    val enqueuedAtMs: Long,
    val attemptCount: Int,
)
