@file:Suppress("ComplexMethod", "ComplexCondition")

package com.vault.vanishx.presentation.home

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.ConversationPreviewResolver
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.conversation.ConversationRowModel

enum class HomeListFilter {
    All,
    Open,
    Expired,
    Favorite,
}

data class HomeUiState(
    val anonymousId: String? = null,
    val isBootstrappingIdentity: Boolean = true,
    val isMailboxSyncing: Boolean = false,
    val rooms: List<ConversationRowModel> = emptyList(),
    val visibleRooms: List<ConversationRowModel> = emptyList(),
    val searchQuery: String = "",
    val listFilter: HomeListFilter = HomeListFilter.Open,
    val inviteDraft: String = "",
    val shareHintUri: String? = null,
    val waitingInviteUri: String? = null,
    val pendingInviteUri: String? = null,
    val showProStubToggle: Boolean = false,
    val isProStub: Boolean = false,
    val inviteDraftEmpty: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface HomeAction {
    data object CreateRoom : HomeAction
    data object JoinRoom : HomeAction
    data object ScanQr : HomeAction
    data class PasteInvite(val text: String) : HomeAction
    data object Resume : HomeAction
    data object OpenSettings : HomeAction
    data object OpenHistory : HomeAction
    data object ToggleProStub : HomeAction
    data class InviteDraftChanged(val value: String) : HomeAction
    data object JoinFromDraft : HomeAction
    data class OpenRoom(val roomId: String) : HomeAction
    data class DeleteRoom(val roomId: String) : HomeAction
    data object DismissShareHint : HomeAction
    data class SearchQueryChanged(val value: String) : HomeAction
    data class SetFilter(val filter: HomeListFilter) : HomeAction
    data class ShareWaiting(val roomId: String) : HomeAction
    data object DismissWaitingInvite : HomeAction
    data object OpenPendingInvite : HomeAction
    data object DismissPendingInvite : HomeAction
}

internal fun MailboxRoom.toConversationRow(
    lastMessage: ChatMessage?,
    messages: List<ChatMessage>,
    isPro: Boolean,
    nowMs: Long = System.currentTimeMillis(),
): ConversationRowModel {
    val home = toHomeItem(nowMs)
    val left = status == MailboxRoom.STATUS_LEFT
    val preview = ConversationPreviewResolver.resolve(
        waiting = home.isWaiting,
        expired = home.isExpired,
        isPro = isPro,
        left = left,
        lastMessage = lastMessage,
        nowMs = nowMs,
    )
    val unread = if (home.isWaiting || home.isExpired || left) {
        0
    } else {
        ConversationPreviewResolver.unreadInboundCount(messages, lastReadMessageId, nowMs)
    }
    val activityAt = preview.lastActivityAt.takeIf { it > 0L } ?: createdAt
    return ConversationRowModel(
        id = id,
        displayName = home.displayName,
        initials = home.initials,
        avatarLocalPath = avatarLocalPath,
        preview = if (preview.lastActivityAt > 0L) preview else preview.copy(lastActivityAt = createdAt),
        unreadCount = unread,
        isFavorite = favorite,
        isMuted = muted,
        isWaiting = home.isWaiting,
        isExpired = home.isExpired,
        isLeft = left,
        hasRoomClock = home.hasRoomClock,
        ttlFraction = home.ttlFraction,
        remainingMs = home.remainingMs,
        activityAt = activityAt,
    )
}

/** Home lists live chats only — waiting handshake rooms belong on History. */
internal fun MailboxRoom.isHomeListEligible(nowMs: Long = System.currentTimeMillis()): Boolean {
    if (status == MailboxRoom.STATUS_LEFT) return false
    return !toHomeItem(nowMs).isWaiting
}

internal fun List<ConversationRowModel>.sortedForHome(): List<ConversationRowModel> =
    sortedWith(
        compareBy<ConversationRowModel> { !it.isFavorite }
            .thenByDescending { it.activityAt },
    )


fun MailboxRoom.toHomeItem(nowMs: Long = System.currentTimeMillis()): HomeRoomItem {
    val resolved = resolvedStatus(nowMs)
    val expired = resolved == MailboxRoom.STATUS_EXPIRED
    val clock = hasRoomClock()
    val remaining = if (clock) (expiresAt - nowMs).coerceAtLeast(0L) else 0L
    val lifetime = when {
        !clock -> 1f
        activatedAt > 0L && expiresAt > activatedAt -> (expiresAt - activatedAt).toFloat()
        expiresAt > createdAt && createdAt > 0L -> (expiresAt - createdAt).toFloat()
        expiresAt > nowMs -> (expiresAt - nowMs + remaining).toFloat().coerceAtLeast(1f)
        else -> 1f
    }
    val fraction = when {
        !clock -> 1f
        lifetime > 0f -> (remaining / lifetime).coerceIn(0f, 1f)
        else -> 0f
    }
    val label = nickname?.takeIf { it.isNotBlank() }
        ?: title?.takeIf { it.isNotBlank() }
        ?: "···${id.takeLast(ROOM_ID_SUFFIX_LEN)}"
    val waiting = role == MailboxRoom.ROLE_CREATOR &&
        isAwaitingGuest() &&
        !expired
    return HomeRoomItem(
        id = id,
        displayName = label,
        remainingMs = remaining,
        isExpired = expired,
        role = role,
        isWaiting = waiting,
        ttlFraction = fraction,
        initials = initialsFrom(label),
        hasRoomClock = clock,
        isFavorite = favorite,
        isMuted = muted,
    )
}

/** TTL/title helper used by History mapping. */
data class HomeRoomItem(
    val id: String,
    val displayName: String,
    val remainingMs: Long,
    val isExpired: Boolean,
    val role: String,
    val isWaiting: Boolean = false,
    val ttlFraction: Float = 1f,
    val initials: String = "?",
    val hasRoomClock: Boolean = false,
    val isFavorite: Boolean = false,
    val isMuted: Boolean = false,
)

private fun initialsFrom(label: String): String {
    val cleaned = label.trim().removePrefix("#")
    if (cleaned.isEmpty()) return "?"
    val parts = cleaned.split(Regex("\\s+|·")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        cleaned.length >= 2 -> cleaned.take(2).uppercase()
        else -> cleaned.take(1).uppercase()
    }
}

private const val ROOM_ID_SUFFIX_LEN = 6
