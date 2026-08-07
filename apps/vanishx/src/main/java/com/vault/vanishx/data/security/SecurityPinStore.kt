package com.vault.vanishx.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

enum class PinVerifyResult {
    UNLOCK,
    PANIC,
    INVALID,
}

/**
 * Unlock PIN and Panic PIN (HMAC-SHA256 + salt) in EncryptedSharedPreferences.
 */
@Singleton
@Suppress("TooManyFunctions")
class SecurityPinStore (
    private val prefs: SharedPreferences,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(createEncryptedPrefs(context))

    private val random = SecureRandom()

    fun hasUnlockPin(): Boolean = prefs.contains(KEY_UNLOCK_HASH)

    fun hasPanicPin(): Boolean = prefs.contains(KEY_PANIC_HASH)

    fun setUnlockPin(pin: String) {
        requireValidPin(pin)
        writePin(KEY_UNLOCK_SALT, KEY_UNLOCK_HASH, pin)
    }

    fun setPanicPin(pin: String) {
        requireValidPin(pin)
        if (matchesStored(pin, KEY_UNLOCK_SALT, KEY_UNLOCK_HASH)) {
            error("Panic PIN must differ from unlock PIN")
        }
        writePin(KEY_PANIC_SALT, KEY_PANIC_HASH, pin)
    }

    fun clearUnlockPin() {
        prefs.edit()
            .remove(KEY_UNLOCK_SALT)
            .remove(KEY_UNLOCK_HASH)
            .apply()
    }

    fun clearPanicPin() {
        prefs.edit()
            .remove(KEY_PANIC_SALT)
            .remove(KEY_PANIC_HASH)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun verify(pin: String): PinVerifyResult = when {
        pin.isBlank() -> PinVerifyResult.INVALID
        matchesStored(pin, KEY_PANIC_SALT, KEY_PANIC_HASH) -> PinVerifyResult.PANIC
        matchesStored(pin, KEY_UNLOCK_SALT, KEY_UNLOCK_HASH) -> PinVerifyResult.UNLOCK
        else -> PinVerifyResult.INVALID
    }

    private fun writePin(saltKey: String, hashKey: String, pin: String) {
        val salt = newSalt()
        prefs.edit()
            .putString(saltKey, encode(salt))
            .putString(hashKey, hash(pin, salt))
            .apply()
    }

    private fun matchesStored(pin: String, saltKey: String, hashKey: String): Boolean {
        val saltEncoded = prefs.getString(saltKey, null)
        val expected = prefs.getString(hashKey, null)
        if (saltEncoded == null || expected == null) return false
        return hash(pin, decode(saltEncoded)) == expected
    }

    fun failedUnlockAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    @Suppress("ReturnCount")
    fun recordFailedUnlock(nowEpochMs: Long = System.currentTimeMillis()): UnlockFailResult {
        val next = failedUnlockAttempts() + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, next).apply()
        if (next < MAX_UNLOCK_ATTEMPTS) {
            return UnlockFailResult.Wrong(attemptsLeft = MAX_UNLOCK_ATTEMPTS - next)
        }
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
        if (isAutoWipeEnabled()) {
            return UnlockFailResult.Wipe
        }
        val tier = cooldownTier().coerceIn(0, COOLDOWN_DURATIONS_MS.lastIndex)
        val durationMs = COOLDOWN_DURATIONS_MS[tier]
        val until = nowEpochMs + durationMs
        prefs.edit()
            .putLong(KEY_COOLDOWN_UNTIL, until)
            .putInt(KEY_COOLDOWN_TIER, (tier + 1).coerceAtMost(COOLDOWN_DURATIONS_MS.lastIndex))
            .apply()
        return UnlockFailResult.Cooldown(
            untilEpochMs = until,
            durationMs = durationMs,
            tierIndex = tier,
        )
    }

    fun clearFailedUnlockAttempts() {
        prefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_COOLDOWN_UNTIL)
            .putInt(KEY_COOLDOWN_TIER, 0)
            .apply()
    }

    fun cooldownUntilEpochMs(): Long = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)

    fun cooldownTier(): Int = prefs.getInt(KEY_COOLDOWN_TIER, 0)

    fun isInCooldown(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val until = cooldownUntilEpochMs()
        return until > nowEpochMs
    }

    fun remainingCooldownMs(nowEpochMs: Long = System.currentTimeMillis()): Long =
        (cooldownUntilEpochMs() - nowEpochMs).coerceAtLeast(0L)

    fun clearExpiredCooldown(nowEpochMs: Long = System.currentTimeMillis()) {
        if (cooldownUntilEpochMs() in 1..nowEpochMs) {
            prefs.edit().remove(KEY_COOLDOWN_UNTIL).apply()
        }
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isFlagSecureEnabled(): Boolean = prefs.getBoolean(KEY_FLAG_SECURE, true)

    fun setFlagSecureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLAG_SECURE, enabled).apply()
    }

    fun isAutoWipeEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_WIPE, false)

    fun setAutoWipeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_WIPE, enabled).apply()
    }

    private fun requireValidPin(pin: String) {
        require(pin.length == PIN_LENGTH) { "PIN must be $PIN_LENGTH digits" }
        require(pin.all { it.isDigit() }) { "PIN must be numeric" }
    }

    private fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { random.nextBytes(it) }

    private fun hash(pin: String, salt: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALG)
        mac.init(SecretKeySpec(salt, HMAC_ALG))
        return encode(mac.doFinal(pin.toByteArray(Charsets.UTF_8)))
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    companion object {
        const val PREF_FILE = "vanishx_security_pin_prefs"
        /** Story 5.3: unlock / panic PIN are exactly 4 digits. */
        const val PIN_LENGTH = 4
        const val PIN_MIN_LENGTH = PIN_LENGTH
        const val PIN_MAX_LENGTH = PIN_LENGTH
        const val MAX_UNLOCK_ATTEMPTS = 5
        /** Progressive lockout after each failed streak: 60s → 5m → 24h. */
        val COOLDOWN_DURATIONS_MS: LongArray = longArrayOf(
            60_000L,
            5 * 60_000L,
            24 * 60 * 60_000L,
        )
        private const val SALT_BYTES = 16
        private const val HMAC_ALG = "HmacSHA256"
        private const val KEY_UNLOCK_SALT = "unlock_salt"
        private const val KEY_UNLOCK_HASH = "unlock_hash"
        private const val KEY_PANIC_SALT = "panic_salt"
        private const val KEY_PANIC_HASH = "panic_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_unlock_attempts"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FLAG_SECURE = "flag_secure_enabled"
        private const val KEY_AUTO_WIPE = "auto_wipe_enabled"
        private const val KEY_COOLDOWN_UNTIL = "cooldown_until_epoch_ms"
        private const val KEY_COOLDOWN_TIER = "cooldown_tier"

        fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return EncryptedSharedPreferences.create(
                PREF_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
