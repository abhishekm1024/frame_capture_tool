package com.sfm.scanner.feature.scan

object ScanDestination {
    const val ARG_FORM_DATA = "formDataJson"
    const val route = "scan/{$ARG_FORM_DATA}"

    fun createRoute(formDataJson: String): String = "scan/$formDataJson"
}
