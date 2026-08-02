package com.vault.vanishx.presentation.security

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.PinVerifyResult
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.data.security.UnlockFailResult
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
    /** Visible burn only when auto-wipe triggers (not panic). */
    val showBurnOverlay: Boolean = false,
    val attemptsLeft: Int = SecurityPinStore.MAX_UNLOCK_ATTEMPTS,
    val showWrongPin: Boolean = false,
    val shakeToken: Int = 0,
    val biometricEnabled: Boolean = false,
    val promptBiometric: Boolean = false,
    val biometricError: String? = null,
    val cooldownRemainingMs: Long = 0L,
    val cooldownTierIndex: Int = 0,
)

sealed interface LockAction {
    data class Digit(val value: Char) : LockAction
    data object Backspace : LockAction
    data object Submit : LockAction
    data object RequestBiometric : LockAction
    data object BiometricPromptShown : LockAction
    data object BiometricSuccess : LockAction
    data class BiometricFailed(val message: String) : LockAction
    data object ClearPinDraft : LockAction
    data object TickCooldown : LockAction
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityPinStore: SecurityPinStore,
    private val appLockSession: AppLockSession,
    private val panicWipe: PanicWipeUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    @Suppress("ComplexMethod")
    fun onAction(action: LockAction) {
        when (action) {
            is LockAction.Digit -> appendDigit(action.value)
            LockAction.Backspace -> {
                if (isCoolingDown()) return
                _uiState.update {
                    it.copy(pin = it.pin.dropLast(1), showWrongPin = false, biometricError = null)
                }
            }
            LockAction.Submit -> submit()
            LockAction.RequestBiometric -> {
                if (isCoolingDown()) return
                _uiState.update { it.copy(promptBiometric = true, biometricError = null) }
            }
            LockAction.BiometricPromptShown -> _uiState.update { it.copy(promptBiometric = false) }
            LockAction.BiometricSuccess -> unlock()
            is LockAction.BiometricFailed -> _uiState.update {
                it.copy(biometricError = action.message, promptBiometric = false)
            }
            LockAction.ClearPinDraft -> _uiState.update {
                it.copy(pin = "", showWrongPin = false, biometricError = null)
            }
            LockAction.TickCooldown -> refreshCooldownFromStore()
        }
    }

    private fun initialState(): LockUiState {
        securityPinStore.clearExpiredCooldown()
        val remaining = securityPinStore.remainingCooldownMs()
        return LockUiState(
            attemptsLeft = remainingAttempts(),
            biometricEnabled = securityPinStore.isBiometricEnabled(),
            promptBiometric = securityPinStore.isBiometricEnabled() && remaining <= 0L,
            cooldownRemainingMs = remaining,
            cooldownTierIndex = (securityPinStore.cooldownTier() - 1)
                .coerceAtLeast(0)
                .coerceAtMost(SecurityPinStore.COOLDOWN_DURATIONS_MS.lastIndex),
        )
    }

    private fun appendDigit(digit: Char) {
        if (_uiState.value.isBusy || _uiState.value.showBurnOverlay || isCoolingDown()) return
        val next = (_uiState.value.pin + digit).take(SecurityPinStore.PIN_LENGTH)
        _uiState.update {
            it.copy(pin = next, showWrongPin = false, biometricError = null)
        }
    }

    private fun submit() {
        if (_uiState.value.isBusy || _uiState.value.showBurnOverlay || isCoolingDown()) return
        val pin = _uiState.value.pin
        if (pin.length != SecurityPinStore.PIN_LENGTH) return
        when (securityPinStore.verify(pin)) {
            PinVerifyResult.UNLOCK -> unlock()
            PinVerifyResult.PANIC -> wipe(silent = true)
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
                cooldownRemainingMs = 0L,
            )
        }
    }

    private fun onInvalidPin() {
        when (val result = securityPinStore.recordFailedUnlock()) {
            is UnlockFailResult.Wrong -> _uiState.update {
                it.copy(
                    pin = "",
                    attemptsLeft = result.attemptsLeft,
                    showWrongPin = true,
                    shakeToken = it.shakeToken + 1,
                )
            }
            is UnlockFailResult.Cooldown -> {
                _uiState.update {
                    it.copy(
                        pin = "",
                        attemptsLeft = SecurityPinStore.MAX_UNLOCK_ATTEMPTS,
                        showWrongPin = false,
                        shakeToken = it.shakeToken + 1,
                        cooldownRemainingMs = result.durationMs,
                        cooldownTierIndex = result.tierIndex,
                    )
                }
            }
            UnlockFailResult.Wipe -> {
                _uiState.update {
                    it.copy(
                        showBurnOverlay = true,
                        pin = "",
                        attemptsLeft = 0,
                        showWrongPin = false,
                        shakeToken = it.shakeToken + 1,
                    )
                }
                wipe(silent = false)
            }
        }
    }

    private fun wipe(silent: Boolean) {
        _uiState.update {
            it.copy(
                isBusy = true,
                showBurnOverlay = !silent,
            )
        }
        flow { emit(panicWipe()) }
            .flowOn(dispatchersProvider.io)
            .onEach {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        wiped = true,
                        unlocked = true,
                        pin = "",
                        showBurnOverlay = !silent && it.showBurnOverlay,
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        biometricError = e.message ?: e::class.java.simpleName,
                        showBurnOverlay = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshCooldownFromStore() {
        securityPinStore.clearExpiredCooldown()
        val remaining = securityPinStore.remainingCooldownMs()
        _uiState.update { it.copy(cooldownRemainingMs = remaining) }
    }

    private fun isCoolingDown(): Boolean = _uiState.value.cooldownRemainingMs > 0L

    private fun remainingAttempts(): Int =
        (SecurityPinStore.MAX_UNLOCK_ATTEMPTS - securityPinStore.failedUnlockAttempts())
            .coerceIn(0, SecurityPinStore.MAX_UNLOCK_ATTEMPTS)
}
