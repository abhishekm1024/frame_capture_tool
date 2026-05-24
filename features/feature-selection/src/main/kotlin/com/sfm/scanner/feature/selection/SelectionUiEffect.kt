package com.sfm.scanner.feature.selection

sealed class SelectionUiEffect {
    data class NavigateToForm(val selectionId: String) : SelectionUiEffect()
}
