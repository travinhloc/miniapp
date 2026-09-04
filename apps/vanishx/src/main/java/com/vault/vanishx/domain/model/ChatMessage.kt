package com.vault.vanishx.domain.model

data class ChatMessage(
    val id: String,
    val roomId: String,
    val body: String,
    val sentAt: Long,
    val expiresAt: Long,
    val direction: String,
    val recalled: Boolean = false,
    /** Opt-in Confide-style blur; peer learns flag from encrypted envelope. */
    val sensitive: Boolean = false,
    /** Parent message id when this is a reply (E9-3). */
    val replyToId: String? = null,
    /** Epic 11 media — null for text. */
    val mediaKind: String? = null,
    val mediaMime: String? = null,
    val mediaBytes: Long? = null,
    val mediaAttId: String? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val mediaFileName: String? = null,
    val mediaAlbumId: String? = null,
    /** App-private path to decrypted (or ready) local bytes. */
    val mediaLocalPath: String? = null,
    val mediaTransferStatus: String? = null,
) {
    val isMedia: Boolean get() = !mediaKind.isNullOrBlank()

    companion object {
        const val DIRECTION_OUT = "out"
        const val DIRECTION_IN = "in"
        const val MEDIA_READY = "ready"
        const val MEDIA_PENDING = "pending"
        const val MEDIA_FAILED = "failed"
    }
}
