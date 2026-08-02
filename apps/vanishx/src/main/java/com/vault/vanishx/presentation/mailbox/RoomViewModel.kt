package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.BlockPeerUseCase
import com.vault.vanishx.domain.usecase.GetRoomUseCase
import com.vault.vanishx.domain.usecase.PingPeerUseCase
import com.vault.vanishx.domain.usecase.PingRoomUseCase
import com.vault.vanishx.domain.usecase.PurgeExpiredRoomUseCase
import com.vault.vanishx.domain.usecase.RecallRoomMessageUseCase
import com.vault.vanishx.domain.usecase.ReportRoomUseCase
import com.vault.vanishx.domain.usecase.SendRoomMessageUseCase
import com.vault.vanishx.domain.usecase.SyncRoomMailboxUseCase
import com.vault.vanishx.presentation.paywall.PaywallDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RoomUiState(
    val roomId: String,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isSending: Boolean = false,
    val isPurging: Boolean = false,
    val isBlocking: Boolean = false,
    val isReporting: Boolean = false,
    val isRecalling: Boolean = false,
    val isPro: Boolean = false,
    val room: MailboxRoom? = null,
    val isExpired: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val showBlockConfirm: Boolean = false,
    val showReportDialog: Boolean = false,
    val reportReason: String = "",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showPingConfirm: Boolean = false,
    val pingBusy: Boolean = false,
    val remoteMetaPresent: Boolean? = null,
    val showInviteSheet: Boolean = false,
    val pingPeerEvent: PingPeerEvent? = null,
)

/** One-shot handshake-nudge feedback (story 7.7) — consumed by the screen as a toast. */
sealed interface PingPeerEvent {
    data object Sent : PingPeerEvent
    data class Cooldown(val secondsRemaining: Int) : PingPeerEvent
}

sealed interface RoomAction {
    data object Back : RoomAction
    data object Refresh : RoomAction
    data object Send : RoomAction
    data class DraftChanged(val value: String) : RoomAction
    data object ClearFeedback : RoomAction
    data object OpenBlockConfirm : RoomAction
    data object DismissBlockConfirm : RoomAction
    data object ConfirmBlock : RoomAction
    data object OpenReport : RoomAction
    data object DismissReport : RoomAction
    data class ReportReasonChanged(val value: String) : RoomAction
    data object SubmitReport : RoomAction
    data class RecallMessage(val messageId: String) : RoomAction
    data object OpenPaywall : RoomAction
    data object PingRoom : RoomAction
    data object DismissPing : RoomAction
    data object OpenInviteSheet : RoomAction
    data object DismissInviteSheet : RoomAction
    data object PingPeer : RoomAction
    data object ConsumePingPeerEvent : RoomAction
}

@HiltViewModel
@Suppress("LargeClass", "TooManyFunctions", "ComplexMethod", "ComplexCondition")
class RoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoom: GetRoomUseCase,
    private val sendRoomMessage: SendRoomMessageUseCase,
    private val syncRoomMailbox: SyncRoomMailboxUseCase,
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase,
    private val pingRoom: PingRoomUseCase,
    private val pingPeerUseCase: PingPeerUseCase,
    private val blockPeer: BlockPeerUseCase,
    private val reportRoom: ReportRoomUseCase,
    private val recallRoomMessage: RecallRoomMessageUseCase,
    private val mailboxRepository: MailboxRepository,
    private val proEntitlement: ProEntitlementRepository,
    private val remote: MailboxRemoteDataSource,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val openInviteOnLoad: Boolean = savedStateHandle["openInvite"] ?: false

    private val _uiState = MutableStateFlow(
        RoomUiState(
            roomId = roomId,
            isPro = proEntitlement.isProNow(),
        ),
    )
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var expiryJob: Job? = null
    private var lastPeerPingAtMs = 0L

    init {
        proEntitlement.isPro
            .onEach { pro -> _uiState.update { it.copy(isPro = pro) } }
            .launchIn(viewModelScope)
        bootstrap()
    }

    override fun onCleared() {
        observeJob?.cancel()
        expiryJob?.cancel()
        super.onCleared()
    }

    fun onAction(action: RoomAction) {
        when (action) {
            RoomAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
            RoomAction.Refresh -> sync(showLoading = true)
            RoomAction.Send -> send()
            is RoomAction.DraftChanged -> _uiState.update {
                it.copy(draft = action.value, errorMessage = null)
            }
            RoomAction.ClearFeedback -> _uiState.update {
                it.copy(errorMessage = null, infoMessage = null)
            }
            else -> onUgcAction(action)
        }
    }

    private fun onUgcAction(action: RoomAction) {
        when (action) {
            RoomAction.OpenBlockConfirm -> _uiState.update {
                it.copy(showBlockConfirm = true, errorMessage = null)
            }
            RoomAction.DismissBlockConfirm -> _uiState.update {
                it.copy(showBlockConfirm = false)
            }
            RoomAction.ConfirmBlock -> confirmBlock()
            RoomAction.OpenReport -> _uiState.update {
                it.copy(showReportDialog = true, reportReason = "", errorMessage = null)
            }
            RoomAction.DismissReport -> _uiState.update {
                it.copy(showReportDialog = false, reportReason = "")
            }
            is RoomAction.ReportReasonChanged -> _uiState.update {
                it.copy(reportReason = action.value)
            }
            RoomAction.SubmitReport -> submitReport()
            is RoomAction.RecallMessage -> recall(action.messageId)
            RoomAction.OpenPaywall -> launch { _navigator.emit(PaywallDestination.Paywall) }
            RoomAction.PingRoom -> confirmPing()
            RoomAction.DismissPing -> _uiState.update {
                it.copy(showPingConfirm = false, remoteMetaPresent = null)
            }
            RoomAction.OpenInviteSheet -> _uiState.update { it.copy(showInviteSheet = true) }
            RoomAction.DismissInviteSheet -> _uiState.update { it.copy(showInviteSheet = false) }
            RoomAction.PingPeer -> pingPeer()
            RoomAction.ConsumePingPeerEvent -> _uiState.update { it.copy(pingPeerEvent = null) }
            else -> Unit
        }
    }

    private fun pingPeer() {
        val state = _uiState.value
        if (handshakeStatus(state.room, state.isExpired) != RoomHandshakeStatus.WAITING) return
        val result = pingPeerUseCase(lastPeerPingAtMs)
        if (result.sent) {
            lastPeerPingAtMs = System.currentTimeMillis()
            _uiState.update { it.copy(pingPeerEvent = PingPeerEvent.Sent) }
        } else {
            val seconds = ((result.cooldownRemainingMs + MS_PER_SEC - 1) / MS_PER_SEC).toInt().coerceAtLeast(1)
            _uiState.update { it.copy(pingPeerEvent = PingPeerEvent.Cooldown(seconds)) }
        }
    }

    private fun confirmPing() {
        val state = _uiState.value
        if (!state.isExpired || !state.isPro || state.pingBusy) return
        if (!state.showPingConfirm) {
            _uiState.update { it.copy(showPingConfirm = true, errorMessage = null) }
            return
        }
        _uiState.update { it.copy(pingBusy = true) }
        flow { emit(pingRoom(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { result ->
                _uiState.update {
                    it.copy(
                        pingBusy = false,
                        showPingConfirm = false,
                        remoteMetaPresent = result.remoteMetaPresent,
                        infoMessage = if (result.remoteMetaPresent) {
                            "Remote room meta still present — ping noted"
                        } else {
                            "Remote meta gone — cannot ping"
                        },
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(pingBusy = false, errorMessage = friendlyError(e))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun recall(messageId: String) {
        if (!_uiState.value.isPro || _uiState.value.isRecalling || _uiState.value.isExpired) return
        _uiState.update { it.copy(isRecalling = true, errorMessage = null) }
        flow { emit(recallRoomMessage(roomId, messageId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { result ->
                _uiState.update { state ->
                    state.copy(
                        isRecalling = false,
                        messages = state.messages.map {
                            if (it.id == result.message.id) result.message else it
                        },
                        infoMessage = if (result.remoteRemoved) {
                            "Message recalled from mailbox (best-effort if already downloaded)."
                        } else {
                            "Recalled locally; remote mailbox may need retry when online."
                        },
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Recall failed")
                _uiState.update {
                    it.copy(
                        isRecalling = false,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun bootstrap() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        flow { emit(getRoom(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                val expired = room?.status == MailboxRoom.STATUS_EXPIRED ||
                    room?.status == MailboxRoom.STATUS_LEFT
                val openInvite = openInviteOnLoad &&
                    room?.role == MailboxRoom.ROLE_CREATOR &&
                    !expired
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        room = room,
                        isExpired = expired,
                        errorMessage = if (room == null) "Room not found" else null,
                        showInviteSheet = it.showInviteSheet || openInvite,
                    )
                }
                when {
                    room == null -> Unit
                    room.status == MailboxRoom.STATUS_LEFT -> Unit
                    expired -> purgeAndShowExpired()
                    else -> {
                        scheduleExpiry(room)
                        sync(showLoading = false)
                        startObserving()
                    }
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun scheduleExpiry(room: MailboxRoom) {
        expiryJob?.cancel()
        val expiresAt = room.expiresAt
        if (expiresAt <= 0L) return
        val delayMs = expiresAt - System.currentTimeMillis()
        if (delayMs <= 0L) {
            onLiveExpiry()
            return
        }
        expiryJob = viewModelScope.launch {
            delay(delayMs)
            onLiveExpiry()
        }
    }

    private fun onLiveExpiry() {
        if (_uiState.value.isExpired) return
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                isExpired = true,
                room = it.room?.copy(status = MailboxRoom.STATUS_EXPIRED),
                draft = "",
            )
        }
        purgeAndShowExpired()
    }

    private fun purgeAndShowExpired() {
        val keepLocal = _uiState.value.isPro
        if (!keepLocal) {
            _uiState.update { it.copy(isPurging = true, messages = emptyList()) }
        } else {
            _uiState.update { it.copy(isPurging = true) }
        }
        flow {
            val result = purgeExpiredRoom(roomId)
            val local = if (_uiState.value.isPro) {
                mailboxRepository.getMessages(roomId)
            } else {
                emptyList()
            }
            emit(result to local)
        }
            .flowOn(dispatchersProvider.io)
            .onEach { (result, local) ->
                _uiState.update {
                    it.copy(
                        isPurging = false,
                        isExpired = true,
                        messages = if (it.isPro) local else emptyList(),
                        room = it.room?.copy(status = MailboxRoom.STATUS_EXPIRED),
                        infoMessage = if (!result.remotePurged) {
                            "Remote purge may need retry when online"
                        } else {
                            null
                        },
                    )
                }
            }
            .catch { e ->
                Timber.w(e, "Purge expired room failed")
                _uiState.update {
                    it.copy(
                        isPurging = false,
                        isExpired = true,
                        messages = if (it.isPro) it.messages else emptyList(),
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun sync(showLoading: Boolean) {
        if (_uiState.value.isExpired) return
        if (showLoading) {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
        }
        flow { emit(syncRoomMailbox(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { result ->
                val roomNow = getRoom(roomId)
                val expired = roomNow?.status == MailboxRoom.STATUS_EXPIRED ||
                    roomNow?.status == MailboxRoom.STATUS_LEFT
                if (expired) {
                    observeJob?.cancel()
                    expiryJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            isExpired = true,
                            room = roomNow,
                            messages = emptyList(),
                        )
                    }
                    return@onEach
                }
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        messages = result.messages,
                        room = roomNow ?: it.room,
                        infoMessage = if (result.decryptFailures > 0) {
                            "Some messages could not be decrypted"
                        } else {
                            null
                        },
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Mailbox sync failed")
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = remote.observeMessages(roomId)
            .onEach { remoteList ->
                if (_uiState.value.isExpired) return@onEach
                val result = syncRoomMailbox.ingestRemoteList(roomId, remoteList)
                val roomNow = getRoom(roomId)
                if (roomNow?.status == MailboxRoom.STATUS_EXPIRED ||
                    roomNow?.status == MailboxRoom.STATUS_LEFT
                ) {
                    observeJob?.cancel()
                    expiryJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isExpired = true,
                            room = roomNow,
                            messages = emptyList(),
                        )
                    }
                    return@onEach
                }
                _uiState.update {
                    it.copy(
                        messages = result.messages,
                        room = roomNow ?: it.room,
                        infoMessage = if (result.decryptFailures > 0) {
                            "Some messages could not be decrypted"
                        } else {
                            it.infoMessage
                        },
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Mailbox observe failed")
                _uiState.update { it.copy(errorMessage = friendlyError(e)) }
            }
            .flowOn(dispatchersProvider.io)
            .launchIn(viewModelScope)
    }

    private fun send() {
        if (_uiState.value.isExpired || _uiState.value.isSending) return
        val draft = _uiState.value.draft
        if (draft.isBlank()) return
        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        flow { emit(sendRoomMessage(roomId, draft)) }
            .flowOn(dispatchersProvider.io)
            .onEach { sent ->
                _uiState.update { state ->
                    state.copy(
                        isSending = false,
                        draft = "",
                        messages = (state.messages + sent).distinctBy { it.id }.sortedBy { it.sentAt },
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Send failed")
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun confirmBlock() {
        if (_uiState.value.isBlocking) return
        _uiState.update {
            it.copy(isBlocking = true, showBlockConfirm = false, errorMessage = null)
        }
        flow { emit(blockPeer(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach {
                observeJob?.cancel()
                expiryJob?.cancel()
                _uiState.update {
                    it.copy(
                        isBlocking = false,
                        isExpired = true,
                        messages = emptyList(),
                        draft = "",
                        room = it.room?.copy(status = MailboxRoom.STATUS_LEFT),
                        infoMessage = "Peer blocked. You left this room.",
                    )
                }
                _navigator.emit(BaseDestination.Up())
            }
            .catch { e ->
                Timber.e(e, "Block peer failed")
                _uiState.update {
                    it.copy(
                        isBlocking = false,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun submitReport() {
        if (_uiState.value.isReporting) return
        val reason = _uiState.value.reportReason
        _uiState.update {
            it.copy(isReporting = true, showReportDialog = false, errorMessage = null)
        }
        flow { emit(reportRoom(roomId, reason)) }
            .flowOn(dispatchersProvider.io)
            .onEach {
                _uiState.update {
                    it.copy(
                        isReporting = false,
                        reportReason = "",
                        infoMessage = "Report submitted.",
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Report failed")
                _uiState.update {
                    it.copy(
                        isReporting = false,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty()
        return mapFriendlyError(message) ?: message.ifBlank { error::class.java.simpleName }
    }

    private fun mapFriendlyError(message: String): String? =
        mapUgcError(message) ?: mapMailboxError(message)

    private fun mapUgcError(message: String): String? = when {
        message.contains("Peer identity not known", ignoreCase = true) ->
            "Peer not known yet. Wait until you receive a message, then try Block again."
        message.contains("Peer is blocked", ignoreCase = true) -> "This peer is blocked"
        message.contains("Reason too long", ignoreCase = true) -> "Report reason is too long"
        message.contains("Pro required", ignoreCase = true) ->
            "Recall is a Pro feature (enable Pro stub on Home in staging debug)."
        message.contains("Only your own", ignoreCase = true) -> "You can only recall your own messages"
        else -> null
    }

    private fun mapMailboxError(message: String): String? = when {
        message.contains("expired", ignoreCase = true) -> "Room expired"
        message.contains("Permission denied", ignoreCase = true) ->
            "Could not reach mailbox (permission). Check connection and try again."
        isNetworkError(message) -> "Network error. Check connection and retry."
        message.contains("decrypt", ignoreCase = true) ->
            "Could not decrypt a message. The room key may be wrong."
        message.contains("empty", ignoreCase = true) -> "Message is empty"
        message.contains("not found", ignoreCase = true) -> "Room not found"
        else -> null
    }

    private fun isNetworkError(message: String): Boolean =
        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("network", ignoreCase = true) ||
            message.contains("Firebase Network", ignoreCase = true)

    private companion object {
        const val MS_PER_SEC = 1_000L
    }
}
