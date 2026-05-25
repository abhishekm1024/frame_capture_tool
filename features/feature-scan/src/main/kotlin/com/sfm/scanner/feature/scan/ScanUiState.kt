package com.sfm.scanner.feature.scan

/**
 * Complete observable UI state for [ScanScreen]. Exact shape from data_contracts.md §7.1.
 */
sealed class ScanUiState {
    data object Initializing : ScanUiState()
    data class Ready(val torchOn: Boolean, val arDisplayState: ArDisplayState) : ScanUiState()
    data class Scanning(
        val frameCount: Int,
        val torchOn: Boolean,
        val arDisplayState: ArDisplayState,
    ) : ScanUiState()
    data object Stopping : ScanUiState()
    data object Complete : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

/**
 * AR status chip display state. data_contracts.md §7.1.
 */
enum class ArDisplayState {
    INITIALIZING,
    TRACKING,
    POINT_A_CAPTURED,
    MEASUREMENT_COMPLETE,
    TRACKING_LOST,
    UNSUPPORTED,
}
