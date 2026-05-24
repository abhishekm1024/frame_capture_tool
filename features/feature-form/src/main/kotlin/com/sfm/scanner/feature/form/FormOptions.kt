package com.sfm.scanner.feature.form

// [TBD-A2] Replace all entries with the resolved keys and labels before release.
internal data class FormOption(val key: String, val label: String)

internal object FormOptions {
    val all: List<FormOption> = listOf(
        FormOption(key = "type_a", label = "Type A"),
        FormOption(key = "type_b", label = "Type B"),
    )
}
