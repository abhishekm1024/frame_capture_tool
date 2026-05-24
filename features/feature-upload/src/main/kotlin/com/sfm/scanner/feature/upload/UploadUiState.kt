package com.sfm.scanner.feature.upload

sealed class UploadUiState {
    data object Authenticating : UploadUiState()
    data class Uploading(val percent: Int, val filename: String) : UploadUiState()
    data object Success : UploadUiState()
    data class Failed(val filename: String) : UploadUiState()
    data object RetryQueued : UploadUiState()
}
