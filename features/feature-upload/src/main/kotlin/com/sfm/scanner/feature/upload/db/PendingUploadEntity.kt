package com.sfm.scanner.feature.upload.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sfm.scanner.feature.upload.PendingUpload

@Entity(tableName = "pending_uploads")
internal data class PendingUploadEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "session_uuid")
    val sessionUuid: String,
    @ColumnInfo(name = "zip_filename")
    val zipFilename: String,
    @ColumnInfo(name = "absolute_path")
    val absolutePath: String,
    @ColumnInfo(name = "enqueued_at_ms")
    val enqueuedAtMs: Long,
    @ColumnInfo(name = "attempt_count", defaultValue = "0")
    val attemptCount: Int,
)

internal fun PendingUploadEntity.toDomain(): PendingUpload = PendingUpload(
    id = id,
    sessionUUID = sessionUuid,
    zipFilename = zipFilename,
    absolutePath = absolutePath,
    enqueuedAtMs = enqueuedAtMs,
    attemptCount = attemptCount,
)

internal fun PendingUpload.toEntity(): PendingUploadEntity = PendingUploadEntity(
    id = id,
    sessionUuid = sessionUUID,
    zipFilename = zipFilename,
    absolutePath = absolutePath,
    enqueuedAtMs = enqueuedAtMs,
    attemptCount = attemptCount,
)
