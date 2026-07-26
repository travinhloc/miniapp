package com.vault.vanishx.data.crypto

import android.util.Base64
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.subtle.AesGcmJce
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-scoped AES-256-GCM via Tink [AesGcmJce].
 * Key material = decoded invite [roomKey] (32 bytes).
 *
 * Wire format: `vx1.` + URL-safe Base64(IV ‖ ciphertext ‖ tag).
 * AAD = UTF-8 roomId (binds ciphertext to room).
 */
@Singleton
class RoomMessageCipher @Inject constructor() {

    init {
        AeadConfig.register()
    }

    fun encrypt(roomId: String, roomKey: String, plaintext: String): String {
        require(plaintext.isNotEmpty()) { "plaintext must not be empty" }
        try {
            val aead = AesGcmJce(decodeKey(roomKey))
            val ciphertext = aead.encrypt(
                plaintext.toByteArray(StandardCharsets.UTF_8),
                roomId.toByteArray(StandardCharsets.UTF_8),
            )
            return PREFIX + encode(ciphertext)
        } catch (e: GeneralSecurityException) {
            throw RoomCryptoException("Failed to encrypt room message", e)
        } catch (e: IllegalArgumentException) {
            throw RoomCryptoException("Invalid room key for encrypt", e)
        }
    }

    fun decrypt(roomId: String, roomKey: String, wire: String): String {
        require(wire.startsWith(PREFIX)) { "Unsupported ciphertext version" }
        try {
            val raw = decode(wire.removePrefix(PREFIX))
            val aead = AesGcmJce(decodeKey(roomKey))
            val plain = aead.decrypt(raw, roomId.toByteArray(StandardCharsets.UTF_8))
            return String(plain, StandardCharsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw RoomCryptoException("Failed to decrypt room message", e)
        } catch (e: IllegalArgumentException) {
            throw RoomCryptoException("Invalid ciphertext or room key", e)
        }
    }

    private fun decodeKey(roomKey: String): ByteArray {
        val keyBytes = decode(roomKey)
        require(keyBytes.size == KEY_SIZE_BYTES) {
            "roomKey must decode to $KEY_SIZE_BYTES bytes"
        }
        return keyBytes
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        const val PREFIX = "vx1."
        const val KEY_SIZE_BYTES = 32
    }
}

class RoomCryptoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
