package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.usecase.CreateRoomUseCase
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

data class CreateRoomUiState(
    val nickname: String = "",
    val title: String = "",
    val inviteNote: String = "",
    val selectedTtl: RoomTtlOption = RoomTtlOption.ONE_HOUR,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface CreateRoomAction {
    data class NicknameChanged(val value: String) : CreateRoomAction
    data class TitleChanged(val value: String) : CreateRoomAction
    data class InviteNoteChanged(val value: String) : CreateRoomAction
    data class SelectTtl(val ttl: RoomTtlOption) : CreateRoomAction
    data object Create : CreateRoomAction
    data object ClearError : CreateRoomAction
    data object Back : CreateRoomAction
}

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val createRoom: CreateRoomUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CreateRoomUiState())
    val uiState: StateFlow<CreateRoomUiState> = _uiState.asStateFlow()

    fun onAction(action: CreateRoomAction) {
        when (action) {
            is CreateRoomAction.NicknameChanged -> _uiState.update {
                it.copy(nickname = action.value.take(NICKNAME_MAX), errorMessage = null)
            }
            is CreateRoomAction.TitleChanged -> _uiState.update {
                it.copy(title = action.value.take(TITLE_MAX), errorMessage = null)
            }
            is CreateRoomAction.InviteNoteChanged -> _uiState.update {
                it.copy(inviteNote = action.value.take(INVITE_NOTE_MAX), errorMessage = null)
            }
            is CreateRoomAction.SelectTtl -> _uiState.update { it.copy(selectedTtl = action.ttl) }
            CreateRoomAction.Create -> create()
            CreateRoomAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            CreateRoomAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
        }
    }

    private fun create() {
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(isCreating = true, errorMessage = null) }
        val state = _uiState.value
        flow {
            emit(
                createRoom(
                    ttl = state.selectedTtl,
                    title = state.title.takeIf { it.isNotBlank() },
                    nickname = state.nickname.takeIf { it.isNotBlank() },
                ),
            )
        }
            .flowOn(dispatchersProvider.io)
            .onEach { created ->
                _uiState.update { it.copy(isCreating = false) }
                val uri = created.invite.toUriString()
                launch {
                    _navigator.emit(
                        BaseDestination.Up(
                            results = hashMapOf<String, Any>("inviteUri" to uri),
                        ),
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        errorMessage = e.message ?: e::class.java.simpleName,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private companion object {
        const val NICKNAME_MAX = 24
        const val TITLE_MAX = 32
        const val INVITE_NOTE_MAX = 140
    }
}
