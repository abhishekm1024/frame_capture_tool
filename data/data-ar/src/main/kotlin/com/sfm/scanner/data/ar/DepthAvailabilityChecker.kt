package com.sfm.scanner.data.ar

import com.google.ar.core.Config
import com.google.ar.core.Session

/**
 * Queries ARCore depth support and builds the best available session config.
 *
 * Always enables InstantPlacement for broadest hit-test coverage regardless of depth support.
 * R-01 mitigation: the hitType field on every ARMeasurement lets downstream tools weight
 * depth vs. plane vs. feature-point measurements appropriately.
 */
internal class DepthAvailabilityChecker {

    fun isDepthSupported(session: Session): Boolean =
        session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)

    fun buildConfig(session: Session): Config {
        val config = Config(session)
        config.depthMode = if (isDepthSupported(session)) {
            Config.DepthMode.AUTOMATIC
        } else {
            Config.DepthMode.DISABLED
        }
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        return config
    }
}
