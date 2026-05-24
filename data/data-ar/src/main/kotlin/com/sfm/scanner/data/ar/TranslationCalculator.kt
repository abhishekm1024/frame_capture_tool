package com.sfm.scanner.data.ar

import kotlin.math.sqrt

/**
 * Computes Euclidean distance between two camera origins.
 *
 * Pure function with no ARCore SDK dependency — fully testable on JVM.
 * Formula per functional_spec.md §5.5.3.
 */
internal object TranslationCalculator {

    fun compute(poseA: CameraPose, poseB: CameraPose): Float {
        val dx = poseB.tx - poseA.tx
        val dy = poseB.ty - poseA.ty
        val dz = poseB.tz - poseA.tz
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun exceedsThreshold(poseA: CameraPose, current: CameraPose): Boolean =
        compute(poseA, current) >= AR_TRANSLATION_THRESHOLD_METERS
}
