package com.miniapp.di.modules

import com.miniapp.data.remote.services.ApiService
import com.miniapp.data.repositories.RepositoryImpl
import com.miniapp.domain.repositories.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
class RepositoryModule {

    @Provides
    fun provideRepository(apiService: ApiService): Repository = RepositoryImpl(apiService)
}
