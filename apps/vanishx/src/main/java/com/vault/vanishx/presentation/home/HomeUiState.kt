package com.vault.vanishx.presentation.home

data class HomeUiState(
    val showPlaceholder: Boolean = false,
    val activeRoomCount: Int = 0,
    val anonymousId: String? = null,
    val isBootstrappingIdentity: Boolean = true,
)

sealed interface HomeAction {
    data object CreateRoom : HomeAction
    data object JoinRoom : HomeAction
    data object ClearPlaceholder : HomeAction
}
