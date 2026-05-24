package com.sfm.scanner.data.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageUploader @Inject constructor(
    private val storage: FirebaseStorage,
) {
    fun upload(uid: String, zipFile: File): Flow<UploadProgress> = callbackFlow {
        val fileUri = Uri.fromFile(zipFile)
        val ref = storage.reference.child("$FIREBASE_STORAGE_BASE_PATH/$uid/${zipFile.name}")
        val uploadTask = ref.putFile(fileUri)

        uploadTask.addOnProgressListener { snapshot ->
            val percent = if (snapshot.totalByteCount > 0L) {
                ((snapshot.bytesTransferred.toDouble() / snapshot.totalByteCount.toDouble()) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)
            } else {
                0
            }
            trySend(UploadProgress.Uploading(percent))
        }

        uploadTask.addOnSuccessListener {
            trySend(UploadProgress.Success)
            close()
        }

        uploadTask.addOnFailureListener { exception ->
            trySend(UploadProgress.Failure(exception))
            close()
        }

        awaitClose { uploadTask.cancel() }
    }
}
