package com.vault.vanishx.data.crypto

import com.vault.vanishx.domain.model.Identity

/**
 * Abstraction over Tink keyset persistence so unit tests can fake bootstrap.
 */
interface IdentityKeyStore {
    /**
     * Loads existing identity key material or creates a new Ed25519 keyset.
     * Must be idempotent across process restarts when backed by durable storage.
     */
    fun getOrCreateIdentity(): Identity
}
