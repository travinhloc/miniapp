package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class ConversationPreviewResolverTest {

    @Test
    fun `waiting hides last message body`() {
        val preview = ConversationPreviewResolver.resolve(
            waiting = true,
            expired = false,
            isPro = false,
            left = false,
            lastMessage = text("icebreaker"),
            nowMs = NOW,
        )
        preview.kind shouldBe ConversationPreviewKind.Waiting
        preview.snippet shouldBe null
    }

    @Test
    fun `expired free hides plaintext`() {
        val preview = ConversationPreviewResolver.resolve(
            waiting = false,
            expired = true,
            isPro = false,
            left = false,
            lastMessage = text("secret"),
            nowMs = NOW,
        )
        preview.kind shouldBe ConversationPreviewKind.Expired
        preview.snippet shouldBe null
    }

    @Test
    fun `expired pro keeps text snippet`() {
        val preview = ConversationPreviewResolver.resolve(
            waiting = false,
            expired = true,
            isPro = true,
            left = false,
            lastMessage = text("kept"),
            nowMs = NOW,
        )
        preview.kind shouldBe ConversationPreviewKind.Text
        preview.snippet shouldBe "kept"
    }

    @Test
    fun `recalled and sensitive are generic`() {
        ConversationPreviewResolver.resolve(
            waiting = false,
            expired = false,
            isPro = false,
            left = false,
            lastMessage = text("gone", recalled = true),
            nowMs = NOW,
        ).kind shouldBe ConversationPreviewKind.Recalled

        ConversationPreviewResolver.resolve(
            waiting = false,
            expired = false,
            isPro = false,
            left = false,
            lastMessage = text("hold", sensitive = true),
            nowMs = NOW,
        ).let { preview ->
            preview.kind shouldBe ConversationPreviewKind.Sensitive
            preview.snippet shouldBe null
        }
    }

    @Test
    fun `expired visible message is skipped`() {
        val preview = ConversationPreviewResolver.resolve(
            waiting = false,
            expired = false,
            isPro = false,
            left = false,
            lastMessage = text("old", expiresAt = NOW - 1L),
            nowMs = NOW,
        )
        preview.kind shouldBe ConversationPreviewKind.Empty
        preview.snippet shouldBe null
    }

    @Test
    fun `unread counts inbound after last read`() {
        val messages = listOf(
            text("a", id = "1", direction = ChatMessage.DIRECTION_IN, sentAt = 1L),
            text("b", id = "2", direction = ChatMessage.DIRECTION_OUT, sentAt = 2L),
            text("c", id = "3", direction = ChatMessage.DIRECTION_IN, sentAt = 3L),
        )
        ConversationPreviewResolver.unreadInboundCount(messages, lastReadMessageId = "1", nowMs = NOW) shouldBe 1
        ConversationPreviewResolver.unreadInboundCount(messages, lastReadMessageId = null, nowMs = NOW) shouldBe 2
    }

    private fun text(
        body: String,
        id: String = "m",
        direction: String = ChatMessage.DIRECTION_IN,
        sentAt: Long = NOW - 1_000L,
        expiresAt: Long = NOW + 60_000L,
        recalled: Boolean = false,
        sensitive: Boolean = false,
    ) = ChatMessage(
        id = id,
        roomId = "r",
        body = body,
        sentAt = sentAt,
        expiresAt = expiresAt,
        direction = direction,
        recalled = recalled,
        sensitive = sensitive,
    )

    private companion object {
        const val NOW = 1_000_000L
    }
}
