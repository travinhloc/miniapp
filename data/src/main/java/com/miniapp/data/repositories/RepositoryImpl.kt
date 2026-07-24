package com.miniapp.data.repositories

import com.miniapp.data.extensions.flowTransform
import com.miniapp.data.remote.models.responses.toModels
import com.miniapp.data.remote.services.ApiService
import com.miniapp.domain.models.Model
import com.miniapp.domain.repositories.Repository
import kotlinx.coroutines.flow.Flow

class RepositoryImpl(
    private val apiService: ApiService
) : Repository {

    override fun getModels(): Flow<List<Model>> = flowTransform {
        apiService.getResponses().toModels()
    }
}
