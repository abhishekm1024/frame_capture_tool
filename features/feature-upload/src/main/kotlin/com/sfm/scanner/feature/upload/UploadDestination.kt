package com.sfm.scanner.feature.upload

object UploadDestination {
    const val ARG_ZIP_ARTIFACT = "zipArtifactJson"
    const val route = "upload/{$ARG_ZIP_ARTIFACT}"

    fun createRoute(zipArtifactJson: String) = "upload/$zipArtifactJson"
}
