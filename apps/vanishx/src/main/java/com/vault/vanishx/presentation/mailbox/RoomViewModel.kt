package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.usecase.GetRoomUseCase
import com.vault.vanishx.domain.usecase.PurgeExpiredRoomUseCase
import com.vault.vanishx.domain.usecase.SendRoomMessageUseCase
import com.vault.vanishx.domain.usecase.SyncRoomMailboxUseCase
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
    val room: MailboxRoom? = null,
    val isExpired: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

sealed interface RoomAction {
    data object Back : RoomAction
    data object Refresh : RoomAction
    data object Send : RoomAction
    data class DraftChanged(val value: String) : RoomAction
    data object ClearFeedback : RoomAction
}

@HiltViewModel
@Suppress("LargeClass", "TooManyFunctions")
class RoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoom: GetRoomUseCase,
    private val sendRoomMessage: SendRoomMessageUseCase,
    private val syncRoomMailbox: SyncRoomMailboxUseCase,
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase,
    private val remote: MailboxRemoteDataSource,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])

    private val _uiState = MutableStateFlow(RoomUiState(roomId = roomId))
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var expiryJob: Job? = null

    init {
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
        }
    }

    private fun bootstrap() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        flow { emit(getRoom(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { room ->
                val expired = room?.status == MailboxRoom.STATUS_EXPIRED
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        room = room,
                        isExpired = expired,
                        errorMessage = if (room == null) "Room not found" else null,
                    )
                }
                when {
                    room == null -> Unit
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
        _uiState.update { it.copy(isPurging = true, messages = emptyList()) }
        flow { emit(purgeExpiredRoom(roomId)) }
            .flowOn(dispatchersProvider.io)
            .onEach { result ->
                _uiState.update {
                    it.copy(
                        isPurging = false,
                        isExpired = true,
                        messages = emptyList(),
                        room = it.room?.copy(status = MailboxRoom.STATUS_EXPIRED),
                        infoMessage = if (!result.remotePurged) {
                            "Local mailbox cleared; remote purge may need retry when online"
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
                        messages = emptyList(),
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
                val expired = roomNow?.status == MailboxRoom.STATUS_EXPIRED
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
                if (roomNow?.status == MailboxRoom.STATUS_EXPIRED) {
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

    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty()
        return mapFriendlyError(message) ?: message.ifBlank { error::class.java.simpleName }
    }

    private fun mapFriendlyError(message: String): String? = when {
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
}
