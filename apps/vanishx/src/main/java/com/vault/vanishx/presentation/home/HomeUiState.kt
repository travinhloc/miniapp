package com.vault.vanishx.presentation.home

import com.vault.vanishx.domain.model.MailboxRoom

data class HomeRoomItem(
    val id: String,
    val displayName: String,
    val remainingMs: Long,
    val isExpired: Boolean,
    val role: String,
)

data class HomeUiState(
    val anonymousId: String? = null,
    val isBootstrappingIdentity: Boolean = true,
    val isMailboxSyncing: Boolean = false,
    val recentRooms: List<HomeRoomItem> = emptyList(),
    val hasMoreRooms: Boolean = false,
    val inviteDraft: String = "",
    val shareHintUri: String? = null,
    val showProStubToggle: Boolean = false,
    val isProStub: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface HomeAction {
    data object CreateRoom : HomeAction
    data object JoinRoom : HomeAction
    data object ScanQr : HomeAction
    data object Resume : HomeAction
    data object OpenSettings : HomeAction
    data object OpenHistory : HomeAction
    data object ToggleProStub : HomeAction
    data class InviteDraftChanged(val value: String) : HomeAction
    data object JoinFromDraft : HomeAction
    data class OpenRoom(val roomId: String) : HomeAction
    data object DismissShareHint : HomeAction
}

fun MailboxRoom.toHomeItem(nowMs: Long = System.currentTimeMillis()): HomeRoomItem {
    val resolved = resolvedStatus(nowMs)
    val expired = resolved == MailboxRoom.STATUS_EXPIRED
    return HomeRoomItem(
        id = id,
        displayName = nickname?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: "···${id.takeLast(ROOM_ID_SUFFIX_LEN)}",
        remainingMs = if (expiresAt > 0L) (expiresAt - nowMs).coerceAtLeast(0L) else 0L,
        isExpired = expired,
        role = role,
    )
}

private const val ROOM_ID_SUFFIX_LEN = 6
