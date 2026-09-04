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
    val direction: String = ChatMessage.DIRECTION_OUT,
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
        direction = direction,
        mediaKind = AttachmentMeta.KIND_ALBUM,
        mediaTransferStatus = transferStatus,
        mediaLocalPath = items.firstOrNull()?.previewPath,
        mediaFileName = items.size.toString(),
    )
}

object MediaAlbumMerge {
    /** Outgoing/incoming burst window for rebuilding album bubbles after sync. */
    private const val ALBUM_BURST_MS = 5_000L

    /** Client + reconstructed albums for merge and media viewer scoping. */
    fun resolveAlbums(
        messages: List<ChatMessage>,
        outgoing: List<MediaAlbumState>,
    ): List<MediaAlbumState> = outgoing + reconstructAlbumsFromMessages(messages, outgoing)

    /** Hide wire member messages covered by an album bubble; inject album chat rows. */
    fun merge(synced: List<ChatMessage>, albums: List<MediaAlbumState>, roomId: String): List<ChatMessage> {
        val allAlbums = resolveAlbums(synced, albums)
        if (allAlbums.isEmpty()) return synced
        val hidden = allAlbums.flatMap { it.memberMessageIds }.toSet()
        val albumRows = allAlbums.map { it.toChatMessage(roomId) }
        return (synced.filterNot { it.id in hidden || it.mediaKind == AttachmentMeta.KIND_ALBUM } + albumRows)
            .distinctBy { it.id }
            .sortedBy { it.sentAt }
    }

    fun findAlbumForMessage(messageId: String, albums: List<MediaAlbumState>): MediaAlbumState? {
        albums.firstOrNull { it.id == messageId }?.let { return it }
        return albums.firstOrNull { messageId in it.memberMessageIds }
    }

    /**
     * Re-group visual wire messages into album bubbles when [outgoingAlbums] was lost
     * (e.g. after re-entering the room).
     */
    @Suppress("ComplexMethod", "NestedBlockDepth")
    internal fun reconstructAlbumsFromMessages(
        messages: List<ChatMessage>,
        existing: List<MediaAlbumState>,
    ): List<MediaAlbumState> {
        val covered = existing.flatMap { it.memberMessageIds }.toSet()
        val existingIds = existing.map { it.id }.toSet()
        val visual = messages.filter { msg ->
            (msg.mediaKind == AttachmentMeta.KIND_IMAGE || msg.mediaKind == AttachmentMeta.KIND_VIDEO) &&
                msg.id !in covered &&
                !msg.mediaLocalPath.isNullOrBlank()
        }.sortedBy { it.sentAt }
        if (visual.size < 2) return emptyList()

        // New envelopes carry an exact encrypted album id. Keep the time-window
        // reconstruction below only as backwards compatibility for older messages.
        val exactGroups = visual
            .filter { !it.mediaAlbumId.isNullOrBlank() }
            .groupBy { checkNotNull(it.mediaAlbumId) }
            .filterValues { it.size > 1 }
        val legacyVisual = visual.filter { it.mediaAlbumId.isNullOrBlank() }
        val legacyGroups = mutableListOf<List<ChatMessage>>()
        var bucket = mutableListOf<ChatMessage>()
        for (msg in legacyVisual) {
            if (bucket.isEmpty()) {
                bucket.add(msg)
                continue
            }
            val last = bucket.last()
            val sameBurst = last.direction == msg.direction &&
                msg.sentAt - last.sentAt <= ALBUM_BURST_MS &&
                bucket.size < MediaLimits.PHOTO_MULTI_SELECT_MAX
            if (sameBurst) {
                bucket.add(msg)
            } else {
                if (bucket.size > 1) legacyGroups += bucket.toList()
                bucket = mutableListOf(msg)
            }
        }
        if (bucket.size > 1) legacyGroups += bucket
        val groups = exactGroups.map { (albumId, group) -> albumId to group.sortedBy { it.sentAt } } +
            legacyGroups.map { group -> "album_reconstructed_${group.first().id}" to group }
        return groups.mapNotNull { (albumId, group) ->
            if (albumId in existingIds) return@mapNotNull null
            MediaAlbumState(
                id = albumId,
                sentAt = group.first().sentAt,
                direction = group.first().direction,
                items = group.map { msg ->
                    MediaAlbumItem(
                        uri = msg.mediaLocalPath.orEmpty(),
                        mime = msg.mediaMime,
                        displayName = msg.mediaFileName,
                        kind = msg.mediaKind ?: AttachmentMeta.KIND_IMAGE,
                        status = msg.mediaTransferStatus ?: ChatMessage.MEDIA_READY,
                        localPath = msg.mediaLocalPath,
                        sentMessageId = msg.id,
                    )
                },
            )
        }
    }

    fun isVisualQueue(items: List<Pair<String?, String?>>): Boolean =
        items.size > 1 && items.all { (mime, name) ->
            val kind = MediaLimits.kindForMimeOrName(mime, name)
            kind == AttachmentMeta.KIND_IMAGE || kind == AttachmentMeta.KIND_VIDEO
        }
}
