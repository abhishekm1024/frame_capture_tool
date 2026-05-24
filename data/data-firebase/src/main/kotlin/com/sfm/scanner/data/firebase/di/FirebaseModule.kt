package com.sfm.scanner.data.firebase.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.sfm.scanner.data.firebase.FirebaseRepository
import com.sfm.scanner.data.firebase.FirebaseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {

    @Binds
    @Singleton
    abstract fun bindFirebaseRepository(impl: FirebaseRepositoryImpl): FirebaseRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
    }
}
