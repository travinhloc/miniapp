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

/** Story 7.5: create is one tap, TTL fixed at 24h — no multi-field form. */
enum class CreateRoomMode {
    INSTANT,
    LATER,
}

data class CreateRoomUiState(
    val mode: CreateRoomMode = CreateRoomMode.INSTANT,
    val icebreaker: String = "",
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface CreateRoomAction {
    data class SelectMode(val mode: CreateRoomMode) : CreateRoomAction
    data class IcebreakerChanged(val value: String) : CreateRoomAction
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
            is CreateRoomAction.SelectMode -> _uiState.update { it.copy(mode = action.mode) }
            is CreateRoomAction.IcebreakerChanged -> _uiState.update {
                it.copy(icebreaker = action.value.take(ICEBREAKER_MAX), errorMessage = null)
            }
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
                    ttl = RoomTtlOption.ONE_DAY,
                    icebreaker = state.icebreaker.takeIf { it.isNotBlank() },
                ),
            )
        }
            .flowOn(dispatchersProvider.io)
            .onEach { created ->
                _uiState.update { it.copy(isCreating = false) }
                when (state.mode) {
                    CreateRoomMode.INSTANT -> launch {
                        _navigator.emit(
                            MailboxDestination.Room(roomId = created.room.id, openInvite = true),
                        )
                    }
                    CreateRoomMode.LATER -> launch {
                        _navigator.emit(
                            BaseDestination.Up(
                                results = hashMapOf<String, Any>("inviteUri" to created.invite.toUriString()),
                            ),
                        )
                    }
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
        const val ICEBREAKER_MAX = 80
    }
}
