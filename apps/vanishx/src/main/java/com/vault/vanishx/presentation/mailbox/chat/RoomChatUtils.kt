@file:Suppress("TooManyFunctions")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RecallPolicy
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

internal sealed interface RoomTimelineItem {
    data class DaySeparator(val dayStartMs: Long, val kind: DaySeparatorKind) : RoomTimelineItem
    data class Message(val message: ChatMessage) : RoomTimelineItem
}

internal enum class DaySeparatorKind {
    TODAY,
    YESTERDAY,
    DATE,
}

internal fun findRecallableMessage(
    messages: List<ChatMessage>,
    isPro: Boolean,
    isExpired: Boolean,
    isRecalling: Boolean,
    nowMs: Long = System.currentTimeMillis(),
): ChatMessage? {
    if (isExpired || isRecalling) return null
    return messages.lastOrNull { message ->
        message.direction == ChatMessage.DIRECTION_OUT &&
            !message.recalled &&
            RecallPolicy.canRecallOutbound(message.sentAt, isPro, nowMs)
    }
}

internal fun resolveRoomTitle(room: MailboxRoom?): String {
    if (room == null) return ""
    return room.title?.takeIf { it.isNotBlank() }
        ?: room.nickname?.takeIf { it.isNotBlank() }
        ?: "···${room.id.takeLast(ROOM_ID_DISPLAY_SUFFIX)}"
}

internal fun resolveAvatarLetter(title: String): String =
    title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

internal fun formatMessageTime(epochMs: Long): String {
    val format = DateFormat.getTimeInstance(DateFormat.SHORT)
    return format.format(Date(epochMs))
}

internal fun formatDaySeparatorDate(dayStartMs: Long): String {
    val format = DateFormat.getDateInstance(DateFormat.MEDIUM)
    return format.format(Date(dayStartMs))
}

internal fun dayStartMs(epochMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

internal fun daySeparatorKind(dayStartMs: Long, nowMs: Long): DaySeparatorKind {
    val todayStart = dayStartMs(nowMs)
    val yesterdayStart = todayStart - TimeUnit.DAYS.toMillis(1)
    return when (dayStartMs) {
        todayStart -> DaySeparatorKind.TODAY
        yesterdayStart -> DaySeparatorKind.YESTERDAY
        else -> DaySeparatorKind.DATE
    }
}

internal fun buildRoomTimeline(
    messages: List<ChatMessage>,
    nowMs: Long = System.currentTimeMillis(),
): List<RoomTimelineItem> {
    if (messages.isEmpty()) return emptyList()
    val items = ArrayList<RoomTimelineItem>(messages.size * 2)
    var lastDayStart = Long.MIN_VALUE
    for (message in messages) {
        val start = dayStartMs(message.sentAt)
        if (start != lastDayStart) {
            items += RoomTimelineItem.DaySeparator(start, daySeparatorKind(start, nowMs))
            lastDayStart = start
        }
        items += RoomTimelineItem.Message(message)
    }
    return items
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

internal fun isMessageAtOrBeforeWatermark(
    messageId: String,
    watermarkId: String?,
    messages: List<ChatMessage>,
): Boolean {
    if (watermarkId.isNullOrBlank()) return false
    val watermarkIndex = messages.indexOfFirst { it.id == watermarkId }
    val messageIndex = messages.indexOfFirst { it.id == messageId }
    return watermarkIndex >= 0 && messageIndex >= 0 && messageIndex <= watermarkIndex
}
