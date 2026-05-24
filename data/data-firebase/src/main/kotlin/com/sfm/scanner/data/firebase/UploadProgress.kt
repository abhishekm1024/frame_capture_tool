package com.sfm.scanner.data.firebase

sealed class UploadProgress {
    data class Uploading(val percent: Int) : UploadProgress()
    data object Success : UploadProgress()
    data class Failure(val cause: Throwable) : UploadProgress()
}
