package com.sfm.scanner.feature.upload.db

import com.sfm.scanner.feature.upload.PendingUpload
import com.sfm.scanner.feature.upload.UploadQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class UploadQueueRepositoryImpl @Inject constructor(
    private val dao: PendingUploadDao,
) : UploadQueueRepository {

    override suspend fun enqueue(upload: PendingUpload) {
        dao.insert(upload.toEntity())
    }

    override suspend fun markComplete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun incrementAttempt(id: String) {
        dao.incrementAttempt(id)
    }

    override fun getAll(): Flow<List<PendingUpload>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): PendingUpload? =
        dao.getById(id)?.toDomain()
}
