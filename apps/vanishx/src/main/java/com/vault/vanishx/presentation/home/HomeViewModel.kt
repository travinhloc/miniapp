package com.vault.vanishx.presentation.home

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.domain.usecase.SyncActiveMailboxesUseCase
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
class HomeViewModel @Inject constructor(
    private val ensureIdentity: EnsureIdentityUseCase,
    private val syncActiveMailboxes: SyncActiveMailboxesUseCase,
    private val consumePendingInvite: ConsumePendingInviteUseCase,
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
            .onEach { result ->
                _uiState.update {
                    it.copy(
                        isMailboxSyncing = false,
                        activeRoomCount = result.activeCount,
                    )
                }
            }
            .catch { e ->
                Timber.w(e, "Sync active mailboxes on open failed")
                _uiState.update { it.copy(isMailboxSyncing = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CreateRoom -> launch {
                _navigator.emit(MailboxDestination.Create)
            }
            HomeAction.JoinRoom -> launch {
                _navigator.emit(MailboxDestination.Join)
            }
            HomeAction.Resume -> syncOnOpen()
            HomeAction.OpenSecurity -> launch {
                _navigator.emit(SecurityDestination.Settings)
            }
            HomeAction.ToggleProStub -> {
                if (!isProStubToggleEnabled()) return
                proEntitlement.setProStub(!_uiState.value.isProStub)
            }
        }
    }

    private companion object {
        fun isProStubToggleEnabled(): Boolean =
            BuildConfig.DEBUG && BuildConfig.FLAVOR == "staging"
    }
}
