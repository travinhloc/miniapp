package com.vault.vanishx.data.crypto

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.Ed25519PublicKey
import com.google.crypto.tink.signature.SignatureConfig
import com.vault.vanishx.domain.model.Identity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ed25519 identity keyset encrypted with Android Keystore master key via Tink.
 *
 * Pref file: [PREF_FILE] — excluded from cloud backup.
 */
@Singleton
class TinkIdentityKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : IdentityKeyStore {

    @Volatile
    private var cached: Identity? = null

    override fun getOrCreateIdentity(): Identity {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: loadOrCreate().also { cached = it }
        }
    }

    override fun clear() {
        synchronized(this) {
            cached = null
            context.deleteSharedPreferences(PREF_FILE)
        }
    }

    private fun loadOrCreate(): Identity {
        try {
            SignatureConfig.register()
            val manager = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE)
                .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
            return identityFromHandle(manager.keysetHandle)
        } catch (e: GeneralSecurityException) {
            throw IdentityCryptoException("Failed to bootstrap VanishX identity keyset", e)
        }
    }

    private fun identityFromHandle(privateHandle: KeysetHandle): Identity {
        val publicHandle = privateHandle.publicKeysetHandle
        val publicKeyBytes = extractEd25519PublicKeyBytes(publicHandle)
        return Identity(
            anonymousId = AnonymousIdDeriver.fromPublicKeyBytes(publicKeyBytes),
            publicKeyBase64 = AnonymousIdDeriver.publicKeyBase64(publicKeyBytes),
            cryptoSchemeVersion = Identity.CRYPTO_SCHEME_VERSION,
        )
    }

    private fun extractEd25519PublicKeyBytes(publicHandle: KeysetHandle): ByteArray {
        for (index in 0 until publicHandle.size()) {
            val key = publicHandle.getAt(index).key
            if (key is Ed25519PublicKey) {
                return key.publicKeyBytes.toByteArray()
            }
        }
        throw IdentityCryptoException("Ed25519 public key not found in keyset")
    }

    companion object {
        const val PREF_FILE = "vanishx_identity_prefs"
        const val KEYSET_NAME = "vanishx_identity_keyset"
        const val MASTER_KEY_URI = "android-keystore://vanishx_identity_master_key"
        const val KEY_TEMPLATE = "ED25519"
    }
}

class IdentityCryptoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
