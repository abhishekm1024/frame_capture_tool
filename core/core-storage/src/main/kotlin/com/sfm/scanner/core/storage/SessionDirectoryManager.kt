package com.sfm.scanner.core.storage

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDirectoryManager @Inject constructor(
    private val fileProvider: AppFileProvider,
) {
    /**
     * Creates and returns the frames directory for a session: cacheDir/sessions/{uuid}/frames/
     */
    fun createSessionDir(uuid: String): File {
        val framesDir = fileProvider.framesDir(uuid)
        framesDir.mkdirs()
        return framesDir
    }

    /**
     * Deletes the entire session directory tree: cacheDir/sessions/{uuid}/
     */
    fun deleteSessionDir(uuid: String) {
        fileProvider.sessionDir(uuid).deleteRecursively()
    }
}
