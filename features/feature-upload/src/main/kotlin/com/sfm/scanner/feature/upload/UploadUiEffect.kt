package com.sfm.scanner.feature.upload

sealed class UploadUiEffect {
    data object NavigateToSelection : UploadUiEffect()
}
