package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.usecase.GetRoomUseCase
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

data class RoomUiState(
    val roomId: String,
    val isLoading: Boolean = true,
    val room: MailboxRoom? = null,
    val isExpired: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface RoomAction {
    data object Back : RoomAction
    data object Refresh : RoomAction
}

@HiltViewModel
class RoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoom: GetRoomUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])

    private val _uiState = MutableStateFlow(RoomUiState(roomId = roomId))
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onAction(action: RoomAction) {
        when (action) {
            RoomAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
            RoomAction.Refresh -> refresh()
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        flow { emit(getRoom(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        room = room,
                        isExpired = room?.status == MailboxRoom.STATUS_EXPIRED,
                        errorMessage = if (room == null) "Room not found" else null,
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: e::class.java.simpleName,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
