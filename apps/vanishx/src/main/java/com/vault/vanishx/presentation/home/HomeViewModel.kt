package com.vault.vanishx.presentation.home

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.domain.usecase.SyncActiveMailboxesUseCase
import com.vault.vanishx.presentation.history.HistoryDestination
import com.vault.vanishx.presentation.mailbox.MailboxDestination
import com.vault.vanishx.presentation.security.SecurityDestination
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions", "ComplexMethod")
class HomeViewModel @Inject constructor(
    private val ensureIdentity: EnsureIdentityUseCase,
    private val syncActiveMailboxes: SyncActiveMailboxesUseCase,
    private val consumePendingInvite: ConsumePendingInviteUseCase,
    private val mailboxRepository: MailboxRepository,
    private val pendingInviteStore: PendingInviteStore,
    private val proEntitlement: ProEntitlementRepository,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            showProStubToggle = isProStubToggleEnabled(),
            isProStub = proEntitlement.isProNow(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        proEntitlement.isPro
            .onEach { pro -> _uiState.update { it.copy(isProStub = pro) } }
            .launchIn(viewModelScope)
        bootstrapIdentity()
    }

    private fun bootstrapIdentity() {
        flow {
            val identity = ensureIdentity()
            val joined = consumePendingInvite()
            emit(identity to joined)
        }
            .injectLoading()
            .onEach { (identity, joinedRoom) ->
                _uiState.update {
                    it.copy(
                        anonymousId = identity.anonymousId,
                        isBootstrappingIdentity = false,
                    )
                }
                refreshRooms()
                if (joinedRoom != null) {
                    _navigator.emit(MailboxDestination.Room(joinedRoom.id))
                }
            }
            .flowOn(dispatchersProvider.io)
            .catch { e ->
                _uiState.update { it.copy(isBootstrappingIdentity = false) }
                _error.emit(e)
            }
            .launchIn(viewModelScope)
    }

    private fun syncOnOpen() {
        if (_uiState.value.isMailboxSyncing) return
        _uiState.update { it.copy(isMailboxSyncing = true) }
        flow { emit(syncActiveMailboxes()) }
            .flowOn(dispatchersProvider.io)
            .onEach {
                _uiState.update { it.copy(isMailboxSyncing = false) }
                refreshRooms()
            }
            .catch { e ->
                Timber.w(e, "Sync active mailboxes on open failed")
                _uiState.update { it.copy(isMailboxSyncing = false) }
                refreshRooms()
            }
            .launchIn(viewModelScope)
    }

    private fun refreshRooms() {
        flow { emit(mailboxRepository.getAllRooms()) }
            .flowOn(dispatchersProvider.io)
            .onEach { rooms ->
                val now = System.currentTimeMillis()
                val items = rooms
                    .filter { it.status != com.vault.vanishx.domain.model.MailboxRoom.STATUS_LEFT }
                    .map { it.toHomeItem(now) }
                    .sortedWith(
                        compareBy<HomeRoomItem> { it.isExpired }
                            .thenByDescending { it.remainingMs },
                    )
                _uiState.update {
                    it.copy(
                        recentRooms = items.take(RECENT_LIMIT),
                        totalRoomCount = items.size,
                        hasMoreRooms = items.size > RECENT_LIMIT,
                    )
                }
            }
            .catch { e -> Timber.w(e, "Refresh rooms failed") }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CreateRoom -> navigateCreate()
            HomeAction.JoinRoom, HomeAction.ScanQr -> navigateJoin()
            HomeAction.Resume -> syncOnOpen()
            HomeAction.OpenSettings -> navigateSettings()
            HomeAction.OpenHistory -> navigateHistory()
            HomeAction.ToggleProStub -> toggleProStub()
            is HomeAction.InviteDraftChanged -> _uiState.update {
                it.copy(
                    inviteDraft = action.value,
                    inviteDraftEmpty = false,
                    errorMessage = null,
                )
            }
            HomeAction.JoinFromDraft -> joinFromDraft()
            is HomeAction.OpenRoom -> navigateRoom(action.roomId)
            is HomeAction.DeleteRoom -> deleteRoom(action.roomId)
            HomeAction.DismissShareHint -> _uiState.update { it.copy(shareHintUri = null) }
        }
    }

    private fun navigateCreate() = launch { _navigator.emit(MailboxDestination.Create) }
    private fun navigateJoin() = launch { _navigator.emit(MailboxDestination.Join) }
    private fun navigateSettings() = launch { _navigator.emit(SecurityDestination.Settings) }
    private fun navigateHistory() = launch { _navigator.emit(HistoryDestination.History) }
    private fun navigateRoom(roomId: String) = launch {
        _navigator.emit(MailboxDestination.Room(roomId))
    }

    private fun toggleProStub() {
        if (!isProStubToggleEnabled()) return
        proEntitlement.setProStub(!_uiState.value.isProStub)
    }

    private fun joinFromDraft() {
        val draft = _uiState.value.inviteDraft.trim()
        if (draft.isEmpty()) {
            _uiState.update { it.copy(inviteDraftEmpty = true) }
            return
        }
        pendingInviteStore.save(draft)
        navigateJoin()
    }

    private fun deleteRoom(roomId: String) {
        flow {
            val room = mailboxRepository.getRoom(roomId) ?: return@flow
            mailboxRepository.deleteMessagesForRoom(roomId)
            mailboxRepository.upsertRoom(room.copy(status = com.vault.vanishx.domain.model.MailboxRoom.STATUS_LEFT))
            emit(Unit)
        }
            .flowOn(dispatchersProvider.io)
            .onEach { refreshRooms() }
            .catch { e -> Timber.w(e, "Delete room failed") }
            .launchIn(viewModelScope)
    }

    fun onReturnedFromCreate(inviteUri: String?) {
        if (!inviteUri.isNullOrBlank()) {
            _uiState.update { it.copy(shareHintUri = inviteUri) }
        }
        refreshRooms()
    }

    private companion object {
        const val RECENT_LIMIT = 3
        fun isProStubToggleEnabled(): Boolean =
            BuildConfig.DEBUG && BuildConfig.FLAVOR == "staging"
    }
}
