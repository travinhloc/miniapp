package com.vault.vanishx.di.modules

import com.vault.vanishx.data.repository.MailboxRepositoryImpl
import com.vault.vanishx.domain.repository.MailboxRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMailboxRepository(impl: MailboxRepositoryImpl): MailboxRepository
}
