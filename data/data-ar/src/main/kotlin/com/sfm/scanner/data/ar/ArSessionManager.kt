package com.sfm.scanner.data.ar

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import java.util.EnumSet

/**
 * Manages the ARCore [Session] lifecycle, bound to the provided [LifecycleOwner].
 *
 * Threading:
 *   - [DefaultLifecycleObserver] callbacks run on Main (resume/pause/close).
 *   - [session] is @Volatile for cross-thread visibility to the measurement loop.
 *
 * SharedCamera (design deviation D-4):
 *   Session is created with [Session.Feature.SHARED_CAMERA]. This means ARCore owns
 *   the camera hardware. [getSharedCameraId] exposes ARCore's chosen camera ID so
 *   feature-scan (M6) can configure CameraX to share the same camera session.
 */
internal class ArSessionManager(
    private val context: Context,
    private val depthChecker: DepthAvailabilityChecker = DepthAvailabilityChecker(),
) : DefaultLifecycleObserver {

    @Volatile var session: Session? = null
        private set

    @Volatile var isRunning: Boolean = false
        private set

    sealed class CreateResult {
        data class Success(val session: Session, val cameraId: String) : CreateResult()
        data object Unsupported : CreateResult()
        data object InstallRequired : CreateResult()
        data class Failure(val cause: Throwable) : CreateResult()
    }

    /**
     * Checks availability and creates a SharedCamera ARCore session.
     * Must be called from a coroutine (not Main) — availability check may block briefly.
     */
    fun createSession(): CreateResult {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        if (!availability.isSupported) {
            Log.w(AR_LOG_TAG, "ARCore not supported: $availability")
            return CreateResult.Unsupported
        }
        when (availability) {
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
            -> {
                Log.w(AR_LOG_TAG, "ARCore install required: $availability")
                return CreateResult.InstallRequired
            }
            else -> { /* SUPPORTED_INSTALLED — proceed */ }
        }

        return try {
            val session = Session(context, EnumSet.of(Session.Feature.SHARED_CAMERA))
            val config = depthChecker.buildConfig(session)
            session.configure(config)
            // ARCore 1.46 exposes the active camera ID via Session.getCameraConfig().getCameraId();
            // SharedCamera itself has no cameraId accessor. The chosen camera ID is what M6/M9
            // hands to CameraX so its CameraSelector binds to the same Camera2 device.
            val cameraId = session.cameraConfig.cameraId
            this.session = session
            CreateResult.Success(session, cameraId)
        } catch (e: UnavailableArcoreNotInstalledException) {
            CreateResult.InstallRequired
        } catch (e: UnavailableApkTooOldException) {
            CreateResult.InstallRequired
        } catch (e: UnavailableSdkTooOldException) {
            CreateResult.Failure(e)
        } catch (e: UnavailableDeviceNotCompatibleException) {
            CreateResult.Unsupported
        } catch (e: Exception) {
            Log.e(AR_LOG_TAG, "Session creation failed", e)
            CreateResult.Failure(e)
        }
    }

    fun bindLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        try {
            session?.resume()
            isRunning = true
        } catch (e: Exception) {
            Log.e(AR_LOG_TAG, "Session resume failed", e)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        isRunning = false
        try {
            session?.pause()
        } catch (e: Exception) {
            Log.e(AR_LOG_TAG, "Session pause failed", e)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        isRunning = false
        closeSession()
        owner.lifecycle.removeObserver(this)
    }

    fun closeSession() {
        try {
            session?.close()
        } catch (e: Exception) {
            Log.e(AR_LOG_TAG, "Session close failed", e)
        } finally {
            session = null
        }
    }
}

private val ArCoreApk.Availability.isSupported: Boolean
    get() = this != ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        && this != ArCoreApk.Availability.UNKNOWN_ERROR
        && this != ArCoreApk.Availability.UNKNOWN_CHECKING
        && this != ArCoreApk.Availability.UNKNOWN_TIMED_OUT
