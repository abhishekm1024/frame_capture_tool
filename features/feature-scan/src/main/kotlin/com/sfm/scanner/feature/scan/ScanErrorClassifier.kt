package com.sfm.scanner.feature.scan

import androidx.camera.core.CameraUnavailableException

/**
 * Maps a Throwable raised inside the camera/AR init path to one of the user-visible
 * error strings in [ScanStrings].
 *
 * Before this classifier existed, every exception out of `cameraRepository.startCapture(...)`
 * was unconditionally rendered as [ScanStrings.ERROR_CAMERA_UNAVAILABLE], which is the
 * spec-mandated *"Camera unavailable. Please check permissions."* string. That made a
 * generic init failure (e.g. CameraX bind error on a device where the OS-side camera
 * service is actually streaming, like the reported Samsung S23 case) look like a
 * permission problem to the user — they would open Settings only to find permission
 * already granted.
 *
 * The classifier preserves the three error states the screen already supports
 * (screen_specs §5.3) and adds [ScanStrings.ERROR_CAMERA_INIT] as the generic-init
 * bucket so we never claim a permission problem without permission evidence.
 *
 * Classification rules:
 *  1. [CameraUnavailableException] with reason [CameraUnavailableException.CAMERA_DISABLED]
 *     → [ScanStrings.ERROR_PERMISSION_CAMERA] (the OS *did* deny camera access — the
 *     Open Settings button is the right CTA).
 *  2. Any ARCore exception (package `com.google.ar.core.exceptions`) leaking through
 *     the camera path → [ScanStrings.ERROR_AR_UNSUPPORTED] (treated as an AR init failure).
 *  3. Everything else (bind errors, in-use, surface combo, generic init) →
 *     [ScanStrings.ERROR_CAMERA_INIT].
 *
 * Pure function; safe to unit-test on the JVM (no Android dependencies beyond the
 * CameraX exception type which is JVM-loadable).
 */
internal object ScanErrorClassifier {

    fun classifyCameraError(cause: Throwable): String {
        if (cause is CameraUnavailableException && cause.reason == CameraUnavailableException.CAMERA_DISABLED) {
            return ScanStrings.ERROR_PERMISSION_CAMERA
        }
        if (isArCoreException(cause)) {
            return ScanStrings.ERROR_AR_UNSUPPORTED
        }
        return ScanStrings.ERROR_CAMERA_INIT
    }

    private fun isArCoreException(cause: Throwable): Boolean {
        // Walk both the class and the cause chain. We avoid importing every concrete
        // ARCore exception type — checking the package name lets us pick up future
        // additions (UnavailableUserDeclinedInstallationException, etc.) without
        // touching this code.
        var t: Throwable? = cause
        while (t != null) {
            if (t.javaClass.name.startsWith("com.google.ar.core.exceptions.")) return true
            t = t.cause
        }
        return false
    }
}
