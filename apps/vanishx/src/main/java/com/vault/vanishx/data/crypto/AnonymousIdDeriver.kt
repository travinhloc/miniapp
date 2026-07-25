package com.vault.vanishx.data.crypto

import java.security.MessageDigest
import java.util.Base64

/**
 * Derives a stable, displayable anonymous id from Ed25519 public key bytes.
 * Does not use advertising ID, ANDROID_ID, phone, or email.
 */
object AnonymousIdDeriver {

    private const val PREFIX = "vx_"
    private const val ID_BODY_LENGTH = 22

    fun fromPublicKeyBytes(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
        val body = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(digest)
            .take(ID_BODY_LENGTH)
        return PREFIX + body
    }

    fun publicKeyBase64(publicKeyBytes: ByteArray): String =
        Base64.getEncoder().encodeToString(publicKeyBytes)
}
