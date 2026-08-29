@file:Suppress("ComplexMethod", "ReturnCount", "LongParameterList", "MaxLineLength")

package com.vault.vanishx.domain.model

enum class ConversationPreviewKind {
    Waiting,
    Expired,
    Recalled,
    Sensitive,
    Image,
    File,
    Video,
    Voice,
    Text,
    Empty,
    Left,
}

data class ConversationPreview(
    val kind: ConversationPreviewKind,
    val snippet: String? = null,
    val lastActivityAt: Long = 0L,
    val outbound: Boolean = false,
)

object ConversationPreviewResolver {
    fun resolve(
        waiting: Boolean,
        expired: Boolean,
        isPro: Boolean,
        left: Boolean,
        lastMessage: ChatMessage?,
        nowMs: Long,
    ): ConversationPreview {
        if (left) {
            return ConversationPreview(kind = ConversationPreviewKind.Left, lastActivityAt = lastMessage?.sentAt ?: 0L)
        }
        if (waiting) {
            return ConversationPreview(kind = ConversationPreviewKind.Waiting)
        }
        if (expired && !isPro) {
            return ConversationPreview(kind = ConversationPreviewKind.Expired, lastActivityAt = lastMessage?.sentAt ?: 0L)
        }
        val visible = lastMessage?.takeUnless { messageExpired(it, nowMs) }
        if (visible == null) {
            return ConversationPreview(
                kind = if (expired) ConversationPreviewKind.Expired else ConversationPreviewKind.Empty,
            )
        }
        val outbound = visible.direction == ChatMessage.DIRECTION_OUT
        val kind = when {
            visible.recalled -> ConversationPreviewKind.Recalled
            visible.sensitive -> ConversationPreviewKind.Sensitive
            visible.mediaKind == AttachmentMeta.KIND_IMAGE -> ConversationPreviewKind.Image
            visible.mediaKind == AttachmentMeta.KIND_FILE -> ConversationPreviewKind.File
            visible.mediaKind == AttachmentMeta.KIND_VIDEO -> ConversationPreviewKind.Video
            visible.mediaKind == AttachmentMeta.KIND_VOICE -> ConversationPreviewKind.Voice
            else -> ConversationPreviewKind.Text
        }
        val snippet = if (kind == ConversationPreviewKind.Text) {
            visible.body.trim().ifBlank { null }
        } else {
            null
        }
        return ConversationPreview(
            kind = kind,
            snippet = snippet,
            lastActivityAt = visible.sentAt,
            outbound = outbound,
        )
    }

    fun unreadInboundCount(
        messages: List<ChatMessage>,
        lastReadMessageId: String?,
        nowMs: Long,
    ): Int {
        val visibleInbound = messages.filter { msg ->
            msg.direction == ChatMessage.DIRECTION_IN && !messageExpired(msg, nowMs)
        }
        if (lastReadMessageId.isNullOrBlank()) return visibleInbound.size
        val lastRead = messages.firstOrNull { it.id == lastReadMessageId } ?: return visibleInbound.size
        return visibleInbound.count { it.sentAt > lastRead.sentAt }
    }

    private fun messageExpired(message: ChatMessage, nowMs: Long): Boolean =
        message.expiresAt > 0L && message.expiresAt <= nowMs
}
