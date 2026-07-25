package com.vault.vanishx.domain.model

/**
 * Local anonymous identity for this device install.
 * Private key material never leaves the secure keyset store.
 */
data class Identity(
    val anonymousId: String,
    val publicKeyBase64: String,
    val cryptoSchemeVersion: Int = CRYPTO_SCHEME_VERSION,
) {
    companion object {
        const val CRYPTO_SCHEME_VERSION: Int = 1
    }
}
