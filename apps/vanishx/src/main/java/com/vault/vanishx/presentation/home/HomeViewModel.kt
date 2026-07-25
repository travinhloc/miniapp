package com.vault.vanishx.presentation.home

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mailboxRepository: MailboxRepository,
    private val ensureIdentity: EnsureIdentityUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        bootstrapIdentity()
        loadRooms()
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
            }
            .flowOn(dispatchersProvider.io)
            .catch { e ->
                _uiState.update { it.copy(isBootstrappingIdentity = false) }
                _error.emit(e)
            }
            .launchIn(viewModelScope)
    }

    private fun loadRooms() {
        flow { emit(mailboxRepository.getActiveRooms()) }
            .onEach { rooms ->
                _uiState.update { it.copy(activeRoomCount = rooms.size) }
            }
            .flowOn(dispatchersProvider.io)
            .catch { e -> _error.emit(e) }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CreateRoom,
            HomeAction.JoinRoom,
            -> {
                _uiState.update { it.copy(showPlaceholder = true) }
            }
            HomeAction.ClearPlaceholder -> {
                _uiState.update { it.copy(showPlaceholder = false) }
            }
        }
    }
}
