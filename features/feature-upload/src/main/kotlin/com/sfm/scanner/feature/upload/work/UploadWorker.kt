package com.sfm.scanner.feature.upload.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.sfm.scanner.core.common.Result as DomainResult
import com.sfm.scanner.data.firebase.FirebaseRepository
import com.sfm.scanner.data.firebase.UploadProgress
import com.sfm.scanner.feature.upload.UPLOAD_WORK_INPUT_PENDING_ID
import com.sfm.scanner.feature.upload.UploadQueueRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.last
import java.io.File

@HiltWorker
internal class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firebaseRepository: FirebaseRepository,
    private val uploadQueueRepository: UploadQueueRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        val pendingId = inputData.getString(UPLOAD_WORK_INPUT_PENDING_ID)
            ?: return ListenableWorker.Result.failure()

        val pending = uploadQueueRepository.getById(pendingId)
            ?: return ListenableWorker.Result.failure()

        val zipFile = File(pending.absolutePath)
        if (!zipFile.exists()) {
            // ZIP is gone — nothing to retry; remove the queue entry to prevent indefinite retries.
            uploadQueueRepository.markComplete(pendingId)
            return ListenableWorker.Result.failure()
        }

        val uid = when (val authResult = firebaseRepository.signInAnonymously()) {
            is DomainResult.Success -> authResult.data
            is DomainResult.Failure -> {
                uploadQueueRepository.incrementAttempt(pendingId)
                return ListenableWorker.Result.retry()
            }
            DomainResult.Loading -> {
                uploadQueueRepository.incrementAttempt(pendingId)
                return ListenableWorker.Result.retry()
            }
        }

        val terminal = firebaseRepository.uploadZip(uid, zipFile).last()
        return when (terminal) {
            UploadProgress.Success -> {
                zipFile.delete()
                uploadQueueRepository.markComplete(pendingId)
                ListenableWorker.Result.success()
            }
            is UploadProgress.Failure -> {
                uploadQueueRepository.incrementAttempt(pendingId)
                ListenableWorker.Result.retry()
            }
            is UploadProgress.Uploading -> {
                // Flow ended without terminal event — treat as failure.
                uploadQueueRepository.incrementAttempt(pendingId)
                ListenableWorker.Result.retry()
            }
        }
    }
}
