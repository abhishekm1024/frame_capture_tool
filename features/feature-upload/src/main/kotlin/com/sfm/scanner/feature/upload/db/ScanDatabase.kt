package com.sfm.scanner.feature.upload.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PendingUploadEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class ScanDatabase : RoomDatabase() {
    abstract fun pendingUploadDao(): PendingUploadDao
}
