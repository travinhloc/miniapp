@file:Suppress("TooManyFunctions", "LongMethod", "ReturnCount")

package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemotePresence
import com.vault.vanishx.data.remote.RemoteReaction
import com.vault.vanishx.data.remote.RemoteReadWatermark
import com.vault.vanishx.data.remote.RemoteTyping
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RecallPolicy
import com.vault.vanishx.domain.model.firebaseSafeKey
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.BlockPeerUseCase
import com.vault.vanishx.domain.usecase.DeleteLocalMessageUseCase
import com.vault.vanishx.domain.usecase.GetRoomUseCase
import com.vault.vanishx.domain.usecase.PingPeerUseCase
import com.vault.vanishx.domain.usecase.PingRoomUseCase
import com.vault.vanishx.domain.usecase.PurgeExpiredRoomUseCase
import com.vault.vanishx.domain.usecase.RecallRoomMessageUseCase
import com.vault.vanishx.domain.usecase.RefreshRoomMetaUseCase
import com.vault.vanishx.domain.usecase.RenameRoomUseCase
import com.vault.vanishx.domain.usecase.ReportRoomUseCase
import com.vault.vanishx.domain.usecase.SendRoomMessageUseCase
import com.vault.vanishx.domain.usecase.SyncRoomMailboxUseCase
import com.vault.vanishx.presentation.paywall.PaywallDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    val showSensitiveSendConfirm: Boolean = false,
    val showScreenshotBanner: Boolean = false,
    val showBlockConfirm: Boolean = false,
    val showReportDialog: Boolean = false,
    val reportReason: String = "",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showPingConfirm: Boolean = false,
    val pingBusy: Boolean = false,
    val remoteMetaPresent: Boolean? = null,
    val showInviteSheet: Boolean = false,
    val showBentoSheet: Boolean = false,
    val showSafetySheet: Boolean = false,
    val showBurnConfirm: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameDraft: String = "",
    val actionMessageId: String? = null,
    val deleteConfirmMessageId: String? = null,
    val detailsMessageId: String? = null,
    val replyToMessageId: String? = null,
    /** messageId → emoji → count (local until RTDB sync). */
    val reactionsByMessage: Map<String, Map<String, Int>> = emptyMap(),
    val myReactionByMessage: Map<String, String> = emptyMap(),
    val peerOnline: Boolean? = null,
    val peerTyping: Boolean = false,
    val peerReadWatermarkId: String? = null,
    val myDeviceId: String = "",
    val toastMessage: String? = null,
    val pendingClipboard: String? = null,
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
    data object RequestSensitiveSend : RoomAction
    data object ConfirmSensitiveSend : RoomAction
    data object DismissSensitiveSend : RoomAction
    data object ScreenshotDetected : RoomAction
    data object DismissScreenshotBanner : RoomAction
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
    data object OpenBentoSheet : RoomAction
    data object DismissBentoSheet : RoomAction
    data object OpenSafetySheet : RoomAction
    data object DismissSafetySheet : RoomAction
    data object OpenBurnConfirm : RoomAction
    data object DismissBurnConfirm : RoomAction
    data object ConfirmBurn : RoomAction
    data object StubChangeTtl : RoomAction
    data object OpenRenameDialog : RoomAction
    data object DismissRenameDialog : RoomAction
    data class RenameDraftChanged(val value: String) : RoomAction
    data object ConfirmRename : RoomAction
    data class OpenMessageActions(val messageId: String) : RoomAction
    data object DismissMessageActions : RoomAction
    data class CopyMessage(val messageId: String) : RoomAction
    data class ReplyToMessage(val messageId: String) : RoomAction
    data object ClearReply : RoomAction
    data class ReactToMessage(val messageId: String, val emoji: String) : RoomAction
    data class OpenDeleteForMe(val messageId: String) : RoomAction
    data object DismissDeleteForMe : RoomAction
    data object ConfirmDeleteForMe : RoomAction
    data class OpenMessageDetails(val messageId: String) : RoomAction
    data object DismissMessageDetails : RoomAction
    data object ConsumeToast : RoomAction
    data object ConsumeClipboard : RoomAction
}

