package com.sfm.scanner.feature.selection

// [TBD-A1] Replace screenTitle and all entries with the resolved values before release.
internal object SelectionOptions {

    const val screenTitle = "Select Object Type"

    val all: List<SelectionOption> = listOf(
        SelectionOption(id = "option_a", displayLabel = "Option A"),
        SelectionOption(id = "option_b", displayLabel = "Option B"),
        SelectionOption(id = "option_c", displayLabel = "Option C"),
    )
}
