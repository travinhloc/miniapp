package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.usecase.JoinRoomUseCase
import com.vault.vanishx.presentation.util.GuestNicknameGenerator
import com.vault.vanishx.presentation.util.formatRemainingMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Message Request preview shown before the guest decides Accept / Later / Block (story 7.6). */
data class JoinInvitePreview(
    val rawInvite: String,
    val roomTitle: String,
    val roomIdLabel: String,
    val remainingLabel: String?,
    /** Host's opener, if any (story 7.5 icebreaker, read-only fetch — no handshake yet). */
    val icebreaker: String? = null,
    /** Host's Ed25519 public key, known once meta is readable — required to Block pre-accept. */
    val creatorPub: String? = null,
)

data class JoinRoomUiState(
    val input: String = "",
    val nickname: String = "",
    val isNicknameEditing: Boolean = false,
    val preview: JoinInvitePreview? = null,
    val isPreviewLoading: Boolean = false,
    val isJoining: Boolean = false,
    val isBlocking: Boolean = false,
    val showBlockConfirm: Boolean = false,
    val errorMessage: String? = null,
)

/** One-shot feedback for actions that don't navigate (story 7.6: Later / Block). */
enum class JoinRoomToast {
    SAVED_FOR_LATER,
    BLOCKED,
}

sealed interface JoinRoomAction {
    data class InputChanged(val value: String) : JoinRoomAction
    data class NicknameChanged(val value: String) : JoinRoomAction
    data object ToggleNicknameEdit : JoinRoomAction
    data class Scanned(val value: String) : JoinRoomAction
    data object RequestPreview : JoinRoomAction
    data object AcceptAndChat : JoinRoomAction
    data object SaveForLater : JoinRoomAction
    data object OpenBlockConfirm : JoinRoomAction
    data object DismissBlockConfirm : JoinRoomAction
    data object ConfirmBlock : JoinRoomAction
    data object DismissPreview : JoinRoomAction
    data object Back : JoinRoomAction
    data object ClearError : JoinRoomAction
}

@HiltViewModel
@Suppress("ComplexMethod", "TooManyFunctions", "LargeClass")
class JoinRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val joinRoom: JoinRoomUseCase,
    private val pendingInviteStore: PendingInviteStore,
    private val remote: MailboxRemoteDataSource,
    private val blockRepository: BlockRepository,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()

    private val _toast = MutableSharedFlow<JoinRoomToast>()
    val toast: SharedFlow<JoinRoomToast> = _toast.asSharedFlow()

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
            JoinRoomAction.ToggleNicknameEdit -> _uiState.update {
                it.copy(isNicknameEditing = !it.isNicknameEditing)
            }
            is JoinRoomAction.Scanned -> {
                _uiState.update { it.copy(input = action.value, preview = null, errorMessage = null) }
                showPreview(action.value)
            }
            JoinRoomAction.RequestPreview -> showPreview(_uiState.value.input)
            JoinRoomAction.AcceptAndChat -> acceptAndChat()
            JoinRoomAction.SaveForLater -> saveForLater()
            JoinRoomAction.OpenBlockConfirm -> _uiState.update {
                it.copy(showBlockConfirm = true, errorMessage = null)
            }
            JoinRoomAction.DismissBlockConfirm -> _uiState.update { it.copy(showBlockConfirm = false) }
            JoinRoomAction.ConfirmBlock -> confirmBlock()
            JoinRoomAction.DismissPreview -> _uiState.update {
                it.copy(preview = null, isNicknameEditing = false)
            }
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
        _uiState.update { it.copy(isPreviewLoading = true, errorMessage = null) }
        flow { emit(runCatching { remote.readRoomMeta(invite.roomId) }.getOrNull()) }
            .flowOn(dispatchersProvider.io)
            .onEach { meta ->
                _uiState.update {
                    it.copy(
                        isPreviewLoading = false,
                        preview = invite.toPreview(trimmed, meta),
                        nickname = it.nickname.ifBlank { GuestNicknameGenerator.generate(invite.roomId) },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun acceptAndChat() {
        if (_uiState.value.isJoining) return
        val raw = _uiState.value.preview?.rawInvite ?: _uiState.value.input.trim()
        if (raw.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Invite is empty") }
            return
        }
        val nickname = _uiState.value.nickname.takeIf { it.isNotBlank() }
        _uiState.update { it.copy(isJoining = true, errorMessage = null) }
        flow { emit(joinRoom(raw, nickname)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                pendingInviteStore.clear()
                _uiState.update { it.copy(isJoining = false, preview = null) }
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

    /**
     * Story 7.6: Later must NOT run the handshake — no local room, no push subscribe, no remote
     * write. It only keeps the invite around so the guest can reopen this same request.
     */
    private fun saveForLater() {
        val raw = _uiState.value.preview?.rawInvite ?: _uiState.value.input.trim()
        if (raw.isEmpty()) return
        pendingInviteStore.save(raw)
        _uiState.update { it.copy(preview = null, isNicknameEditing = false) }
        launch {
            _toast.emit(JoinRoomToast.SAVED_FOR_LATER)
            _navigator.emit(BaseDestination.Up())
        }
    }

    private fun confirmBlock() {
        if (_uiState.value.isBlocking) return
        val creatorPub = _uiState.value.preview?.creatorPub
        _uiState.update { it.copy(showBlockConfirm = false) }
        if (creatorPub.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Peer identity not known yet. Try again once it loads.") }
            return
        }
        _uiState.update { it.copy(isBlocking = true, errorMessage = null) }
        flow {
            blockRepository.block(creatorPub)
            emit(Unit)
        }
            .flowOn(dispatchersProvider.io)
            .onEach {
                pendingInviteStore.clear()
                _uiState.update { it.copy(isBlocking = false, preview = null) }
                _toast.emit(JoinRoomToast.BLOCKED)
                _navigator.emit(BaseDestination.Up())
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isBlocking = false,
                        errorMessage = e.message ?: e::class.java.simpleName,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private companion object {
        const val NICKNAME_MAX = 24
    }
}

private const val PREVIEW_TITLE_SUFFIX = 6
private const val PREVIEW_ID_SUFFIX = 4

private fun RoomInvite.toPreview(rawInvite: String, meta: RemoteRoomMeta?): JoinInvitePreview {
    val now = System.currentTimeMillis()
    val resolvedExpiresAt = expiresAt?.takeIf { it > 0L } ?: meta?.expiresAt?.takeIf { it > 0L }
    val remaining = resolvedExpiresAt?.let { exp -> if (exp > now) formatRemainingMs(exp - now) else null }
    return JoinInvitePreview(
        rawInvite = rawInvite,
        roomTitle = "···${roomId.takeLast(PREVIEW_TITLE_SUFFIX)}",
        roomIdLabel = "···${roomId.takeLast(PREVIEW_ID_SUFFIX).uppercase()}",
        remainingLabel = remaining,
        icebreaker = meta?.icebreaker,
        creatorPub = meta?.creatorPub,
    )
}
