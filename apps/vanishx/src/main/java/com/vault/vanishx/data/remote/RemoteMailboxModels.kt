package com.vault.vanishx.data.remote

data class RemoteRoomMeta(
    val createdAt: Long,
    val expiresAt: Long,
    val creatorPub: String? = null,
)

data class RemoteMailboxMessage(
    val messageId: String,
    val ciphertext: String,
    val senderPub: String,
    val createdAt: Long,
    val expiresAt: Long,
) {
    companion object {
        /** Base64("test") — staging smoke payload only. */
        const val SMOKE_CIPHERTEXT = "dGVzdA=="
        const val MAX_CIPHERTEXT_LENGTH = 16_384
    }
}
