package com.sfm.scanner.feature.upload

import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.core.common.Result
import com.sfm.scanner.data.firebase.FirebaseRepository
import com.sfm.scanner.data.firebase.UploadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID
import javax.inject.Inject

internal class UploadZipUseCase @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val uploadQueueRepository: UploadQueueRepository,
    private val uploadWorkEnqueuer: UploadWorkEnqueuer,
    private val appDispatchers: AppDispatchers,
) {

    operator fun invoke(zipArtifact: ZipArtifact): Flow<UploadStage> = flow {
        emit(UploadStage.Authenticating)

        val zipFile = File(zipArtifact.absolutePath)
        if (!zipFile.exists()) {
            emit(UploadStage.Failed(IllegalStateException("ZIP file missing: ${zipArtifact.absolutePath}")))
            return@flow
        }

        val authResult = firebaseRepository.signInAnonymously()
        val uid = when (authResult) {
            is Result.Success -> authResult.data
            is Result.Failure -> {
                enqueueRetry(zipArtifact)
                emit(UploadStage.RetryQueued)
                return@flow
            }
            Result.Loading -> {
                enqueueRetry(zipArtifact)
                emit(UploadStage.RetryQueued)
                return@flow
            }
        }

        var terminalEmitted = false
        firebaseRepository.uploadZip(uid, zipFile).collect { progress ->
            when (progress) {
                is UploadProgress.Uploading -> emit(UploadStage.Uploading(progress.percent))
                UploadProgress.Success -> {
                    zipFile.delete()
                    emit(UploadStage.Success)
                    terminalEmitted = true
                }
                is UploadProgress.Failure -> {
                    enqueueRetry(zipArtifact)
                    emit(UploadStage.RetryQueued)
                    terminalEmitted = true
                }
            }
        }

        if (!terminalEmitted) {
            // Upload flow completed without a terminal event — treat as failure and queue retry.
            enqueueRetry(zipArtifact)
            emit(UploadStage.RetryQueued)
        }
    }.flowOn(appDispatchers.io)

    private suspend fun enqueueRetry(zipArtifact: ZipArtifact) {
        val pending = PendingUpload(
            id = UUID.randomUUID().toString(),
            sessionUUID = zipArtifact.sessionUUID,
            zipFilename = zipArtifact.zipFilename,
            absolutePath = zipArtifact.absolutePath,
            enqueuedAtMs = System.currentTimeMillis(),
            attemptCount = 0,
        )
        uploadQueueRepository.enqueue(pending)
        uploadWorkEnqueuer.enqueueUploadWork(pending.id)
    }
}
