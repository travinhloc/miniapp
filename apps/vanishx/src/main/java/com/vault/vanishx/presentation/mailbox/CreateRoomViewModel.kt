package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.usecase.CreateRoomUseCase
import com.vault.vanishx.presentation.qr.QrBitmapEncoder
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
    val selectedTtl: RoomTtlOption = RoomTtlOption.ONE_HOUR,
    val isCreating: Boolean = false,
    val inviteUri: String? = null,
    val roomId: String? = null,
    val qrBitmap: android.graphics.Bitmap? = null,
    val errorMessage: String? = null,
)

sealed interface CreateRoomAction {
    data class SelectTtl(val ttl: RoomTtlOption) : CreateRoomAction
    data object Create : CreateRoomAction
    data object OpenRoom : CreateRoomAction
    data object ClearError : CreateRoomAction
    data object Back : CreateRoomAction
}

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val createRoom: CreateRoomUseCase,
    private val qrBitmapEncoder: QrBitmapEncoder,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CreateRoomUiState())
    val uiState: StateFlow<CreateRoomUiState> = _uiState.asStateFlow()

    fun onAction(action: CreateRoomAction) {
        when (action) {
            is CreateRoomAction.SelectTtl -> {
                if (_uiState.value.inviteUri == null) {
                    _uiState.update { it.copy(selectedTtl = action.ttl) }
                }
            }
            CreateRoomAction.Create -> create()
            CreateRoomAction.OpenRoom -> {
                val roomId = _uiState.value.roomId ?: return
                launch { _navigator.emit(MailboxDestination.Room(roomId)) }
            }
            CreateRoomAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            CreateRoomAction.Back -> launch { _navigator.emit(com.miniapp.core.mvvm.BaseDestination.Up()) }
        }
    }

    private fun create() {
        if (_uiState.value.isCreating || _uiState.value.inviteUri != null) return
        _uiState.update { it.copy(isCreating = true, errorMessage = null) }
        flow { emit(createRoom(_uiState.value.selectedTtl)) }
            .flowOn(dispatchersProvider.io)
            .onEach { created ->
                val uri = created.invite.toUriString()
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        inviteUri = uri,
                        roomId = created.room.id,
                        qrBitmap = qrBitmapEncoder.encode(uri),
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
}
