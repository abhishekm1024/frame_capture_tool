package com.sfm.scanner.feature.form

data class FormUiState(
    val dropdownSelection: String? = null,
    val sizeInput: String = "",
    val sizeError: String? = null,
    val detailInput: String = "",
    val detailError: String? = null,
    val gtInput: String = "",
    val gtError: String? = null,
    val proceedEnabled: Boolean = false,
)
