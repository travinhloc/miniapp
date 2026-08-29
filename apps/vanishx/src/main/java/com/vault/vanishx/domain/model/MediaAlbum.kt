package com.vault.vanishx.domain.model

/**
 * Client-side multi-media album (Zalo-like collage).
 * Wire still sends one envelope per item (v2); album is a UI aggregation for the sender
 * (and can group member message ids after upload).
 */
data class MediaAlbumItem(
    val uri: String,
    val mime: String?,
    val displayName: String?,
    val kind: String,
    val status: String = ChatMessage.MEDIA_PENDING,
    val localPath: String? = null,
    val sentMessageId: String? = null,
    val durationMs: Long = 0L,
) {
    val previewPath: String get() = localPath?.takeIf { it.isNotBlank() } ?: uri
    val isVisual: Boolean
        get() = kind == AttachmentMeta.KIND_IMAGE || kind == AttachmentMeta.KIND_VIDEO
}

data class MediaAlbumState(
    val id: String,
    val sentAt: Long,
    val items: List<MediaAlbumItem>,
) {
    val memberMessageIds: Set<String>
        get() = items.mapNotNull { it.sentMessageId }.toSet()

    val transferStatus: String
        get() = when {
            items.any { it.status == ChatMessage.MEDIA_PENDING } -> ChatMessage.MEDIA_PENDING
            items.any { it.status == ChatMessage.MEDIA_FAILED } &&
                items.none { it.status == ChatMessage.MEDIA_PENDING } -> ChatMessage.MEDIA_FAILED
            else -> ChatMessage.MEDIA_READY
        }

    fun toChatMessage(roomId: String): ChatMessage = ChatMessage(
        id = id,
        roomId = roomId,
        body = "",
        sentAt = sentAt,
        expiresAt = sentAt,
        direction = ChatMessage.DIRECTION_OUT,
        mediaKind = AttachmentMeta.KIND_ALBUM,
        mediaTransferStatus = transferStatus,
        mediaLocalPath = items.firstOrNull()?.previewPath,
        mediaFileName = items.size.toString(),
    )
}

object MediaAlbumMerge {
    /** Hide wire member messages covered by an album bubble; inject album chat rows. */
    fun merge(synced: List<ChatMessage>, albums: List<MediaAlbumState>, roomId: String): List<ChatMessage> {
        if (albums.isEmpty()) return synced
        val hidden = albums.flatMap { it.memberMessageIds }.toSet()
        val albumRows = albums.map { it.toChatMessage(roomId) }
        return (synced.filterNot { it.id in hidden } + albumRows)
            .distinctBy { it.id }
            .sortedBy { it.sentAt }
    }

    fun isVisualQueue(items: List<Pair<String?, String?>>): Boolean =
        items.size > 1 && items.all { (mime, name) ->
            val kind = MediaLimits.kindForMimeOrName(mime, name)
            kind == AttachmentMeta.KIND_IMAGE || kind == AttachmentMeta.KIND_VIDEO
        }
}
