@file:Suppress("ComplexMethod", "MatchingDeclarationName")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MediaAlbumMerge
import com.vault.vanishx.domain.model.MediaAlbumState

internal data class MediaViewerPage(
    val messageId: String,
    val mediaPath: String?,
    val mediaKind: String,
    val direction: String,
    val sentAt: Long,
)

internal fun buildMediaViewerPages(
    messages: List<ChatMessage>,
    albums: List<MediaAlbumState>,
    focusMessageId: String? = null,
): List<MediaViewerPage> {
    val focusAlbum = focusMessageId?.let { MediaAlbumMerge.findAlbumForMessage(it, albums) }
    if (focusAlbum != null) {
        return focusAlbum.items
            .filter { it.isVisual }
            .mapIndexed { index, item ->
                MediaViewerPage(
                    messageId = item.sentMessageId ?: "${focusAlbum.id}_$index",
                    mediaPath = item.previewPath,
                    mediaKind = item.kind,
                    direction = focusAlbum.direction,
                    sentAt = focusAlbum.sentAt,
                )
            }
    }
    val albumsById = albums.associateBy { it.id }
    val pages = ArrayList<MediaViewerPage>()
    for (msg in messages.sortedBy { it.sentAt }) {
        when (msg.mediaKind) {
            AttachmentMeta.KIND_ALBUM -> {
                val album = albumsById[msg.id] ?: continue
                album.items.filter { it.isVisual }.forEachIndexed { index, item ->
                    pages += MediaViewerPage(
                        messageId = item.sentMessageId ?: "${msg.id}_$index",
                        mediaPath = item.previewPath,
                        mediaKind = item.kind,
                        direction = msg.direction,
                        sentAt = msg.sentAt,
                    )
                }
            }
            AttachmentMeta.KIND_IMAGE, AttachmentMeta.KIND_VIDEO -> {
                val path = msg.mediaLocalPath ?: continue
                pages += MediaViewerPage(
                    messageId = msg.id,
                    mediaPath = path,
                    mediaKind = msg.mediaKind,
                    direction = msg.direction,
                    sentAt = msg.sentAt,
                )
            }
        }
    }
    return pages
}

internal fun findMediaViewerPageIndex(
    pages: List<MediaViewerPage>,
    messageId: String,
): Int = pages.indexOfFirst { it.messageId == messageId }.coerceAtLeast(0)
