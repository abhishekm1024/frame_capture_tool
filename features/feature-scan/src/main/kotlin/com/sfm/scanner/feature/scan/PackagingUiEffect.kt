package com.sfm.scanner.feature.scan

sealed class PackagingUiEffect {
    /**
     * Navigate to UploadScreen with packaged ZIP data.
     * [zipArtifactJson] is URL-encoded JSON of `feature-upload.ZipArtifact` (same shape as
     * the local `PackagedSession`).
     */
    data class NavigateToUpload(val zipArtifactJson: String) : PackagingUiEffect()

    /**
     * Packaging failed; UploadScreen will show the error per screen_specs §6.5.
     * The error message is included; UploadScreen routes to its Failed UI variant.
     */
    data class NavigateToUploadWithError(val message: String) : PackagingUiEffect()
}
