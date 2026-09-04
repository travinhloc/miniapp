package com.vault.vanishx.domain.model

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class MediaAlbumMergeTest {

    @Test
    fun `encrypted album id restores collage outside legacy burst window`() {
        val messages = listOf(
            visualMessage("m1", sentAt = 1_000L, albumId = "album_1"),
            visualMessage("m2", sentAt = 31_000L, albumId = "album_1"),
        )

        val albums = MediaAlbumMerge.resolveAlbums(messages, emptyList())

        albums shouldHaveSize 1
        albums.single().id shouldBe "album_1"
        albums.single().memberMessageIds shouldBe setOf("m1", "m2")
        albums.single().direction shouldBe ChatMessage.DIRECTION_OUT
    }

    @Test
    fun `different encrypted album ids are not merged`() {
        val messages = listOf(
            visualMessage("m1", sentAt = 1_000L, albumId = "album_1"),
            visualMessage("m2", sentAt = 2_000L, albumId = "album_2"),
        )

        MediaAlbumMerge.resolveAlbums(messages, emptyList()) shouldBe emptyList()
    }

    @Test
    fun `resolved album survives member messages being hidden from presentation list`() {
        val raw = listOf(
            visualMessage("m1", sentAt = 1_000L, albumId = "album_1"),
            visualMessage("m2", sentAt = 2_000L, albumId = "album_1"),
        )
        val persistedUiAlbums = MediaAlbumMerge.resolveAlbums(raw, emptyList())
        val presentationMessages = MediaAlbumMerge.merge(raw, persistedUiAlbums, "room")

        presentationMessages.single().mediaKind shouldBe AttachmentMeta.KIND_ALBUM
        MediaAlbumMerge.resolveAlbums(presentationMessages, persistedUiAlbums) shouldBe persistedUiAlbums
    }

    private fun visualMessage(id: String, sentAt: Long, albumId: String) = ChatMessage(
        id = id,
        roomId = "room",
        body = "",
        sentAt = sentAt,
        expiresAt = sentAt + 60_000L,
        direction = ChatMessage.DIRECTION_OUT,
        mediaKind = AttachmentMeta.KIND_IMAGE,
        mediaMime = "image/jpeg",
        mediaLocalPath = "/tmp/$id.jpg",
        mediaTransferStatus = ChatMessage.MEDIA_READY,
        mediaAlbumId = albumId,
    )
}
