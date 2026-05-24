package com.sfm.scanner.feature.form

sealed class FormUiEffect {
    data class NavigateToScan(val formData: FormData) : FormUiEffect()
}
