package com.sfm.scanner.feature.upload

interface UploadWorkEnqueuer {
    fun enqueueUploadWork(pendingUploadId: String)
}
