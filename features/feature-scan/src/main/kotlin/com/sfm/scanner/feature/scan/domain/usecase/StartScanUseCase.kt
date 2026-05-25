package com.sfm.scanner.feature.scan.domain.usecase

import com.sfm.scanner.core.storage.SessionDirectoryManager
import java.io.File
import javax.inject.Inject

/**
 * Prepares the session-scoped temporary directory for frame files.
 *
 * Returns the `cacheDir/sessions/{uuid}/frames/` directory ready for writing.
 */
internal class StartScanUseCase @Inject constructor(
    private val sessionDirectoryManager: SessionDirectoryManager,
) {
    fun createSessionDirectory(sessionUuid: String): File =
        sessionDirectoryManager.createSessionDir(sessionUuid)
}
