package com.sfm.scanner.feature.form

object FormDestination {
    const val ARG_SELECTION_ID = "selectionId"
    const val route = "form/{$ARG_SELECTION_ID}"

    fun createRoute(selectionId: String) = "form/$selectionId"
}
