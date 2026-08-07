package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date

internal fun findRecallableMessage(
    messages: List<ChatMessage>,
    isPro: Boolean,
    isExpired: Boolean,
    isRecalling: Boolean,
): ChatMessage? {
    val canRecall = isPro && !isExpired && !isRecalling
    if (!canRecall) return null
    return messages.lastOrNull { it.direction == ChatMessage.DIRECTION_OUT && !it.recalled }
}

internal fun resolveRoomTitle(room: MailboxRoom?): String {
    if (room == null) return ""
    return room.nickname?.takeIf { it.isNotBlank() }
        ?: room.title?.takeIf { it.isNotBlank() }
        ?: "···${room.id.takeLast(ROOM_ID_DISPLAY_SUFFIX)}"
}

internal fun resolveAvatarLetter(title: String): String =
    title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

internal fun formatMessageTime(epochMs: Long): String {
    val format = DateFormat.getTimeInstance(DateFormat.SHORT)
    return format.format(Date(epochMs))
}

/**
 * Local safety-numbers lite: SHA-256 of room key, grouped for compare-by-eye.
 */
internal fun roomKeyFingerprint(roomKey: String): String {
    if (roomKey.isBlank()) return "—"
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(roomKey.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString("") { b -> "%02X".format(b) }
        .take(RoomUiDimens.fingerprintHexLen)
    return hex.chunked(RoomUiDimens.fingerprintGroupSize).joinToString(" ")
}

/**
 * 0f = fully remaining / no clock · 1f = expired.
 * Aura engages when remaining fraction ≤ [RoomUiDimens.auraThresholdFraction].
 */
internal fun roomExpiryProgress(
    expiresAt: Long?,
    activatedAt: Long?,
    nowMs: Long,
): Float {
    if (expiresAt == null || expiresAt <= 0L) return 0f
    val start = activatedAt?.takeIf { it > 0L } ?: (expiresAt - ROOM_TTL_DEFAULT_MS)
    val total = (expiresAt - start).coerceAtLeast(1L)
    val remaining = (expiresAt - nowMs).coerceAtLeast(0L)
    return (1f - remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

internal fun shouldShowBubbleAura(expiryProgress: Float): Boolean =
    expiryProgress >= (1f - RoomUiDimens.auraThresholdFraction)
