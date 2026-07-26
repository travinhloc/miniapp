package com.vault.vanishx.presentation.home

data class HomeUiState(
    val showPlaceholder: Boolean = false,
    val activeRoomCount: Int = 0,
    val anonymousId: String? = null,
    val isBootstrappingIdentity: Boolean = true,
    val isMailboxSyncing: Boolean = false,
    val showMailboxSmoke: Boolean = false,
    val showProStubToggle: Boolean = false,
    val isProStub: Boolean = false,
    val isMailboxSmokeRunning: Boolean = false,
    val mailboxSmokeResult: String? = null,
    val mailboxSmokeError: String? = null,
)

sealed interface HomeAction {
    data object CreateRoom : HomeAction
    data object JoinRoom : HomeAction
    data object ClearPlaceholder : HomeAction
    data object Resume : HomeAction
    data object OpenSecurity : HomeAction
    data object RunMailboxSmoke : HomeAction
    data object ClearMailboxSmokeFeedback : HomeAction
    data object ToggleProStub : HomeAction
}
