package com.sfm.scanner.feature.scan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Exact `details.json` schema per data_contracts.md §2.1.
 *
 * Field order matches the spec; `gt` is nullable JSON array (never empty array).
 */
@Serializable
internal data class DetailsJson(
    val initialSelection: String,
    val dropdownSelection: String,
    val size: Int,
    val detail: String,
    val gt: List<Double>?,
    val scanTimestamp: String,
    val deviceInfo: String,
    val appVersion: String,
)
