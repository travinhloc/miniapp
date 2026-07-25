package com.vault.vanishx.domain.repository

import com.vault.vanishx.domain.model.Identity

interface IdentityRepository {
    /**
     * Returns existing identity or creates one on first launch (idempotent).
     */
    suspend fun ensureIdentity(): Identity
}
