package com.sfm.scanner.data.camera

import android.util.Size
import androidx.camera.core.ResolutionFilter
import androidx.camera.core.ResolutionSelector

/**
 * Encapsulates the preferred → fallback resolution selection logic.
 *
 * Priority order: 2560×1440 → 1920×1080 → 1280×720 → largest supported size.
 *
 * [buildCameraXSelector] returns a [ResolutionSelector] for use with
 * [androidx.camera.core.ImageAnalysis.Builder].
 *
 * [selectFrom] is exposed separately for unit testing without needing a
 * live camera session.
 */
internal class ResolutionPicker {

    private val preferredSizes = listOf(
        Size(PREFERRED_WIDTH, PREFERRED_HEIGHT),
        Size(FALLBACK_WIDTH, FALLBACK_HEIGHT),
        Size(1280, 720),
    )

    /**
     * Returns the best matching [Size] from [supportedSizes], or null if empty.
     *
     * Walks [preferredSizes] in order; if none match, returns the largest
     * supported size by pixel count as a safe fallback.
     */
    fun selectFrom(supportedSizes: List<Size>): Size? {
        if (supportedSizes.isEmpty()) return null
        val supported = supportedSizes.toHashSet()
        return preferredSizes.firstOrNull { it in supported }
            ?: supportedSizes.maxByOrNull { it.width.toLong() * it.height }
    }

    /**
     * Builds a [ResolutionSelector] that delivers preferred sizes first to CameraX.
     * CameraX picks the first size from the filter's returned list that the hardware supports.
     */
    fun buildCameraXSelector(): ResolutionSelector {
        val filter = ResolutionFilter { supportedSizes, _ ->
            val preferred = preferredSizes.filter { it in supportedSizes }
            val others = supportedSizes.filterNot { it in preferred }
            preferred + others
        }
        return ResolutionSelector.Builder()
            .setResolutionFilter(filter)
            .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
            .build()
    }
}
