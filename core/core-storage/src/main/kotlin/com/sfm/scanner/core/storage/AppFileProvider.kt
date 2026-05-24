package com.sfm.scanner.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFileProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val sessionsRoot: File
        get() = File(context.cacheDir, StorageConstants.SESSION_DIR_ROOT)

    val uploadsDir: File
        get() = File(context.filesDir, StorageConstants.UPLOADS_DIR_ROOT).also { it.mkdirs() }

    fun sessionDir(uuid: String): File =
        File(sessionsRoot, uuid)

    fun framesDir(uuid: String): File =
        File(sessionDir(uuid), StorageConstants.FRAMES_SUBDIR)
}
