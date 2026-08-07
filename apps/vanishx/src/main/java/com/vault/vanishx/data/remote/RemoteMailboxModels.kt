package com.vault.vanishx.data.remote

data class RemoteRoomMeta(
    val createdAt: Long,
    /**
     * `0` while Free Host waits for guest enter, or forever for Pro Host.
     * After Free activate: absolute epoch ms (> now).
     */
    val expiresAt: Long,
    val creatorPub: String? = null,
    /** Optional one-line opener the guest sees before accepting (story 7.5). */
    val icebreaker: String? = null,
    /** True when the room creator had Pro at create — no room clock. */
    val hostPro: Boolean = false,
    /** Set when the guest first enters the room (activate). */
    val activatedAt: Long? = null,
) {
    companion object {
        const val MAX_ICEBREAKER_LENGTH = 80
    }
}

data class RemoteMailboxMessage(
    val messageId: String,
    val ciphertext: String,
    val senderPub: String,
    val createdAt: Long,
    val expiresAt: Long,
) {
    companion object {
        const val MAX_CIPHERTEXT_LENGTH = 16_384
    }
}

/** UGC report payload (story 3.3) — write-only RTDB node. */
data class RemoteReport(
    val reportId: String,
    val roomId: String,
    val reporterPub: String,
    val peerPub: String? = null,
    val reason: String? = null,
    val createdAt: Long,
) {
    companion object {
        const val MAX_REASON_LENGTH = 500
        const val MAX_ROOM_ID_LENGTH = 64
        const val MAX_PUB_LENGTH = 512
    }
}