@HiltViewModel
@Suppress("LargeClass", "TooManyFunctions", "ComplexMethod", "ComplexCondition")
class RoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoom: GetRoomUseCase,
    private val refreshRoomMeta: RefreshRoomMetaUseCase,
    private val sendRoomMessage: SendRoomMessageUseCase,
    private val syncRoomMailbox: SyncRoomMailboxUseCase,
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase,
    private val pingRoom: PingRoomUseCase,
    private val pingPeerUseCase: PingPeerUseCase,
    private val blockPeer: BlockPeerUseCase,
    private val reportRoom: ReportRoomUseCase,
    private val recallRoomMessage: RecallRoomMessageUseCase,
    private val renameRoom: RenameRoomUseCase,
    private val deleteLocalMessage: DeleteLocalMessageUseCase,
    private val mailboxRepository: MailboxRepository,
    private val identityRepository: IdentityRepository,
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
    private var engagementJob: Job? = null
    private var lastPeerPingAtMs = 0L
    private var presenceDeviceId: String = ""

    init {
        proEntitlement.isPro
            .onEach { pro -> _uiState.update { it.copy(isPro = pro) } }
            .launchIn(viewModelScope)
        bootstrap()
    }

    override fun onCleared() {
        observeJob?.cancel()
        expiryJob?.cancel()
        engagementJob?.cancel()
        val deviceId = presenceDeviceId.ifBlank { _uiState.value.myDeviceId }
        if (deviceId.isNotBlank()) {
            CoroutineScope(dispatchersProvider.io).launch {
                runCatching { remote.setPresence(roomId, deviceId, online = false) }
            }
        }
        super.onCleared()
    }

    fun onAction(action: RoomAction) {
        when (action) {
            RoomAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
            RoomAction.Refresh -> sync(showLoading = true)
            RoomAction.Send -> send(sensitive = false)
            is RoomAction.DraftChanged -> {
                _uiState.update {
                    it.copy(draft = action.value, errorMessage = null)
                }
                publishTyping(action.value)
            }
            RoomAction.RequestSensitiveSend -> {
                if (_uiState.value.draft.isNotBlank() && !_uiState.value.isSending) {
                    _uiState.update { it.copy(showSensitiveSendConfirm = true) }
                }
            }
            RoomAction.ConfirmSensitiveSend -> {
                _uiState.update { it.copy(showSensitiveSendConfirm = false) }
                send(sensitive = true)
            }
            RoomAction.DismissSensitiveSend -> _uiState.update {
                it.copy(showSensitiveSendConfirm = false)
            }
            RoomAction.ScreenshotDetected -> _uiState.update {
                it.copy(showScreenshotBanner = true)
            }
            RoomAction.DismissScreenshotBanner -> _uiState.update {
                it.copy(showScreenshotBanner = false)
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
            RoomAction.OpenBentoSheet -> _uiState.update {
                it.copy(showBentoSheet = true, errorMessage = null)
            }
            RoomAction.DismissBentoSheet -> _uiState.update { it.copy(showBentoSheet = false) }
            RoomAction.OpenSafetySheet -> _uiState.update {
                it.copy(showSafetySheet = true, showBentoSheet = false)
            }
            RoomAction.DismissSafetySheet -> _uiState.update { it.copy(showSafetySheet = false) }
            RoomAction.OpenBurnConfirm -> _uiState.update {
                it.copy(showBurnConfirm = true, showBentoSheet = false, errorMessage = null)
            }
            RoomAction.DismissBurnConfirm -> _uiState.update { it.copy(showBurnConfirm = false) }
            RoomAction.ConfirmBurn -> confirmBurn()
            RoomAction.StubChangeTtl -> _uiState.update {
                it.copy(
                    showBentoSheet = false,
                    infoMessage = "Changing TTL will arrive in a later build.",
                )
            }
            RoomAction.OpenRenameDialog -> _uiState.update { state ->
                state.copy(
                    showRenameDialog = true,
                    renameDraft = state.room?.title.orEmpty().ifBlank {
                        state.room?.nickname.orEmpty()
                    },
                    errorMessage = null,
                )
            }
            RoomAction.DismissRenameDialog -> _uiState.update {
                it.copy(showRenameDialog = false, renameDraft = "")
            }
            is RoomAction.RenameDraftChanged -> _uiState.update {
                it.copy(renameDraft = action.value)
            }
            RoomAction.ConfirmRename -> confirmRename()
            is RoomAction.OpenMessageActions -> _uiState.update {
                it.copy(actionMessageId = action.messageId)
            }
            RoomAction.DismissMessageActions -> _uiState.update {
                it.copy(actionMessageId = null)
            }
            is RoomAction.CopyMessage -> copyMessage(action.messageId)
            is RoomAction.ReplyToMessage -> _uiState.update {
                it.copy(replyToMessageId = action.messageId)
            }
            RoomAction.ClearReply -> _uiState.update { it.copy(replyToMessageId = null) }
            is RoomAction.ReactToMessage -> react(action.messageId, action.emoji)
            is RoomAction.OpenDeleteForMe -> _uiState.update {
                it.copy(deleteConfirmMessageId = action.messageId)
            }
            RoomAction.DismissDeleteForMe -> _uiState.update {
                it.copy(deleteConfirmMessageId = null)
            }
            RoomAction.ConfirmDeleteForMe -> confirmDeleteForMe()
            is RoomAction.OpenMessageDetails -> _uiState.update {
                it.copy(detailsMessageId = action.messageId)
            }
            RoomAction.DismissMessageDetails -> _uiState.update {
                it.copy(detailsMessageId = null)
            }
            RoomAction.ConsumeToast -> _uiState.update { it.copy(toastMessage = null) }
            RoomAction.ConsumeClipboard -> _uiState.update { it.copy(pendingClipboard = null) }
            else -> Unit
        }
    }

    private fun copyMessage(messageId: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (message.recalled || message.body.isBlank()) return
        if (message.sensitive) {
            _uiState.update {
                it.copy(toastMessage = "sensitive_copy_blocked")
            }
            return
        }
        _uiState.update {
            it.copy(
                pendingClipboard = message.body,
                toastMessage = "copied",
            )
        }
    }

    private fun react(messageId: String, emoji: String) {
        val previous = _uiState.value.myReactionByMessage[messageId]
        val cleared = previous == emoji
        _uiState.update { state ->
            val counts = state.reactionsByMessage[messageId].orEmpty().toMutableMap()
            val prior = state.myReactionByMessage[messageId]
            if (prior != null) {
                val next = (counts[prior] ?: 1) - 1
                if (next <= 0) counts.remove(prior) else counts[prior] = next
            }
            val myNext = state.myReactionByMessage.toMutableMap()
            if (prior == emoji) {
                myNext.remove(messageId)
            } else {
                counts[emoji] = (counts[emoji] ?: 0) + 1
                myNext[messageId] = emoji
            }
            state.copy(
                reactionsByMessage = state.reactionsByMessage + (messageId to counts),
                myReactionByMessage = myNext,
            )
        }
        val deviceId = _uiState.value.myDeviceId.ifBlank { presenceDeviceId }
        if (deviceId.isBlank()) return
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                if (cleared) {
                    remote.clearReaction(roomId, messageId, deviceId)
                } else {
                    remote.setReaction(roomId, messageId, deviceId, emoji)
                }
            }.onFailure { Timber.w(it, "Reaction sync failed") }
        }
    }

    private fun confirmDeleteForMe() {
        val messageId = _uiState.value.deleteConfirmMessageId ?: return
        flow { emit(deleteLocalMessage(roomId, messageId)) }
            .flowOn(dispatchersProvider.io)
            .onEach {
                _uiState.update { state ->
                    state.copy(
                        deleteConfirmMessageId = null,
                        messages = state.messages.filterNot { it.id == messageId },
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        deleteConfirmMessageId = null,
                        errorMessage = friendlyError(e),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun confirmRename() {
        val draft = _uiState.value.renameDraft
        flow { emit(renameRoom(roomId, draft)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                _uiState.update {
                    it.copy(
                        room = room,
                        showRenameDialog = false,
                        renameDraft = "",
                        infoMessage = null,
                    )
                }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = friendlyError(e)) }
            }
            .launchIn(viewModelScope)
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
        val state = _uiState.value
        if (state.isRecalling || state.isExpired) return
        val target = state.messages.firstOrNull { it.id == messageId } ?: return
        if (!RecallPolicy.canRecallOutbound(target.sentAt, state.isPro)) {
            launch { _navigator.emit(PaywallDestination.Paywall) }
            return
        }
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
        flow {
            refreshRoomMeta(roomId)
            emit(getRoom(roomId))
        }
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
                        maybeStartEngagement(room)
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
        engagementJob?.cancel()
        setPresenceOffline()
        _uiState.update {
            it.copy(
                isExpired = true,
                room = it.room?.copy(status = MailboxRoom.STATUS_EXPIRED),
                draft = "",
                peerOnline = null,
                peerTyping = false,
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
                    engagementJob?.cancel()
                    setPresenceOffline()
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            isExpired = true,
                            room = roomNow,
                            messages = emptyList(),
                            peerOnline = null,
                            peerTyping = false,
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
                maybeStartEngagement(roomNow ?: _uiState.value.room)
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
                    engagementJob?.cancel()
                    setPresenceOffline()
                    _uiState.update {
                        it.copy(
                            isExpired = true,
                            room = roomNow,
                            messages = emptyList(),
                            peerOnline = null,
                            peerTyping = false,
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
                maybeStartEngagement(roomNow ?: _uiState.value.room)
            }
            .catch { e ->
                Timber.e(e, "Mailbox observe failed")
                _uiState.update { it.copy(errorMessage = friendlyError(e)) }
            }
            .flowOn(dispatchersProvider.io)
            .launchIn(viewModelScope)
    }

    private fun send(sensitive: Boolean) {
        if (_uiState.value.isExpired || _uiState.value.isSending) return
        val draft = _uiState.value.draft
        if (draft.isBlank()) return
        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        flow { emit(sendRoomMessage(roomId, draft, sensitive, _uiState.value.replyToMessageId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { sent ->
                clearTypingRemote()
                _uiState.update { state ->
                    state.copy(
                        isSending = false,
                        draft = "",
                        showSensitiveSendConfirm = false,
                        replyToMessageId = null,
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
                engagementJob?.cancel()
                setPresenceOffline()
                _uiState.update {
                    it.copy(
                        isBlocking = false,
                        isExpired = true,
                        messages = emptyList(),
                        draft = "",
                        room = it.room?.copy(status = MailboxRoom.STATUS_LEFT),
                        peerOnline = null,
                        peerTyping = false,
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

    private fun confirmBurn() {
        if (_uiState.value.isBlocking) return
        val room = _uiState.value.room ?: return
        _uiState.update {
            it.copy(isBlocking = true, showBurnConfirm = false, errorMessage = null)
        }
        flow {
            mailboxRepository.deleteMessagesForRoom(roomId)
            mailboxRepository.upsertRoom(room.copy(status = MailboxRoom.STATUS_LEFT))
            emit(Unit)
        }
            .flowOn(dispatchersProvider.io)
            .onEach {
                observeJob?.cancel()
                expiryJob?.cancel()
                engagementJob?.cancel()
                setPresenceOffline()
                _uiState.update {
                    it.copy(
                        isBlocking = false,
                        isExpired = true,
                        messages = emptyList(),
                        draft = "",
                        room = it.room?.copy(status = MailboxRoom.STATUS_LEFT),
                        peerOnline = null,
                        peerTyping = false,
                        infoMessage = "Room burned on this device.",
                    )
                }
                _navigator.emit(BaseDestination.Up())
            }
            .catch { e ->
                Timber.e(e, "Burn room failed")
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

    private fun maybeStartEngagement(room: MailboxRoom?) {
        if (handshakeStatus(room, _uiState.value.isExpired) != RoomHandshakeStatus.LIVE) return
        startEngagement()
    }

    private fun startEngagement() {
        if (engagementJob?.isActive == true) return
        engagementJob = viewModelScope.launch(dispatchersProvider.io) {
            val identity = runCatching { identityRepository.ensureIdentity() }
                .getOrElse {
                    Timber.w(it, "Engagement identity failed")
                    return@launch
                }
            val deviceId = firebaseSafeKey(identity.publicKeyBase64)
            presenceDeviceId = deviceId
            _uiState.update { it.copy(myDeviceId = deviceId) }
            runCatching { remote.setPresence(roomId, deviceId, online = true) }
                .onFailure { Timber.w(it, "setPresence failed") }

            launch {
                remote.observePresence(roomId)
                    .catch { e -> Timber.w(e, "observePresence failed") }
                    .collect { list -> applyPresence(list, deviceId) }
            }
            launch {
                remote.observeReadWatermarks(roomId)
                    .catch { e -> Timber.w(e, "observeReadWatermarks failed") }
                    .collect { list -> applyReadWatermarks(list, deviceId) }
            }
            launch {
                remote.observeTyping(roomId)
                    .catch { e -> Timber.w(e, "observeTyping failed") }
                    .collect { list -> applyTyping(list, deviceId) }
            }
            launch {
                remote.observeReactions(roomId)
                    .catch { e -> Timber.w(e, "observeReactions failed") }
                    .collect { list -> applyReactions(list, deviceId) }
            }
            launch {
                _uiState
                    .map { state ->
                        val live = handshakeStatus(state.room, state.isExpired) ==
                            RoomHandshakeStatus.LIVE
                        val lastId = state.messages.lastOrNull()?.id
                        live to lastId
                    }
                    .distinctUntilChanged()
                    .collect { (live, lastId) ->
                        if (live && !lastId.isNullOrBlank()) {
                            runCatching {
                                remote.setReadWatermark(roomId, deviceId, lastId)
                            }.onFailure { Timber.w(it, "setReadWatermark failed") }
                        }
                    }
            }
        }
    }

    private fun applyPresence(list: List<RemotePresence>, myDeviceId: String) {
        val now = System.currentTimeMillis()
        val peerOnline = list.any { presence ->
            presence.deviceId != myDeviceId &&
                (presence.online || now - presence.updatedAt <= PRESENCE_GRACE_MS)
        }
        _uiState.update { it.copy(peerOnline = peerOnline) }
    }

    private fun applyReadWatermarks(list: List<RemoteReadWatermark>, myDeviceId: String) {
        val peerMark = list
            .filter { it.deviceId != myDeviceId }
            .maxByOrNull { it.updatedAt }
            ?.messageId
        _uiState.update { it.copy(peerReadWatermarkId = peerMark) }
    }

    private fun applyTyping(list: List<RemoteTyping>, myDeviceId: String) {
        val now = System.currentTimeMillis()
        val peerTyping = list.any { typing ->
            typing.deviceId != myDeviceId && now - typing.at <= TYPING_TTL_MS
        }
        _uiState.update { it.copy(peerTyping = peerTyping) }
    }

    private fun applyReactions(list: List<RemoteReaction>, myDeviceId: String) {
        val counts = mutableMapOf<String, MutableMap<String, Int>>()
        val mine = mutableMapOf<String, String>()
        list.forEach { reaction ->
            val perMessage = counts.getOrPut(reaction.messageId) { mutableMapOf() }
            perMessage[reaction.emoji] = (perMessage[reaction.emoji] ?: 0) + 1
            if (reaction.deviceId == myDeviceId) {
                mine[reaction.messageId] = reaction.emoji
            }
        }
        _uiState.update {
            it.copy(
                reactionsByMessage = counts.mapValues { (_, v) -> v.toMap() },
                myReactionByMessage = mine,
            )
        }
    }

    private fun publishTyping(draft: String) {
        val deviceId = _uiState.value.myDeviceId.ifBlank { presenceDeviceId }
        if (deviceId.isBlank()) return
        if (handshakeStatus(_uiState.value.room, _uiState.value.isExpired) !=
            RoomHandshakeStatus.LIVE
        ) {
            return
        }
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                if (draft.isBlank()) {
                    remote.clearTyping(roomId, deviceId)
                } else {
                    remote.setTyping(roomId, deviceId, System.currentTimeMillis())
                }
            }.onFailure { Timber.w(it, "Typing sync failed") }
        }
    }

    private fun clearTypingRemote() {
        val deviceId = _uiState.value.myDeviceId.ifBlank { presenceDeviceId }
        if (deviceId.isBlank()) return
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { remote.clearTyping(roomId, deviceId) }
                .onFailure { Timber.w(it, "clearTyping failed") }
        }
    }

    private fun setPresenceOffline() {
        val deviceId = presenceDeviceId.ifBlank { _uiState.value.myDeviceId }
        if (deviceId.isBlank()) return
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { remote.setPresence(roomId, deviceId, online = false) }
                .onFailure { Timber.w(it, "setPresence offline failed") }
        }
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
            "Recall after 24h needs Pro (enable Pro stub on Home in staging debug)."
        message.contains("Only your own", ignoreCase = true) -> "You can only recall your own messages"
        message.contains("Room title required", ignoreCase = true) -> "Enter a room name"
        message.contains("Room title too long", ignoreCase = true) -> "Name is too long"
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
        const val PRESENCE_GRACE_MS = 60_000L
        const val TYPING_TTL_MS = 3_000L
    }
}
