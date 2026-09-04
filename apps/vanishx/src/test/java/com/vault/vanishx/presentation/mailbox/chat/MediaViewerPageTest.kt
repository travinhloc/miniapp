package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MediaAlbumItem
import com.vault.vanishx.domain.model.MediaAlbumState
import io.kotest.matchers.shouldBe
import org.junit.Test

class MediaViewerPageTest {

    @Test
    fun `opening album returns only its items in collage order`() {
        val album = MediaAlbumState(
            id = "album_1",
            sentAt = 1L,
            direction = ChatMessage.DIRECTION_IN,
            items = (0..3).map { index ->
                MediaAlbumItem(
                    uri = "/tmp/$index.jpg",
                    mime = "image/jpeg",
                    displayName = "$index.jpg",
                    kind = AttachmentMeta.KIND_IMAGE,
                    localPath = "/tmp/$index.jpg",
                    sentMessageId = "m$index",
                )
            },
        )

        val pages = buildMediaViewerPages(
            messages = emptyList(),
            albums = listOf(album),
            focusMessageId = album.id,
        )

        pages.map { it.messageId } shouldBe listOf("m0", "m1", "m2", "m3")
        pages.map { it.direction }.distinct() shouldBe listOf(ChatMessage.DIRECTION_IN)
        findMediaViewerPageIndex(pages, "m2") shouldBe 2
    }

    @Test
    fun `pending album items retain stable pager ids`() {
        val album = MediaAlbumState(
            id = "album_pending",
            sentAt = 1L,
            items = (0..2).map { index ->
                MediaAlbumItem(
                    uri = "/tmp/$index.jpg",
                    mime = "image/jpeg",
                    displayName = null,
                    kind = AttachmentMeta.KIND_IMAGE,
                    localPath = "/tmp/$index.jpg",
                )
            },
        )

        val pages = buildMediaViewerPages(emptyList(), listOf(album), album.id)

        pages.map { it.messageId } shouldBe listOf(
            "album_pending_0",
            "album_pending_1",
            "album_pending_2",
        )
    }
}
