package com.sfm.scanner.data.ar

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow

/**
 * Contract for ARCore session management and measurement.
 *
 * Design deviations from data_contracts.md §6.2 (approved in technical design):
 *   D-1: Interface lives in data-ar (not feature-scan) to satisfy architecture §12.
 *   D-2: [startSession] accepts [LifecycleOwner] (required for DefaultLifecycleObserver).
 *   D-3: [getMeasurementFlow] accepts display dimensions (required for Frame.hitTest pixel coords).
 *   D-4: [ArSessionEvent.CameraShared] added so feature-scan can wire CameraX SharedCamera.
 */
interface ArRepository {
    fun startSession(lifecycleOwner: LifecycleOwner): Flow<ArSessionEvent>
    fun getMeasurementFlow(displayWidthPx: Int, displayHeightPx: Int): Flow<ARMeasurement>
    suspend fun pauseSession()
    suspend fun resumeSession()
    suspend fun destroySession()
}

sealed class ArSessionEvent {
    /** ARCore has opened the camera; [cameraId] is the Camera2 camera ID for SharedCamera wiring in M6. */
    data class CameraShared(val cameraId: String) : ArSessionEvent()
    /** First TRACKING state after session start; measurement can begin. */
    data object Ready : ArSessionEvent()
    data class TrackingChanged(val state: ArTrackingState) : ArSessionEvent()
    data class Error(val cause: Throwable) : ArSessionEvent()
    /** Device does not support ARCore or ARCore is permanently unavailable. */
    data object Unsupported : ArSessionEvent()
    /** ARCore is not installed / outdated; ScanScreen Activity must call ArCoreApk.requestInstall(). */
    data object InstallRequired : ArSessionEvent()
}
