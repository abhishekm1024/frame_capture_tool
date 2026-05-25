package com.sfm.scanner.feature.scan.domain.usecase

import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Wraps [ArRepository.getMeasurementFlow] to centralise AR measurement consumption.
 *
 * Currently a thin pass-through. Future extensions may add filtering or rate-limiting
 * (e.g., deduplicating consecutive identical measurements).
 */
internal class MonitorArMeasurementUseCase @Inject constructor(
    private val arRepository: ArRepository,
) {
    fun execute(displayWidthPx: Int, displayHeightPx: Int): Flow<ARMeasurement> =
        arRepository.getMeasurementFlow(displayWidthPx, displayHeightPx)
}
