package com.sfm.scanner.feature.scan.domain.model

import kotlinx.serialization.Serializable

/**
 * Snapshot of FormData passed via the `formDataJson` nav argument.
 *
 * Design deviation E-1: this type mirrors the JSON shape of [com.sfm.scanner.feature.form.FormData]
 * because [architecture.md §12] forbids cross-feature dependencies
 * (`feature-scan → feature-form` is not allowed). The serialized shape is identical so
 * the same `formDataJson` nav arg deserialises into either type.
 *
 * Consolidation into core-common deferred to M11 hardening (see implementation_status §6 N/O).
 */
@Serializable
internal data class FormDataSnapshot(
    val initialSelection: String,
    val dropdownSelection: String,
    val size: Int,
    val detail: String,
    val gt: List<Double>? = null,
)
