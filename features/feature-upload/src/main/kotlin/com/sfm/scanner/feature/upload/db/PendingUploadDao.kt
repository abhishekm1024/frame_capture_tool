package com.sfm.scanner.feature.upload.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PendingUploadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingUploadEntity)

    @Query("DELETE FROM pending_uploads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM pending_uploads ORDER BY enqueued_at_ms ASC")
    fun getAll(): Flow<List<PendingUploadEntity>>

    @Query("SELECT * FROM pending_uploads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingUploadEntity?

    @Query("UPDATE pending_uploads SET attempt_count = attempt_count + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: String)
}
