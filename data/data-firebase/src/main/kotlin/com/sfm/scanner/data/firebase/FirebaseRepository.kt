package com.sfm.scanner.data.firebase

import com.sfm.scanner.core.common.Result
import kotlinx.coroutines.flow.Flow
import java.io.File

interface FirebaseRepository {
    suspend fun signInAnonymously(): Result<String>
    fun uploadZip(uid: String, zipFile: File): Flow<UploadProgress>
}
