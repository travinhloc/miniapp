package com.vault.vanishx.data.invite

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a single pending invite URI across cold start / post-install (story 3.1).
 */
@Singleton
class PendingInviteStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy { createPrefs() }

    @Synchronized
    fun save(uri: String) {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_PENDING_URI, trimmed).apply()
    }

    @Synchronized
    fun peek(): String? = prefs.getString(KEY_PENDING_URI, null)?.takeIf { it.isNotBlank() }

    @Synchronized
    fun consume(): String? {
        val value = peek() ?: return null
        prefs.edit().remove(KEY_PENDING_URI).apply()
        return value
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_PENDING_URI).apply()
    }

    private fun createPrefs() = EncryptedSharedPreferences.create(
        PREF_FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private companion object {
        const val PREF_FILE = "vanishx_pending_invite_prefs"
        const val KEY_PENDING_URI = "pending_invite_uri"
    }
}
