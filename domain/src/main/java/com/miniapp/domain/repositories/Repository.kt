package com.miniapp.domain.repositories

import com.miniapp.domain.models.Model
import kotlinx.coroutines.flow.Flow

interface Repository {

    fun getModels(): Flow<List<Model>>
}
