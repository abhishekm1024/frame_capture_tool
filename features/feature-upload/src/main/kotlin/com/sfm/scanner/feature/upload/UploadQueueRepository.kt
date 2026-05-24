package com.sfm.scanner.feature.upload

import kotlinx.coroutines.flow.Flow

interface UploadQueueRepository {
    suspend fun enqueue(upload: PendingUpload)
    suspend fun markComplete(id: String)
    suspend fun incrementAttempt(id: String)
    fun getAll(): Flow<List<PendingUpload>>
    suspend fun getById(id: String): PendingUpload?
}
