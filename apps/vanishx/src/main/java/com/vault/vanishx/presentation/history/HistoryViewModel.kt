package com.vault.vanishx.presentation.history

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.presentation.home.toHomeItem
import com.vault.vanishx.presentation.mailbox.MailboxDestination
import com.vault.vanishx.presentation.paywall.PaywallDestination
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

enum class HistoryRoomFilter {
    Open,
    Expired,
    All,
}

enum class HistoryRoomMeta {
    Creator,
    Member,
    Archived,
    Left,
}

data class HistoryRoomItem(
    val id: String,
    val displayName: String,
    val meta: HistoryRoomMeta,
    val remainingMs: Long,
    val isExpired: Boolean,
    val isLeft: Boolean,
)

data class HistoryUiState(
    val filter: HistoryRoomFilter = HistoryRoomFilter.Open,
    val rooms: List<HistoryRoomItem> = emptyList(),
    val isPro: Boolean = false,
)

sealed interface HistoryAction {
    data object Back : HistoryAction
    data class SetFilter(val filter: HistoryRoomFilter) : HistoryAction
    data class OpenRoom(val roomId: String) : HistoryAction
    data object OpenPaywall : HistoryAction
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val proEntitlement: ProEntitlementRepository,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        HistoryUiState(isPro = proEntitlement.isProNow()),
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allRooms: List<HistoryRoomItem> = emptyList()

    init {
        proEntitlement.isPro
            .onEach { pro -> _uiState.update { it.copy(isPro = pro) } }
            .launchIn(viewModelScope)
        loadRooms()
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            HistoryAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
            is HistoryAction.SetFilter -> {
                _uiState.update { it.copy(filter = action.filter) }
                applyFilter()
            }
            is HistoryAction.OpenRoom -> launch {
                _navigator.emit(MailboxDestination.Room(action.roomId))
            }
            HistoryAction.OpenPaywall -> launch {
                _navigator.emit(PaywallDestination.Paywall)
            }
        }
    }

    private fun loadRooms() {
        flow { emit(mailboxRepository.getAllRooms()) }
            .flowOn(dispatchersProvider.io)
            .onEach { rooms ->
                val now = System.currentTimeMillis()
                allRooms = rooms
                    .map { it.toHistoryItem(now) }
                    .sortedWith(
                        compareBy<HistoryRoomItem> { it.isLeft }
                            .thenBy { it.isExpired }
                            .thenByDescending { it.remainingMs },
                    )
                applyFilter()
            }
            .catch { e -> Timber.w(e, "Load history rooms failed") }
            .launchIn(viewModelScope)
    }

    private fun applyFilter() {
        val filter = _uiState.value.filter
        val filtered = when (filter) {
            HistoryRoomFilter.Open -> allRooms.filter { !it.isExpired && !it.isLeft }
            HistoryRoomFilter.Expired -> allRooms.filter { it.isExpired || it.isLeft }
            HistoryRoomFilter.All -> allRooms
        }
        _uiState.update { it.copy(rooms = filtered) }
    }
}

private fun MailboxRoom.toHistoryItem(nowMs: Long): HistoryRoomItem {
    val homeItem = toHomeItem(nowMs)
    val left = status == MailboxRoom.STATUS_LEFT
    val meta = when {
        left -> HistoryRoomMeta.Left
        homeItem.isExpired -> HistoryRoomMeta.Archived
        role == MailboxRoom.ROLE_CREATOR -> HistoryRoomMeta.Creator
        else -> HistoryRoomMeta.Member
    }
    return HistoryRoomItem(
        id = id,
        displayName = homeItem.displayName,
        meta = meta,
        remainingMs = homeItem.remainingMs,
        isExpired = homeItem.isExpired,
        isLeft = left,
    )
}
