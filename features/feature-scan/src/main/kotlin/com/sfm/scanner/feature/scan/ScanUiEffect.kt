package com.sfm.scanner.feature.scan

sealed class ScanUiEffect {
    /** Navigate to PackagingScreen; ScanSession already deposited in ScanSessionHolder. */
    data object NavigateToPackaging : ScanUiEffect()
    /** ARCore needs install/update; ScanScreen Activity must call ArCoreApk.requestInstall(). */
    data object RequestArInstall : ScanUiEffect()
}
