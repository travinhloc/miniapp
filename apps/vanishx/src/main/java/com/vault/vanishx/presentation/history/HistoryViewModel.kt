@file:Suppress("ComplexMethod")

package com.vault.vanishx.presentation.history

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.presentation.conversation.ConversationRowModel
import com.vault.vanishx.presentation.home.toConversationRow
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
    Favorite,
}

enum class HistoryRoomMeta {
    Creator,
    Member,
    Archived,
    Left,
}

data class HistoryRoomItem(
    val row: ConversationRowModel,
    val meta: HistoryRoomMeta,
)

data class HistoryUiState(
    val filter: HistoryRoomFilter = HistoryRoomFilter.Open,
    val searchQuery: String = "",
    val rooms: List<HistoryRoomItem> = emptyList(),
    val isPro: Boolean = false,
    val hasAnyRooms: Boolean = false,
)

sealed interface HistoryAction {
    data object Back : HistoryAction
    data class SetFilter(val filter: HistoryRoomFilter) : HistoryAction
    data class SearchQueryChanged(val value: String) : HistoryAction
    data class OpenRoom(val roomId: String) : HistoryAction
    data object OpenPaywall : HistoryAction
    data object CreateRoom : HistoryAction
    data object JoinRoom : HistoryAction
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
            is HistoryAction.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = action.value) }
                applyFilter()
            }
            is HistoryAction.OpenRoom -> launch {
                _navigator.emit(MailboxDestination.Room(action.roomId))
            }
            HistoryAction.OpenPaywall -> launch {
                _navigator.emit(PaywallDestination.Paywall)
            }
            HistoryAction.CreateRoom -> launch {
                _navigator.emit(MailboxDestination.Create)
            }
            HistoryAction.JoinRoom -> launch {
                _navigator.emit(MailboxDestination.Join)
            }
        }
    }

    private fun loadRooms() {
        flow {
            val now = System.currentTimeMillis()
            val isPro = _uiState.value.isPro
            val rooms = mailboxRepository.getAllRooms()
            emit(
                rooms.map { room ->
                    val last = mailboxRepository.getLatestVisibleMessage(room.id, now)
                    val messages = mailboxRepository.getMessages(room.id)
                    val row = room.toConversationRow(last, messages, isPro, now)
                    HistoryRoomItem(row = row, meta = room.toMeta(row))
                }.sortedWith(
                    compareBy<HistoryRoomItem> { it.row.isLeft }
                        .thenBy { it.row.isExpired }
                        .thenByDescending { it.row.remainingMs },
                ),
            )
        }
            .flowOn(dispatchersProvider.io)
            .onEach { items ->
                allRooms = items
                applyFilter()
            }
            .catch { e -> Timber.w(e, "Load history rooms failed") }
            .launchIn(viewModelScope)
    }

    private fun applyFilter() {
        val filter = _uiState.value.filter
        val query = _uiState.value.searchQuery.trim()
        val filtered = allRooms.filter { item ->
            val row = item.row
            val matchesFilter = when (filter) {
                HistoryRoomFilter.Open -> !row.isExpired && !row.isLeft
                HistoryRoomFilter.Expired -> row.isExpired || row.isLeft
                HistoryRoomFilter.All -> true
                HistoryRoomFilter.Favorite -> row.isFavorite
            }
            val matchesQuery = query.isEmpty() ||
                row.displayName.contains(query, ignoreCase = true) ||
                row.id.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        _uiState.update { it.copy(rooms = filtered, hasAnyRooms = allRooms.isNotEmpty()) }
    }
}

private fun MailboxRoom.toMeta(row: ConversationRowModel): HistoryRoomMeta = when {
    row.isLeft -> HistoryRoomMeta.Left
    row.isExpired -> HistoryRoomMeta.Archived
    role == MailboxRoom.ROLE_CREATOR -> HistoryRoomMeta.Creator
    else -> HistoryRoomMeta.Member
}
