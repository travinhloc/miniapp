package com.vault.vanishx.presentation.home

import com.vault.vanishx.domain.model.MailboxRoom

data class HomeRoomItem(
    val id: String,
    val displayName: String,
    val remainingMs: Long,
    val isExpired: Boolean,
    val role: String,
    val isWaiting: Boolean = false,
    /** 0f..1f remaining fraction of room lifetime (1 = full). */
    val ttlFraction: Float = 1f,
    val initials: String = "?",
    /** False for Pro Host rooms or Free rooms not yet activated. */
    val hasRoomClock: Boolean = false,
)

data class HomeUiState(
    val anonymousId: String? = null,
    val isBootstrappingIdentity: Boolean = true,
    val isMailboxSyncing: Boolean = false,
    val recentRooms: List<HomeRoomItem> = emptyList(),
    val totalRoomCount: Int = 0,
    val hasMoreRooms: Boolean = false,
    val inviteDraft: String = "",
    val shareHintUri: String? = null,
    val showProStubToggle: Boolean = false,
    val isProStub: Boolean = false,
    val inviteDraftEmpty: Boolean = false,
    val vaporizedToday: Int = 0,
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
}

@Suppress("ComplexMethod")
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
        activatedAt <= 0L &&
        peerPub.isNullOrBlank() &&
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
    )
}

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
