package com.vault.vanishx.presentation.home

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.domain.usecase.SmokeMailboxRemoteUseCase
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
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ensureIdentity: EnsureIdentityUseCase,
    private val syncActiveMailboxes: SyncActiveMailboxesUseCase,
    private val consumePendingInvite: ConsumePendingInviteUseCase,
    private val smokeMailboxRemote: SmokeMailboxRemoteUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(showMailboxSmoke = isMailboxSmokeEnabled()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
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
            HomeAction.ClearPlaceholder -> {
                _uiState.update { it.copy(showPlaceholder = false) }
            }
            HomeAction.Resume -> syncOnOpen()
            HomeAction.OpenSecurity -> launch {
                _navigator.emit(SecurityDestination.Settings)
            }
            HomeAction.RunMailboxSmoke -> runMailboxSmoke()
            HomeAction.ClearMailboxSmokeFeedback -> {
                _uiState.update {
                    it.copy(
                        isMailboxSmokeRunning = false,
                        mailboxSmokeResult = null,
                        mailboxSmokeError = null,
                    )
                }
            }
        }
    }

    private fun runMailboxSmoke() {
        if (!isMailboxSmokeEnabled()) return
        if (_uiState.value.isMailboxSmokeRunning) return

        _uiState.update {
            it.copy(
                isMailboxSmokeRunning = true,
                mailboxSmokeResult = null,
                mailboxSmokeError = null,
            )
        }

        flow {
            val result = withTimeout(SMOKE_TIMEOUT_MS) { smokeMailboxRemote() }
            emit(result)
        }
            .flowOn(dispatchersProvider.io)
            .onEach { result ->
                Timber.i("Mailbox smoke OK: %s", result)
                _uiState.update {
                    it.copy(
                        isMailboxSmokeRunning = false,
                        mailboxSmokeResult = result,
                        mailboxSmokeError = null,
                    )
                }
            }
            .catch { e ->
                val detail = formatSmokeError(e)
                Timber.e(e, "Mailbox smoke failed")
                _uiState.update {
                    it.copy(
                        isMailboxSmokeRunning = false,
                        mailboxSmokeResult = null,
                        mailboxSmokeError = detail,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun formatSmokeError(error: Throwable): String {
        val chain = generateSequence(error) { it.cause }
            .mapIndexed { index, throwable ->
                val prefix = if (index == 0) "" else "Caused by: "
                "$prefix${throwable::class.java.name}: ${throwable.message}"
            }
            .joinToString(separator = "\n")
        val stack = error.stackTraceToString().take(MAX_STACK_CHARS)
        return "$chain\n\n$stack"
    }

    private companion object {
        const val MAX_STACK_CHARS = 3500
        const val SMOKE_TIMEOUT_MS = 25_000L

        fun isMailboxSmokeEnabled(): Boolean =
            BuildConfig.DEBUG && BuildConfig.FLAVOR == "staging"
    }
}
