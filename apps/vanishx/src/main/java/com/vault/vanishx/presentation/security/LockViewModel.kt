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
    val unlocked: Boolean = false,
    val wiped: Boolean = false,
    val showBurnOverlay: Boolean = false,
    val attemptsLeft: Int = SecurityPinStore.MAX_UNLOCK_ATTEMPTS,
    val showWrongPin: Boolean = false,
    val shakeToken: Int = 0,
    val biometricEnabled: Boolean = false,
    val promptBiometric: Boolean = false,
    val biometricError: String? = null,
)

sealed interface LockAction {
    data class Digit(val value: Char) : LockAction
    data object Backspace : LockAction
    data object Submit : LockAction
    data object RequestBiometric : LockAction
    data object BiometricPromptShown : LockAction
    data object BiometricSuccess : LockAction
    data class BiometricFailed(val message: String) : LockAction
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityPinStore: SecurityPinStore,
    private val appLockSession: AppLockSession,
    private val panicWipe: PanicWipeUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        LockUiState(
            attemptsLeft = remainingAttempts(),
            biometricEnabled = securityPinStore.isBiometricEnabled(),
            promptBiometric = securityPinStore.isBiometricEnabled(),
        ),
    )
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    @Suppress("ComplexMethod")
    fun onAction(action: LockAction) {
        when (action) {
            is LockAction.Digit -> appendDigit(action.value)
            LockAction.Backspace -> _uiState.update {
                it.copy(pin = it.pin.dropLast(1), showWrongPin = false, biometricError = null)
            }
            LockAction.Submit -> submit()
            LockAction.RequestBiometric -> _uiState.update {
                it.copy(promptBiometric = true, biometricError = null)
            }
            LockAction.BiometricPromptShown -> _uiState.update { it.copy(promptBiometric = false) }
            LockAction.BiometricSuccess -> unlock()
            is LockAction.BiometricFailed -> _uiState.update {
                it.copy(biometricError = action.message, promptBiometric = false)
            }
        }
    }

    private fun appendDigit(digit: Char) {
        if (_uiState.value.isBusy || _uiState.value.showBurnOverlay) return
        val next = (_uiState.value.pin + digit).take(SecurityPinStore.PIN_LENGTH)
        _uiState.update {
            it.copy(pin = next, showWrongPin = false, biometricError = null)
        }
        if (next.length == SecurityPinStore.PIN_LENGTH) submit()
    }

    private fun submit() {
        if (_uiState.value.isBusy || _uiState.value.showBurnOverlay) return
        val pin = _uiState.value.pin
        if (pin.length != SecurityPinStore.PIN_LENGTH) return
        when (securityPinStore.verify(pin)) {
            PinVerifyResult.UNLOCK -> unlock()
            PinVerifyResult.PANIC -> wipe()
            PinVerifyResult.INVALID -> onInvalidPin()
        }
    }

    private fun unlock() {
        securityPinStore.clearFailedUnlockAttempts()
        appLockSession.unlock()
        _uiState.update {
            it.copy(
                unlocked = true,
                pin = "",
                showWrongPin = false,
                biometricError = null,
                attemptsLeft = SecurityPinStore.MAX_UNLOCK_ATTEMPTS,
            )
        }
    }

    private fun onInvalidPin() {
        val failures = securityPinStore.recordFailedUnlock()
        val left = (SecurityPinStore.MAX_UNLOCK_ATTEMPTS - failures).coerceAtLeast(0)
        if (failures >= SecurityPinStore.MAX_UNLOCK_ATTEMPTS) {
            _uiState.update {
                it.copy(
                    showBurnOverlay = true,
                    pin = "",
                    attemptsLeft = 0,
                    showWrongPin = false,
                    shakeToken = it.shakeToken + 1,
                )
            }
            wipe()
        } else {
            _uiState.update {
                it.copy(
                    pin = "",
                    attemptsLeft = left,
                    showWrongPin = true,
                    shakeToken = it.shakeToken + 1,
                )
            }
        }
    }

    private fun wipe() {
        _uiState.update { it.copy(isBusy = true, showBurnOverlay = true) }
        flow { emit(panicWipe()) }
            .flowOn(dispatchersProvider.io)
            .onEach {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        wiped = true,
                        unlocked = true,
                        pin = "",
                        showBurnOverlay = true,
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        biometricError = e.message ?: e::class.java.simpleName,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun remainingAttempts(): Int =
        (SecurityPinStore.MAX_UNLOCK_ATTEMPTS - securityPinStore.failedUnlockAttempts())
            .coerceIn(0, SecurityPinStore.MAX_UNLOCK_ATTEMPTS)
}
