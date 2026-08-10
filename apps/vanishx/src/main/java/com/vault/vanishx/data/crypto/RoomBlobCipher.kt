package com.vault.vanishx.data.crypto

import android.util.Base64
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.subtle.AesGcmJce
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-scoped AES-256-GCM for media blobs (Epic 11).
 * AAD = UTF-8 `"$roomId|$attId"`.
 */
@Singleton
class RoomBlobCipher @Inject constructor() {

    init {
        AeadConfig.register()
    }

    fun encrypt(roomId: String, attId: String, roomKey: String, plaintext: ByteArray): ByteArray {
        require(plaintext.isNotEmpty()) { "plaintext must not be empty" }
        require(attId.isNotBlank()) { "attId required" }
        return crypt(roomId, attId, roomKey) { aead, aad -> aead.encrypt(plaintext, aad) }
    }

    fun decrypt(roomId: String, attId: String, roomKey: String, ciphertext: ByteArray): ByteArray {
        require(ciphertext.isNotEmpty()) { "ciphertext must not be empty" }
        require(attId.isNotBlank()) { "attId required" }
        return crypt(roomId, attId, roomKey) { aead, aad -> aead.decrypt(ciphertext, aad) }
    }

    private fun crypt(
        roomId: String,
        attId: String,
        roomKey: String,
        operation: (AesGcmJce, ByteArray) -> ByteArray,
    ): ByteArray = try {
        operation(
            AesGcmJce(decodeKey(roomKey)),
            "$roomId|$attId".toByteArray(StandardCharsets.UTF_8),
        )
    } catch (e: GeneralSecurityException) {
        throw RoomCryptoException("Failed to process room blob", e)
    } catch (e: IllegalArgumentException) {
        throw RoomCryptoException("Invalid room blob input", e)
    }

    private fun decodeKey(roomKey: String): ByteArray {
        val keyBytes = Base64.decode(
            roomKey,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        require(keyBytes.size == RoomMessageCipher.KEY_SIZE_BYTES) {
            "roomKey must decode to ${RoomMessageCipher.KEY_SIZE_BYTES} bytes"
        }
        return keyBytes
    }
}
