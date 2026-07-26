package com.vault.vanishx.presentation.security

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.PinVerifyResult
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.usecase.PanicWipeUseCase
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

data class LockUiState(
    val pin: String = "",
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val unlocked: Boolean = false,
    val wiped: Boolean = false,
)

sealed interface LockAction {
    data class PinChanged(val value: String) : LockAction
    data object Submit : LockAction
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityPinStore: SecurityPinStore,
    private val appLockSession: AppLockSession,
    private val panicWipe: PanicWipeUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun onAction(action: LockAction) {
        when (action) {
            is LockAction.PinChanged -> {
                val digits = action.value.filter { it.isDigit() }
                    .take(SecurityPinStore.PIN_MAX_LENGTH)
                _uiState.update { it.copy(pin = digits, errorMessage = null) }
            }
            LockAction.Submit -> submit()
        }
    }

    private fun submit() {
        if (_uiState.value.isBusy) return
        val pin = _uiState.value.pin
        when (securityPinStore.verify(pin)) {
            PinVerifyResult.UNLOCK -> {
                appLockSession.unlock()
                _uiState.update { it.copy(unlocked = true, pin = "", errorMessage = null) }
            }
            PinVerifyResult.PANIC -> {
                _uiState.update { it.copy(isBusy = true, errorMessage = null) }
                flow { emit(panicWipe()) }
                    .flowOn(dispatchersProvider.io)
                    .onEach {
                        _uiState.update {
                            it.copy(isBusy = false, wiped = true, unlocked = true, pin = "")
                        }
                    }
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                errorMessage = e.message ?: e::class.java.simpleName,
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            }
            PinVerifyResult.INVALID -> {
                _uiState.update {
                    it.copy(pin = "", errorMessage = "Incorrect PIN")
                }
            }
        }
    }
}
