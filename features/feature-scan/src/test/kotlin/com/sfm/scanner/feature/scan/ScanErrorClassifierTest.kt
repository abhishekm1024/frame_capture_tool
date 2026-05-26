package com.sfm.scanner.feature.scan

import androidx.camera.core.CameraUnavailableException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Pins the runtime bug fix: scan-path exceptions are no longer all collapsed to the
 * spec-mandated "Camera unavailable. Please check permissions." string.
 *
 * The reported Samsung S23 case (CAMERA permission granted, CameraService streaming,
 * but CameraX bind raises an exception) is covered by [bind_failure_with_permission_granted_does_not_blame_permission].
 */
class ScanErrorClassifierTest {

    @Test
    fun camera_disabled_classifies_as_permission_error() {
        val cause = CameraUnavailableException(CameraUnavailableException.CAMERA_DISABLED)
        assertEquals(
            ScanStrings.ERROR_PERMISSION_CAMERA,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun camera_in_use_classifies_as_generic_init_not_permission() {
        // Another app holds the camera. This is NOT a permission problem; the user
        // would learn nothing useful from "check permissions."
        val cause = CameraUnavailableException(CameraUnavailableException.CAMERA_IN_USE)
        assertEquals(
            ScanStrings.ERROR_CAMERA_INIT,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun camera_disconnected_classifies_as_generic_init_not_permission() {
        val cause = CameraUnavailableException(CameraUnavailableException.CAMERA_DISCONNECTED)
        assertEquals(
            ScanStrings.ERROR_CAMERA_INIT,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun arcore_not_installed_exception_classifies_as_ar_unsupported() {
        val cause = UnavailableArcoreNotInstalledException()
        assertEquals(
            ScanStrings.ERROR_AR_UNSUPPORTED,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun arcore_device_not_compatible_exception_classifies_as_ar_unsupported() {
        val cause = UnavailableDeviceNotCompatibleException()
        assertEquals(
            ScanStrings.ERROR_AR_UNSUPPORTED,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun arcore_exception_nested_inside_runtime_exception_still_classifies_as_ar_unsupported() {
        // CameraX wraps internal failures in RuntimeException; if an AR exception is
        // the root cause it must still be recognised.
        val arCause = UnavailableArcoreNotInstalledException()
        val wrapper = RuntimeException("wrapping", arCause)
        assertEquals(
            ScanStrings.ERROR_AR_UNSUPPORTED,
            ScanErrorClassifier.classifyCameraError(wrapper),
        )
    }

    @Test
    fun illegal_argument_from_bindToLifecycle_classifies_as_generic_init() {
        // bindToLifecycle raises IAE when no supported surface combination is found.
        val cause = IllegalArgumentException("No supported surface combination is found")
        assertEquals(
            ScanStrings.ERROR_CAMERA_INIT,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun illegal_state_from_bindToLifecycle_classifies_as_generic_init() {
        // bindToLifecycle raises ISE when use cases conflict or the surface is in use.
        val cause = IllegalStateException("Surface already in use")
        assertEquals(
            ScanStrings.ERROR_CAMERA_INIT,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun generic_runtime_exception_classifies_as_generic_init() {
        val cause = RuntimeException("unexpected")
        assertEquals(
            ScanStrings.ERROR_CAMERA_INIT,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun io_exception_classifies_as_generic_init() {
        val cause = IOException("disk write failed")
        assertEquals(
            ScanStrings.ERROR_CAMERA_INIT,
            ScanErrorClassifier.classifyCameraError(cause),
        )
    }

    @Test
    fun bind_failure_with_permission_granted_does_not_blame_permission() {
        // The reported Samsung S23 scenario: OS-side camera works fine (permission
        // granted, CameraService streaming), but CameraX bind fails for an internal
        // reason. The classifier must NOT route this to ERROR_PERMISSION_CAMERA;
        // routing there would surface "Camera permission is required" to a user
        // who has already granted it.
        val s23BindFailure = IllegalStateException(
            "CameraX bindToLifecycle failed while camera service is ACTIVE",
        )
        val classified = ScanErrorClassifier.classifyCameraError(s23BindFailure)
        assertEquals(ScanStrings.ERROR_CAMERA_INIT, classified)
        // And explicitly: never the permission string here.
        assert(classified != ScanStrings.ERROR_PERMISSION_CAMERA) {
            "S23 bind failure must not be misclassified as a permission error"
        }
    }
}
