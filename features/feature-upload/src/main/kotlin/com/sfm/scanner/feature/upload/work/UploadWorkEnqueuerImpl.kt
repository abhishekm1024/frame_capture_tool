package com.sfm.scanner.feature.upload.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sfm.scanner.feature.upload.UPLOAD_WORK_INPUT_PENDING_ID
import com.sfm.scanner.feature.upload.UPLOAD_WORK_NAME_PREFIX
import com.sfm.scanner.feature.upload.UploadWorkEnqueuer
import com.sfm.scanner.feature.upload.WORKMANAGER_INITIAL_BACKOFF_SECS
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class UploadWorkEnqueuerImpl @Inject constructor(
    private val workManager: WorkManager,
) : UploadWorkEnqueuer {

    override fun enqueueUploadWork(pendingUploadId: String) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UPLOAD_WORK_INPUT_PENDING_ID to pendingUploadId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WORKMANAGER_INITIAL_BACKOFF_SECS,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            "$UPLOAD_WORK_NAME_PREFIX$pendingUploadId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
