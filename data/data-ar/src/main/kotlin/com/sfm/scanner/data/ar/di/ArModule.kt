package com.sfm.scanner.data.ar.di

import com.sfm.scanner.data.ar.ArRepository
import com.sfm.scanner.data.ar.ArRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ArModule {

    @Binds
    @Singleton
    abstract fun bindArRepository(impl: ArRepositoryImpl): ArRepository
}
