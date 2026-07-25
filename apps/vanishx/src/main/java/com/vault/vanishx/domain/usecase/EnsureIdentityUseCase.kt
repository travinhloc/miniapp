package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.repository.IdentityRepository
import javax.inject.Inject

class EnsureIdentityUseCase @Inject constructor(
    private val identityRepository: IdentityRepository,
) {
    suspend operator fun invoke(): Identity = identityRepository.ensureIdentity()
}
