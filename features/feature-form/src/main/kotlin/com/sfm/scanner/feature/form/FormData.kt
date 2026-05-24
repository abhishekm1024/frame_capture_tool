package com.sfm.scanner.feature.form

import kotlinx.serialization.Serializable

@Serializable
data class FormData(
    val initialSelection: String,
    val dropdownSelection: String,
    val size: Int,
    val detail: String,
    val gt: List<Double>?,
)
