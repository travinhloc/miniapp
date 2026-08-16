package com.vault.vanishx.data.invite

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.vault.vanishx.domain.model.InviteClipboardParser
import com.vault.vanishx.domain.model.InvitePendingCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a single pending invite URI across cold start / lock / setup (stories 7.6 · 14.4).
 * Stored value is the canonical HTTPS invite when the input can be parsed.
 */
@Singleton
class PendingInviteStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy { createPrefs() }
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()
    private var hydrated = false

    @Synchronized
    fun save(uri: String) {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return
        val canonical = InvitePendingCodec.canonicalize(trimmed)
        val toStore = when {
            canonical != null -> canonical
            trimmed.startsWith("${InviteClipboardParser.PREFIX}:") -> return
            else -> trimmed
        }
        prefs.edit().putString(KEY_PENDING_URI, toStore).apply()
        hydrated = true
        _pending.value = toStore
    }

    @Synchronized
    fun peek(): String? {
        hydrateLocked()
        return _pending.value
    }

    @Synchronized
    fun consume(): String? {
        val value = peek() ?: return null
        clear()
        return value
    }

    @Synchronized
    fun clear() {
        hydrateLocked()
        prefs.edit().remove(KEY_PENDING_URI).apply()
        _pending.value = null
    }

    private fun hydrateLocked() {
        if (hydrated) return
        _pending.value = prefs.getString(KEY_PENDING_URI, null)?.takeIf { it.isNotBlank() }
        hydrated = true
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
