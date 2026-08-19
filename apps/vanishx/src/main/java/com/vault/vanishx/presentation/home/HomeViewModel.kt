package com.vault.vanishx.presentation.home

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
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
@Suppress("TooManyFunctions", "ComplexMethod", "LargeClass")
class HomeViewModel @Inject constructor(
    private val ensureIdentity: EnsureIdentityUseCase,
    private val syncActiveMailboxes: SyncActiveMailboxesUseCase,
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
        observePendingInvite()
        bootstrapIdentity()
    }

    private var routedPendingUri: String? = null

    private fun observePendingInvite() {
        pendingInviteStore.peek()
        pendingInviteStore.pending
            .onEach { uri ->
                _uiState.update { it.copy(pendingInviteUri = uri) }
                if (uri == null) {
                    routedPendingUri = null
                } else if (uri != routedPendingUri) {
                    routedPendingUri = uri
                    _navigator.emit(MailboxDestination.Join)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun bootstrapIdentity() {
        flow { emit(ensureIdentity()) }
            .injectLoading()
            .onEach { identity ->
                _uiState.update {
                    it.copy(
                        anonymousId = identity.anonymousId,
                        isBootstrappingIdentity = false,
                    )
                }
                refreshRooms()
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
        flow {
            val now = System.currentTimeMillis()
            val isPro = _uiState.value.isProStub
            val rooms = mailboxRepository.getAllRooms()
            val items = rooms
                .filter { it.isHomeListEligible(now) }
                .map { room ->
                    val last = mailboxRepository.getLatestVisibleMessage(room.id, now)
                    val messages = mailboxRepository.getMessages(room.id)
                    room.toConversationRow(last, messages, isPro, now)
                }
                .sortedForHome()
            emit(items)
        }
            .flowOn(dispatchersProvider.io)
            .onEach { items ->
                _uiState.update { state ->
                    state.copy(rooms = items).withVisibleRooms()
                }
            }
            .catch { e -> Timber.w(e, "Refresh rooms failed") }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CreateRoom -> navigateCreate()
            HomeAction.JoinRoom, HomeAction.ScanQr -> navigateJoin()
            is HomeAction.PasteInvite -> {
                val text = action.text.trim()
                if (text.isEmpty()) {
                    _uiState.update { it.copy(inviteDraftEmpty = true) }
                } else {
                    _uiState.update {
                        it.copy(inviteDraft = text, inviteDraftEmpty = false, errorMessage = null)
                    }
                    pendingInviteStore.save(text)
                    navigateJoin()
                }
            }
            HomeAction.Resume -> {
                refreshPendingInvite()
                syncOnOpen()
            }
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
            is HomeAction.SearchQueryChanged -> _uiState.update {
                it.copy(searchQuery = action.value).withVisibleRooms()
            }
            is HomeAction.SetFilter -> _uiState.update {
                it.copy(listFilter = action.filter).withVisibleRooms()
            }
            is HomeAction.ShareWaiting -> showWaitingInvite(action.roomId)
            HomeAction.DismissWaitingInvite -> _uiState.update { it.copy(waitingInviteUri = null) }
            HomeAction.OpenPendingInvite -> navigateJoin()
            HomeAction.DismissPendingInvite -> {
                pendingInviteStore.clear()
                _uiState.update { it.copy(pendingInviteUri = null) }
            }
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

    private fun refreshPendingInvite() {
        _uiState.update { it.copy(pendingInviteUri = pendingInviteStore.peek()) }
    }

    private fun showWaitingInvite(roomId: String) {
        flow {
            val room = mailboxRepository.getRoom(roomId) ?: return@flow
            emit(
                InviteUriCodec.format(
                    RoomInvite(
                        roomId = room.id,
                        roomKey = room.roomKey,
                        expiresAt = room.expiresAt.takeIf { it > 0L },
                    ),
                ),
            )
        }
            .flowOn(dispatchersProvider.io)
            .onEach { uri -> _uiState.update { it.copy(waitingInviteUri = uri) } }
            .catch { e -> Timber.w(e, "Waiting invite failed") }
            .launchIn(viewModelScope)
    }

    private fun HomeUiState.withVisibleRooms(): HomeUiState {
        val query = searchQuery.trim()
        val filtered = rooms.filter { room ->
            val matchesFilter = when (listFilter) {
                HomeListFilter.All, HomeListFilter.Open -> !room.isExpired
                HomeListFilter.Expired -> room.isExpired
                HomeListFilter.Favorite -> room.isFavorite
            }
            val matchesQuery = query.isEmpty() ||
                room.displayName.contains(query, ignoreCase = true) ||
                room.id.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        return copy(visibleRooms = filtered)
    }

    private companion object {
        fun isProStubToggleEnabled(): Boolean =
            BuildConfig.DEBUG && BuildConfig.FLAVOR == "staging"
    }
}
