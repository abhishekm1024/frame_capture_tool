package com.sfm.scanner.feature.upload.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.sfm.scanner.feature.upload.UploadQueueRepository
import com.sfm.scanner.feature.upload.UploadWorkEnqueuer
import com.sfm.scanner.feature.upload.db.PendingUploadDao
import com.sfm.scanner.feature.upload.db.ScanDatabase
import com.sfm.scanner.feature.upload.db.UploadQueueRepositoryImpl
import com.sfm.scanner.feature.upload.work.UploadWorkEnqueuerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UploadQueueModule {

    @Binds
    @Singleton
    abstract fun bindUploadQueueRepository(
        impl: UploadQueueRepositoryImpl,
    ): UploadQueueRepository

    @Binds
    @Singleton
    abstract fun bindUploadWorkEnqueuer(
        impl: UploadWorkEnqueuerImpl,
    ): UploadWorkEnqueuer

    companion object {

        @Provides
        @Singleton
        fun provideScanDatabase(@ApplicationContext context: Context): ScanDatabase =
            Room.databaseBuilder(
                context,
                ScanDatabase::class.java,
                "scan_database",
            ).build()

        @Provides
        fun providePendingUploadDao(database: ScanDatabase): PendingUploadDao =
            database.pendingUploadDao()

        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)
    }
}
