package com.sfm.scanner.data.firebase

import com.sfm.scanner.core.common.Result
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepositoryImpl @Inject constructor(
    private val authSource: AnonymousAuthSource,
    private val uploader: FirebaseStorageUploader,
) : FirebaseRepository {

    override suspend fun signInAnonymously(): Result<String> = authSource.signIn()

    override fun uploadZip(uid: String, zipFile: File): Flow<UploadProgress> =
        uploader.upload(uid, zipFile)
}
