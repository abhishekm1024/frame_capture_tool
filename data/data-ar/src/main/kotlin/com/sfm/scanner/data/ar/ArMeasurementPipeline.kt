package com.sfm.scanner.data.ar

import android.content.Context
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.sfm.scanner.core.common.AppDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Runs the ARCore update loop and drives the [MeasurementStateReducer] to produce
 * [ARMeasurement] emissions.
 *
 * Threading:
 *   - [session.update()] is blocking; runs inside [withContext(dispatchers.default)].
 *   - The entire flow is [flowOn(dispatchers.default)]; collectors observe on their own dispatcher.
 *   - [ArSessionManager.isRunning] is checked before each update to avoid calling
 *     [session.update()] while the session is paused (which is illegal per ARCore).
 */
internal class ArMeasurementPipeline(
    private val context: Context,
    private val sessionManager: ArSessionManager,
    private val dispatchers: AppDispatchers,
) {

    fun measurementFlow(displayWidthPx: Int, displayHeightPx: Int): Flow<ARMeasurement> =
        flow {
            val session = requireNotNull(sessionManager.session) {
                "ArMeasurementPipeline: session is null — call ArSessionManager.createSession() first"
            }

            val centerX = displayWidthPx / 2f
            val centerY = displayHeightPx / 2f

            // Tell ARCore the display geometry so Frame.hitTest() uses correct coordinates.
            // setDisplayGeometry may be called from any thread before the next update().
            session.setDisplayGeometry(displayRotation(), displayWidthPx, displayHeightPx)

            var measurementState: MeasurementState = MeasurementState.AwaitingPointA
            var consecutiveNoHitCount = 0

            // Cancellation propagates through delay() and withContext() suspension points.
            while (true) {
                if (!sessionManager.isRunning) {
                    delay(50L)
                    continue
                }

                val frame = try {
                    withContext(dispatchers.default) { session.update() }
                } catch (e: CameraNotAvailableException) {
                    Log.e(AR_LOG_TAG, "Camera not available during update", e)
                    emit(ARMeasurement(trackingState = ArTrackingState.STOPPED, hitType = HitType.NONE))
                    break
                } catch (e: Exception) {
                    Log.e(AR_LOG_TAG, "session.update() threw unexpectedly: ${e.message}", e)
                    break
                }

                val trackingState = frame.camera.trackingState.toDomain()
                val cameraPose = frame.camera.pose.toDomain()

                val hit = if (trackingState == ArTrackingState.TRACKING) {
                    extractCenterHit(frame, cameraPose, centerX, centerY)
                } else {
                    null
                }

                val output = MeasurementStateReducer.reduce(
                    current = measurementState,
                    trackingState = trackingState,
                    hit = hit,
                    currentCameraPose = cameraPose,
                    consecutiveNoHitCount = consecutiveNoHitCount,
                    nowMs = System.currentTimeMillis(),
                )

                measurementState = output.nextState
                val prevCount = consecutiveNoHitCount
                consecutiveNoHitCount = output.newConsecutiveNoHitCount
                // Log budget exhaustion: counter reset while tracking (not due to tracking loss)
                if (prevCount > 0 && consecutiveNoHitCount == 0
                    && output.emission?.hitType == HitType.NONE
                    && output.emission?.trackingState == ArTrackingState.TRACKING) {
                    Log.w(AR_LOG_TAG, "Retry budget exhausted: no center-pixel hit after $AR_RAYCAST_RETRY_BUDGET consecutive frames")
                }
                output.emission?.let { emit(it) }

                if (measurementState is MeasurementState.Complete) break
            }
        }.flowOn(dispatchers.default)

    private fun extractCenterHit(
        frame: Frame,
        cameraPose: CameraPose,
        centerX: Float,
        centerY: Float,
    ): HitTypeAndPosition? {
        val hits = frame.hitTest(centerX, centerY)

        val selected = hits.firstOrNull { it.trackable is DepthPoint }
            ?: hits.firstOrNull { it.trackable is Plane }
            ?: hits.firstOrNull { it.trackable is InstantPlacementPoint }
            ?: hits.firstOrNull()

        val trackable = selected?.trackable ?: return null

        val hitType = when (trackable) {
            is DepthPoint -> HitType.DEPTH
            is Plane -> HitType.PLANE
            is InstantPlacementPoint -> HitType.INSTANT_PLACEMENT
            else -> HitType.FEATURE_POINT
        }

        val hitPose = selected.hitPose
        return HitTypeAndPosition(
            hitType = hitType,
            position = ArPoint(
                x = hitPose.tx(),
                y = hitPose.ty(),
                z = hitPose.tz(),
                timestampMs = System.currentTimeMillis(),
            ),
            cameraPose = cameraPose,
        )
    }

    @Suppress("DEPRECATION")
    private fun displayRotation(): Int =
        context.getSystemService(WindowManager::class.java)
            ?.defaultDisplay
            ?.rotation
            ?: Surface.ROTATION_0
}

// ── ARCore → domain type adapters ────────────────────────────────────────────

private fun TrackingState.toDomain(): ArTrackingState = when (this) {
    TrackingState.TRACKING -> ArTrackingState.TRACKING
    TrackingState.PAUSED -> ArTrackingState.PAUSED
    TrackingState.STOPPED -> ArTrackingState.STOPPED
}

private fun Pose.toDomain(): CameraPose = CameraPose(
    tx = tx(), ty = ty(), tz = tz(),
    qx = qx(), qy = qy(), qz = qz(), qw = qw(),
)
