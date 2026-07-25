package com.vault.vanishx.di.modules

import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.common.DispatchersProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideDispatchersProvider(): DispatchersProvider {
        return DispatchersProviderImpl()
    }
}
