package com.sfm.scanner.core.storage.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module anchor for core-storage.
 * [AppFileProvider] and [SessionDirectoryManager] are self-bound via @Inject constructor.
 * [ZipBuilder] is unscoped — each injection site receives a dedicated instance to avoid
 * concurrent ZipOutputStream state conflicts.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule
