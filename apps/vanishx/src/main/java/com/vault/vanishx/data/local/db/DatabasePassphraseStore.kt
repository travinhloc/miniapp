package com.vault.vanishx.data.local.db

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Random 32-byte SQLCipher passphrase, stored via EncryptedSharedPreferences + MasterKeys.
 */
@Singleton
class DatabasePassphraseStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs by lazy { createPrefs() }

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val bytes = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(bytes, Base64.NO_WRAP))
            .apply()
        return bytes
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun createPrefs(): android.content.SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREF_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        const val PREF_FILE = "vanishx_db_passphrase_prefs"
        private const val KEY_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_BYTES = 32
    }
}
