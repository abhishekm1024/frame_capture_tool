package com.sfm.scanner.feature.upload

internal sealed class UploadStage {
    data object Authenticating : UploadStage()
    data class Uploading(val percent: Int) : UploadStage()
    data object Success : UploadStage()
    data class Failed(val cause: Throwable) : UploadStage()
    data object RetryQueued : UploadStage()
}
