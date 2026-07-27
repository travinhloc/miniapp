package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.usecase.JoinRoomUseCase
import com.vault.vanishx.presentation.util.formatRemainingMs
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

data class JoinInvitePreview(
    val rawInvite: String,
    val roomTitle: String,
    val roomIdLabel: String,
    val remainingLabel: String?,
)

data class JoinRoomUiState(
    val input: String = "",
    val nickname: String = "",
    val preview: JoinInvitePreview? = null,
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface JoinRoomAction {
    data class InputChanged(val value: String) : JoinRoomAction
    data class NicknameChanged(val value: String) : JoinRoomAction
    data class Scanned(val value: String) : JoinRoomAction
    data object RequestPreview : JoinRoomAction
    data object EnterRoom : JoinRoomAction
    data object SaveForLater : JoinRoomAction
    data object DismissPreview : JoinRoomAction
    data object Back : JoinRoomAction
    data object ClearError : JoinRoomAction
}

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val joinRoom: JoinRoomUseCase,
    private val pendingInviteStore: PendingInviteStore,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()

    init {
        val roomId = savedStateHandle.get<String>("roomId")
        val roomKey = savedStateHandle.get<String>("roomKey")
        val expiresAt = savedStateHandle.get<String>("expiresAt")?.toLongOrNull()
        val deepLinkInvite = if (!roomId.isNullOrBlank() && !roomKey.isNullOrBlank()) {
            RoomInvite(
                roomId = roomId,
                roomKey = roomKey,
                expiresAt = expiresAt?.takeIf { it > 0L },
            ).toUriString()
        } else {
            null
        }
        val pending = pendingInviteStore.peek()
        val prefill = deepLinkInvite ?: pending
        if (!prefill.isNullOrBlank()) {
            _uiState.update { it.copy(input = prefill) }
            showPreview(prefill)
        }
    }

    @Suppress("ComplexMethod")
    fun onAction(action: JoinRoomAction) {
        when (action) {
            is JoinRoomAction.InputChanged -> _uiState.update {
                it.copy(input = action.value, preview = null, errorMessage = null)
            }
            is JoinRoomAction.NicknameChanged -> _uiState.update {
                it.copy(nickname = action.value.take(NICKNAME_MAX), errorMessage = null)
            }
            is JoinRoomAction.Scanned -> {
                _uiState.update { it.copy(input = action.value, preview = null, errorMessage = null) }
                showPreview(action.value)
            }
            JoinRoomAction.RequestPreview -> showPreview(_uiState.value.input)
            JoinRoomAction.EnterRoom -> join(navigateToRoom = true)
            JoinRoomAction.SaveForLater -> join(navigateToRoom = false)
            JoinRoomAction.DismissPreview -> _uiState.update { it.copy(preview = null) }
            JoinRoomAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            JoinRoomAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
        }
    }

    private fun showPreview(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Invite is empty") }
            return
        }
        val invite = InviteUriCodec.parse(trimmed)
        if (invite == null) {
            _uiState.update { it.copy(errorMessage = "Invalid invite link or code", preview = null) }
            return
        }
        _uiState.update {
            it.copy(
                preview = invite.toPreview(trimmed),
                errorMessage = null,
            )
        }
    }

    private fun join(navigateToRoom: Boolean) {
        val raw = resolveJoinRaw() ?: return
        val nickname = _uiState.value.nickname.takeIf { it.isNotBlank() }
        _uiState.update { it.copy(isJoining = true, errorMessage = null) }
        flow { emit(joinRoom(raw, nickname)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                pendingInviteStore.clear()
                _uiState.update { it.copy(isJoining = false, preview = null) }
                if (navigateToRoom) {
                    _navigator.emit(MailboxDestination.Room(room.id))
                } else {
                    _navigator.emit(BaseDestination.Up())
                }
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

    private fun resolveJoinRaw(): String? {
        if (_uiState.value.isJoining) return null
        val preview = _uiState.value.preview
        val raw = preview?.rawInvite ?: _uiState.value.input.trim()
        val invalidEmpty = raw.isEmpty()
        if (invalidEmpty) {
            _uiState.update { it.copy(errorMessage = "Invite is empty") }
        }
        val needsPreview = !invalidEmpty && preview == null
        if (needsPreview) {
            showPreview(raw)
        }
        val ready = !invalidEmpty && (preview != null || _uiState.value.preview != null)
        return raw.takeIf { ready }
    }

    private companion object {
        const val NICKNAME_MAX = 24
    }
}

private const val PREVIEW_TITLE_SUFFIX = 6
private const val PREVIEW_ID_SUFFIX = 4

private fun RoomInvite.toPreview(rawInvite: String): JoinInvitePreview {
    val now = System.currentTimeMillis()
    val remaining = expiresAt?.let { exp ->
        if (exp > now) formatRemainingMs(exp - now) else null
    }
    return JoinInvitePreview(
        rawInvite = rawInvite,
        roomTitle = "···${roomId.takeLast(PREVIEW_TITLE_SUFFIX)}",
        roomIdLabel = "···${roomId.takeLast(PREVIEW_ID_SUFFIX).uppercase()}",
        remainingLabel = remaining,
    )
}
