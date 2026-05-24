package com.sfm.scanner.navigation

object Routes {
    const val SPLASH = "splash"
    const val SELECTION = "selection"
    const val FORM = "form"

    // SCAN receives URL-encoded FormData JSON (see data_contracts.md §5.1)
    const val SCAN = "scan/{$ARG_FORM_DATA}"

    // PACKAGING receives no nav args; ScanSession is accessed via ViewModel scope
    const val PACKAGING = "packaging"

    // UPLOAD receives URL-encoded ZipArtifact JSON (see data_contracts.md §5.2)
    const val UPLOAD = "upload/{$ARG_ZIP_ARTIFACT}"

    const val ARG_FORM_DATA = "formDataJson"
    const val ARG_ZIP_ARTIFACT = "zipArtifactJson"
}
