package com.vault.vanishx.presentation.security

import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.data.security.SecurityPinStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SecuritySettingsUiState(
    val hasUnlockPin: Boolean = false,
    val hasPanicPin: Boolean = false,
    val unlockPin: String = "",
    val unlockPinConfirm: String = "",
    val panicPin: String = "",
    val panicPinConfirm: String = "",
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface SecuritySettingsAction {
    data class UnlockPinChanged(val value: String) : SecuritySettingsAction
    data class UnlockPinConfirmChanged(val value: String) : SecuritySettingsAction
    data class PanicPinChanged(val value: String) : SecuritySettingsAction
    data class PanicPinConfirmChanged(val value: String) : SecuritySettingsAction
    data object SaveUnlockPin : SecuritySettingsAction
    data object SavePanicPin : SecuritySettingsAction
    data object ClearUnlockPin : SecuritySettingsAction
    data object ClearPanicPin : SecuritySettingsAction
    data object ClearFeedback : SecuritySettingsAction
    data object Back : SecuritySettingsAction
}

@HiltViewModel
@Suppress("ComplexMethod")
class SecuritySettingsViewModel @Inject constructor(
    private val securityPinStore: SecurityPinStore,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(refreshState())
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    fun onAction(action: SecuritySettingsAction) {
        when (action) {
            is SecuritySettingsAction.UnlockPinChanged ->
                updatePinField(unlock = true, confirm = false, action.value)
            is SecuritySettingsAction.UnlockPinConfirmChanged ->
                updatePinField(unlock = true, confirm = true, action.value)
            is SecuritySettingsAction.PanicPinChanged ->
                updatePinField(unlock = false, confirm = false, action.value)
            is SecuritySettingsAction.PanicPinConfirmChanged ->
                updatePinField(unlock = false, confirm = true, action.value)
            SecuritySettingsAction.SaveUnlockPin -> saveUnlock()
            SecuritySettingsAction.SavePanicPin -> savePanic()
            SecuritySettingsAction.ClearUnlockPin -> clearUnlock()
            SecuritySettingsAction.ClearPanicPin -> clearPanic()
            SecuritySettingsAction.ClearFeedback -> clearFeedback()
            SecuritySettingsAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
        }
    }

    private fun updatePinField(unlock: Boolean, confirm: Boolean, value: String) {
        val digits = value.filter { it.isDigit() }.take(SecurityPinStore.PIN_MAX_LENGTH)
        _uiState.update { state ->
            when {
                unlock && !confirm -> state.copy(unlockPin = digits, errorMessage = null, infoMessage = null)
                unlock && confirm -> state.copy(unlockPinConfirm = digits, errorMessage = null, infoMessage = null)
                !unlock && !confirm -> state.copy(panicPin = digits, errorMessage = null, infoMessage = null)
                else -> state.copy(panicPinConfirm = digits, errorMessage = null, infoMessage = null)
            }
        }
    }

    private fun saveUnlock() {
        val state = _uiState.value
        if (state.unlockPin != state.unlockPinConfirm) {
            _uiState.update { it.copy(errorMessage = "Unlock PIN confirmation does not match") }
            return
        }
        runCatching { securityPinStore.setUnlockPin(state.unlockPin) }
            .onSuccess { _uiState.value = refreshState().copy(infoMessage = "Unlock PIN saved") }
            .onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: e::class.java.simpleName) }
            }
    }

    private fun savePanic() {
        val state = _uiState.value
        if (state.panicPin != state.panicPinConfirm) {
            _uiState.update { it.copy(errorMessage = "Panic PIN confirmation does not match") }
            return
        }
        runCatching { securityPinStore.setPanicPin(state.panicPin) }
            .onSuccess { _uiState.value = refreshState().copy(infoMessage = "Panic PIN saved") }
            .onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: e::class.java.simpleName) }
            }
    }

    private fun clearUnlock() {
        securityPinStore.clearUnlockPin()
        _uiState.value = refreshState().copy(infoMessage = "Unlock PIN removed")
    }

    private fun clearPanic() {
        securityPinStore.clearPanicPin()
        _uiState.value = refreshState().copy(infoMessage = "Panic PIN removed")
    }

    private fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun refreshState() = SecuritySettingsUiState(
        hasUnlockPin = securityPinStore.hasUnlockPin(),
        hasPanicPin = securityPinStore.hasPanicPin(),
    )
}
