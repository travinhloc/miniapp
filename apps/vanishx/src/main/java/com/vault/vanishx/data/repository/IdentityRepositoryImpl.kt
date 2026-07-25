package com.vault.vanishx.data.repository

import com.miniapp.core.common.DispatchersProvider
import com.vault.vanishx.data.crypto.IdentityKeyStore
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.repository.IdentityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class IdentityRepositoryImpl @Inject constructor(
    private val identityKeyStore: IdentityKeyStore,
    private val dispatchersProvider: DispatchersProvider,
) : IdentityRepository {

    override suspend fun ensureIdentity(): Identity = withContext(dispatchersProvider.io) {
        identityKeyStore.getOrCreateIdentity()
    }
}
