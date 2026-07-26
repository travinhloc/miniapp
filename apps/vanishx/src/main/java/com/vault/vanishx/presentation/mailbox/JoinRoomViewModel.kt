package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.usecase.JoinRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class JoinRoomUiState(
    val input: String = "",
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface JoinRoomAction {
    data class InputChanged(val value: String) : JoinRoomAction
    data class Scanned(val value: String) : JoinRoomAction
    data object Join : JoinRoomAction
    data object Back : JoinRoomAction
    data object ClearError : JoinRoomAction
}

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val joinRoom: JoinRoomUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()

    init {
        val roomId = savedStateHandle.get<String>("roomId")
        val roomKey = savedStateHandle.get<String>("roomKey")
        val expiresAt = savedStateHandle.get<String>("expiresAt")?.toLongOrNull()
        if (!roomId.isNullOrBlank() && !roomKey.isNullOrBlank()) {
            val invite = RoomInvite(
                roomId = roomId,
                roomKey = roomKey,
                expiresAt = expiresAt?.takeIf { it > 0L },
            )
            _uiState.update { it.copy(input = invite.toUriString()) }
            join(invite.toUriString())
        }
    }

    fun onAction(action: JoinRoomAction) {
        when (action) {
            is JoinRoomAction.InputChanged -> _uiState.update {
                it.copy(input = action.value, errorMessage = null)
            }
            is JoinRoomAction.Scanned -> {
                _uiState.update { it.copy(input = action.value, errorMessage = null) }
                join(action.value)
            }
            JoinRoomAction.Join -> join(_uiState.value.input)
            JoinRoomAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            JoinRoomAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
        }
    }

    private fun join(raw: String) {
        if (_uiState.value.isJoining) return
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Invite is empty") }
            return
        }
        // Validate early for clearer errors
        if (InviteUriCodec.parse(trimmed) == null && !trimmed.contains("://")) {
            // allow bare "roomId?k=key" via codec normalize
        }
        _uiState.update { it.copy(isJoining = true, errorMessage = null) }
        flow { emit(joinRoom(trimmed)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                _uiState.update { it.copy(isJoining = false) }
                _navigator.emit(MailboxDestination.Room(room.id))
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        errorMessage = e.message ?: e::class.java.simpleName,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
