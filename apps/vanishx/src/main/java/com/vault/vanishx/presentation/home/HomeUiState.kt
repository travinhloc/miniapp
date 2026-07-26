package com.vault.vanishx.presentation.home

data class HomeUiState(
    val activeRoomCount: Int = 0,
    val anonymousId: String? = null,
    val isBootstrappingIdentity: Boolean = true,
    val isMailboxSyncing: Boolean = false,
    val showProStubToggle: Boolean = false,
    val isProStub: Boolean = false,
)

sealed interface HomeAction {
    data object CreateRoom : HomeAction
    data object JoinRoom : HomeAction
    data object Resume : HomeAction
    data object OpenSecurity : HomeAction
    data object ToggleProStub : HomeAction
}
