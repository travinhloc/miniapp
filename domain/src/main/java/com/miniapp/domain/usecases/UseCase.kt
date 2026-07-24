package com.miniapp.domain.usecases

import com.miniapp.domain.models.Model
import com.miniapp.domain.repositories.Repository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UseCase @Inject constructor(private val repository: Repository) {

    operator fun invoke(): Flow<List<Model>> {
        return repository.getModels()
    }
}
