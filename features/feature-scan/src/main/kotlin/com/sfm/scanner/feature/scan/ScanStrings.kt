package com.sfm.scanner.feature.scan

/**
 * String literals for ScanScreen and PackagingScreen, from screen_specs.md §5.3 and §6.3.
 */
internal object ScanStrings {
    const val AR_STATE_INITIALIZING = "Initializing AR..."
    const val AR_STATE_TRACKING = "Tracking"
    const val AR_STATE_POINT_A = "Reference A captured"
    const val AR_STATE_COMPLETE = "Measurement complete"
    const val AR_STATE_LOST = "Tracking lost"
    const val AR_STATE_UNSUPPORTED = "AR unavailable"

    const val INSTRUCTION_READY = "Point camera at the center of the object, then tap Start Scan"
    const val INSTRUCTION_SCANNING = "Slowly move around the object"

    const val LABEL_FRAMES = "Frames: %d"

    const val BUTTON_START = "Start Scan"
    const val BUTTON_STOP = "Stop"
    const val BUTTON_OPEN_SETTINGS = "Open Settings"

    const val TORCH_OFF_DESCRIPTION = "Torch off"
    const val TORCH_ON_DESCRIPTION = "Torch on"

    // Spec-mandated strings from screen_specs §5.3. Do not edit verbatim copy.
    const val ERROR_CAMERA_UNAVAILABLE = "Camera unavailable. Please check permissions."
    const val ERROR_AR_UNSUPPORTED = "This device does not support AR measurement. Scanning is unavailable."
    const val ERROR_PERMISSION_CAMERA = "Camera permission is required to scan."

    // Non-permission camera/preview initialization failure. Used when CameraX raises
    // a bind-time exception unrelated to the runtime permission (e.g. the camera is
    // open by another use case, surface combination not supported, ARCore SharedCamera
    // race). The user reads this and does not mistakenly believe permission is missing.
    const val ERROR_CAMERA_INIT = "Camera could not be initialized. Please close other camera apps and try again."

    const val PACKAGING_LABEL = "Packaging scan data..."
    const val PACKAGING_CONTENT_DESCRIPTION = "Packaging scan data, please wait"

    const val CAMERA_VIEWFINDER_CONTENT_DESCRIPTION = "Camera viewfinder"
}
