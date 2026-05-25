package com.sfm.scanner.data.ar

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.sfm.scanner.core.common.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ArRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : ArRepository {

    private val sessionManager = ArSessionManager(context)
    private val pipeline by lazy { ArMeasurementPipeline(context, sessionManager, dispatchers) }

    /**
     * Checks ARCore availability, creates the session (with SharedCamera), binds lifecycle,
     * and emits [ArSessionEvent]s. Completes after emitting the initial Ready or error event.
     *
     * The caller (ScanViewModel) is expected to continue collecting this flow through the
     * session lifetime to receive [ArSessionEvent.TrackingChanged] events.
     */
    override fun startSession(lifecycleOwner: LifecycleOwner): Flow<ArSessionEvent> =
        flow {
            // createSession() runs on the Default dispatcher (flowOn below).
            val result = sessionManager.createSession()

            when (result) {
                is ArSessionManager.CreateResult.Unsupported -> {
                    emit(ArSessionEvent.Unsupported)
                    return@flow
                }
                is ArSessionManager.CreateResult.InstallRequired -> {
                    emit(ArSessionEvent.InstallRequired)
                    return@flow
                }
                is ArSessionManager.CreateResult.Failure -> {
                    emit(ArSessionEvent.Error(result.cause))
                    return@flow
                }
                is ArSessionManager.CreateResult.Success -> {
                    emit(ArSessionEvent.CameraShared(result.cameraId))
                }
            }

            // bindLifecycle calls lifecycle.addObserver() which must run on Main.
            // DefaultLifecycleObserver.onResume() will call session.resume() once the
            // LifecycleOwner reaches RESUMED — at that point the AR session is live and
            // the screen can transition Initializing → Ready.
            withContext(dispatchers.main) {
                sessionManager.bindLifecycle(lifecycleOwner)
            }

            // Session is created and lifecycle-bound; surface Ready so the screen can
            // exit Initializing per screen_specs §6 (AR_STATE_TRACKING). The actual
            // ARCore tracking-quality signal flows through the measurement pipeline
            // (TrackingChanged is sourced from there).
            emit(ArSessionEvent.Ready)

            // Stay active until the collector cancels (e.g., ScanViewModel leaves composition).
            awaitCancellation()
        }.flowOn(dispatchers.default)

    /**
     * Begins continuous AR measurement. Must be called after [startSession] has emitted
     * [ArSessionEvent.Ready] (i.e., after the first TRACKING state is confirmed).
     *
     * Completes when [ARMeasurement.distanceMeters] is populated (MEASUREMENT_COMPLETE).
     * Caller cancels the coroutine on scan stop; [ARMeasurement] with null pointB is the
     * partial result passed to ScanSession.
     */
    override fun getMeasurementFlow(displayWidthPx: Int, displayHeightPx: Int): Flow<ARMeasurement> =
        pipeline.measurementFlow(displayWidthPx, displayHeightPx)

    override suspend fun pauseSession() {
        withContext(dispatchers.main) {
            sessionManager.session?.pause()
        }
    }

    override suspend fun resumeSession() {
        withContext(dispatchers.main) {
            try {
                sessionManager.session?.resume()
            } catch (e: Exception) {
                Log.e(AR_LOG_TAG, "resumeSession failed", e)
            }
        }
    }

    override suspend fun destroySession() {
        withContext(dispatchers.main) {
            sessionManager.closeSession()
        }
    }
}
