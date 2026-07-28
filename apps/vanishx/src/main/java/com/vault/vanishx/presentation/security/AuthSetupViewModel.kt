package com.vault.vanishx.presentation.security

import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class AuthSetupStep {
    Enter,
    Confirm,
    Biometric,
}

data class AuthSetupUiState(
    val step: AuthSetupStep = AuthSetupStep.Enter,
    val pin: String = "",
    val firstPin: String = "",
    val isBusy: Boolean = false,
    val showMismatch: Boolean = false,
    val shakeToken: Int = 0,
    val completed: Boolean = false,
)

sealed interface AuthSetupAction {
    data class Digit(val value: Char) : AuthSetupAction
    data object Backspace : AuthSetupAction
    data class EnableBiometric(val enabled: Boolean) : AuthSetupAction
}

@HiltViewModel
class AuthSetupViewModel @Inject constructor(
    private val securityPinStore: SecurityPinStore,
    private val appLockSession: AppLockSession,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AuthSetupUiState())
    val uiState: StateFlow<AuthSetupUiState> = _uiState.asStateFlow()

    fun onAction(action: AuthSetupAction) {
        when (action) {
            is AuthSetupAction.Digit -> appendDigit(action.value)
            AuthSetupAction.Backspace -> backspace()
            is AuthSetupAction.EnableBiometric -> finish(action.enabled)
        }
    }

    private fun appendDigit(digit: Char) {
        val state = _uiState.value
        if (state.step == AuthSetupStep.Biometric || state.isBusy) return
        if (state.pin.length >= SecurityPinStore.PIN_LENGTH) return
        val next = state.pin + digit
        _uiState.update { it.copy(pin = next, showMismatch = false) }
        if (next.length == SecurityPinStore.PIN_LENGTH) {
            onPinComplete(next)
        }
    }

    private fun backspace() {
        _uiState.update {
            it.copy(pin = it.pin.dropLast(1), showMismatch = false)
        }
    }

    private fun onPinComplete(pin: String) {
        val state = _uiState.value
        when (state.step) {
            AuthSetupStep.Enter -> {
                _uiState.update {
                    it.copy(
                        step = AuthSetupStep.Confirm,
                        firstPin = pin,
                        pin = "",
                        showMismatch = false,
                    )
                }
            }
            AuthSetupStep.Confirm -> {
                if (pin != state.firstPin) {
                    _uiState.update {
                        it.copy(
                            step = AuthSetupStep.Enter,
                            pin = "",
                            firstPin = "",
                            showMismatch = true,
                            shakeToken = it.shakeToken + 1,
                        )
                    }
                } else {
                    runCatching { securityPinStore.setUnlockPin(pin) }
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    step = AuthSetupStep.Biometric,
                                    pin = "",
                                    showMismatch = false,
                                )
                            }
                        }
                }
            }
            AuthSetupStep.Biometric -> Unit
        }
    }

    private fun finish(enableBiometric: Boolean) {
        securityPinStore.setBiometricEnabled(enableBiometric)
        securityPinStore.clearFailedUnlockAttempts()
        appLockSession.unlock()
        _uiState.update { it.copy(completed = true) }
    }
}
