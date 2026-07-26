package com.vault.vanishx.di.modules

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.vault.vanishx.data.push.FirebaseRoomPushTopics
import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.data.remote.FirebaseMailboxRemoteDataSource
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindModule {

    @Binds
    @Singleton
    abstract fun bindMailboxRemoteDataSource(
        impl: FirebaseMailboxRemoteDataSource,
    ): MailboxRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRoomPushTopics(
        impl: FirebaseRoomPushTopics,
    ): RoomPushTopics
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseProvideModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
