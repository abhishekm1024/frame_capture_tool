package com.sfm.scanner.feature.selection

data class SelectionUiState(
    val options: List<SelectionOption> = SelectionOptions.all,
    val selectedOption: SelectionOption? = null,
)
