package com.vault.vanishx.di.modules

import com.vault.vanishx.data.crypto.IdentityKeyStore
import com.vault.vanishx.data.crypto.TinkIdentityKeyStore
import com.vault.vanishx.data.local.db.VanishxLocalDatabase
import com.vault.vanishx.data.repository.BlockRepositoryImpl
import com.vault.vanishx.data.repository.IdentityRepositoryImpl
import com.vault.vanishx.data.repository.MailboxRepositoryImpl
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.LocalDatabaseWiper
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

    @Binds
    @Singleton
    abstract fun bindBlockRepository(impl: BlockRepositoryImpl): BlockRepository

    @Binds
    @Singleton
    abstract fun bindIdentityRepository(impl: IdentityRepositoryImpl): IdentityRepository

    @Binds
    @Singleton
    abstract fun bindIdentityKeyStore(impl: TinkIdentityKeyStore): IdentityKeyStore

    @Binds
    @Singleton
    abstract fun bindLocalDatabaseWiper(impl: VanishxLocalDatabase): LocalDatabaseWiper
}
