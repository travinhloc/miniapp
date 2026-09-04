package com.vault.vanishx.presentation.security

import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.R
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.model.BlockedPeer
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.presentation.history.HistoryDestination
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
import javax.inject.Inject

data class SecuritySettingsUiState(
    val anonymousId: String? = null,
    val hasUnlockPin: Boolean = false,
    val hasPanicPin: Boolean = false,
    val unlockCurrentPin: String = "",
    val unlockPin: String = "",
    val unlockPinConfirm: String = "",
    val panicCurrentPin: String = "",
    val panicPin: String = "",
    val panicPinConfirm: String = "",
    val flagSecureEnabled: Boolean = true,
    val autoWipeEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val promptBiometricEnable: Boolean = false,
    val blockedPeers: List<BlockedPeer> = emptyList(),
    val showProStubToggle: Boolean = false,
    val isProStub: Boolean = false,
    val pendingClearPin: PendingClearPin? = null,
    val clearPinDraft: String = "",
    val clearPinErrorRes: Int? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    /** String resource for pin-form validation (shown next to the fields). */
    val pinFormErrorRes: Int? = null,
)

enum class PendingClearPin {
    Unlock,
    Panic,
}

sealed interface SecuritySettingsAction {
    data class UnlockCurrentPinChanged(val value: String) : SecuritySettingsAction
    data class UnlockPinChanged(val value: String) : SecuritySettingsAction
    data class UnlockPinConfirmChanged(val value: String) : SecuritySettingsAction
    data class PanicCurrentPinChanged(val value: String) : SecuritySettingsAction
    data class PanicPinChanged(val value: String) : SecuritySettingsAction
    data class PanicPinConfirmChanged(val value: String) : SecuritySettingsAction
    data object SaveUnlockPin : SecuritySettingsAction
    data object SavePanicPin : SecuritySettingsAction
    data object ClearUnlockPin : SecuritySettingsAction
    data object ClearPanicPin : SecuritySettingsAction
    data class ClearPinDraftChanged(val value: String) : SecuritySettingsAction
    data object ConfirmClearPin : SecuritySettingsAction
    data object DismissClearPin : SecuritySettingsAction
    data object ClearFeedback : SecuritySettingsAction
    data object OpenHistory : SecuritySettingsAction
    data object OpenPaywall : SecuritySettingsAction
    data object RestorePurchases : SecuritySettingsAction
    data object ToggleProStub : SecuritySettingsAction
    data class SetFlagSecure(val enabled: Boolean) : SecuritySettingsAction
    data class SetAutoWipe(val enabled: Boolean) : SecuritySettingsAction
    data class SetBiometric(val enabled: Boolean) : SecuritySettingsAction
    data object BiometricEnablePromptShown : SecuritySettingsAction
    data object BiometricEnableSuccess : SecuritySettingsAction
    data class BiometricEnableFailed(val message: String) : SecuritySettingsAction
    data class UnblockPeer(val peerPub: String) : SecuritySettingsAction
    data object Back : SecuritySettingsAction
}

@HiltViewModel
@Suppress("ComplexMethod", "TooManyFunctions", "LargeClass")
class SecuritySettingsViewModel @Inject constructor(
    private val securityPinStore: SecurityPinStore,
    private val ensureIdentity: EnsureIdentityUseCase,
    private val proEntitlement: ProEntitlementRepository,
    private val blockRepository: BlockRepository,
    private val dispatchersProvider: DispatchersProvider,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(refreshState(anonymousId = null))
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    init {
        proEntitlement.isPro
            .onEach { pro -> _uiState.update { it.copy(isProStub = pro) } }
            .launchIn(viewModelScope)
        flow { emit(ensureIdentity().anonymousId) }
            .onEach { id -> _uiState.update { it.copy(anonymousId = id) } }
            .catch { /* optional identity display */ }
            .launchIn(viewModelScope)
        refreshBlockedPeers()
    }

    fun onAction(action: SecuritySettingsAction) {
        when (action) {
            is SecuritySettingsAction.UnlockCurrentPinChanged ->
                updatePinField(field = PinFieldKind.UnlockCurrent, action.value)
            is SecuritySettingsAction.UnlockPinChanged ->
                updatePinField(field = PinFieldKind.UnlockNew, action.value)
            is SecuritySettingsAction.UnlockPinConfirmChanged ->
                updatePinField(field = PinFieldKind.UnlockConfirm, action.value)
            is SecuritySettingsAction.PanicCurrentPinChanged ->
                updatePinField(field = PinFieldKind.PanicCurrent, action.value)
            is SecuritySettingsAction.PanicPinChanged ->
                updatePinField(field = PinFieldKind.PanicNew, action.value)
            is SecuritySettingsAction.PanicPinConfirmChanged ->
                updatePinField(field = PinFieldKind.PanicConfirm, action.value)
            SecuritySettingsAction.SaveUnlockPin -> saveUnlock()
            SecuritySettingsAction.SavePanicPin -> savePanic()
            SecuritySettingsAction.ClearUnlockPin -> _uiState.update {
                it.copy(
                    pendingClearPin = PendingClearPin.Unlock,
                    clearPinDraft = "",
                    clearPinErrorRes = null,
                    errorMessage = null,
                    infoMessage = null,
                )
            }
            SecuritySettingsAction.ClearPanicPin -> _uiState.update {
                it.copy(
                    pendingClearPin = PendingClearPin.Panic,
                    clearPinDraft = "",
                    clearPinErrorRes = null,
                    errorMessage = null,
                    infoMessage = null,
                )
            }
            is SecuritySettingsAction.ClearPinDraftChanged -> {
                val digits = action.value.filter { it.isDigit() }.take(SecurityPinStore.PIN_MAX_LENGTH)
                _uiState.update {
                    it.copy(clearPinDraft = digits, clearPinErrorRes = null)
                }
            }
            SecuritySettingsAction.ConfirmClearPin -> confirmClearPin()
            SecuritySettingsAction.DismissClearPin -> _uiState.update {
                it.copy(
                    pendingClearPin = null,
                    clearPinDraft = "",
                    clearPinErrorRes = null,
                )
            }
            SecuritySettingsAction.ClearFeedback -> clearFeedback()
            SecuritySettingsAction.OpenHistory -> launch {
                _navigator.emit(HistoryDestination.History)
            }
            SecuritySettingsAction.OpenPaywall -> launch {
                _navigator.emit(PaywallDestination.Paywall)
            }
            SecuritySettingsAction.RestorePurchases -> _uiState.update {
                it.copy(infoMessage = "Restore purchases is not available yet (IAP stub).")
            }
            SecuritySettingsAction.ToggleProStub -> {
                if (!isProStubToggleEnabled()) return
                proEntitlement.setProStub(!_uiState.value.isProStub)
            }
            is SecuritySettingsAction.SetFlagSecure -> {
                securityPinStore.setFlagSecureEnabled(action.enabled)
                _uiState.update { it.copy(flagSecureEnabled = action.enabled) }
            }
            is SecuritySettingsAction.SetAutoWipe -> {
                securityPinStore.setAutoWipeEnabled(action.enabled)
                _uiState.update { it.copy(autoWipeEnabled = action.enabled) }
            }
            is SecuritySettingsAction.SetBiometric -> setBiometric(action.enabled)
            SecuritySettingsAction.BiometricEnablePromptShown -> _uiState.update {
                it.copy(promptBiometricEnable = false)
            }
            SecuritySettingsAction.BiometricEnableSuccess -> {
                securityPinStore.setBiometricEnabled(true)
                _uiState.update {
                    it.copy(
                        biometricEnabled = true,
                        promptBiometricEnable = false,
                        errorMessage = null,
                        infoMessage = null,
                    )
                }
            }
            is SecuritySettingsAction.BiometricEnableFailed -> _uiState.update {
                it.copy(
                    promptBiometricEnable = false,
                    biometricEnabled = false,
                    errorMessage = action.message,
                )
            }
            is SecuritySettingsAction.UnblockPeer -> unblockPeer(action.peerPub)
            SecuritySettingsAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
        }
    }

    private fun setBiometric(enabled: Boolean) {
        if (!enabled) {
            securityPinStore.setBiometricEnabled(false)
            _uiState.update {
                it.copy(
                    biometricEnabled = false,
                    promptBiometricEnable = false,
                    errorMessage = null,
                )
            }
            return
        }
        if (_uiState.value.biometricEnabled) return
        _uiState.update {
            it.copy(
                promptBiometricEnable = true,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    private enum class PinFieldKind {
        UnlockCurrent,
        UnlockNew,
        UnlockConfirm,
        PanicCurrent,
        PanicNew,
        PanicConfirm,
    }

    private fun updatePinField(field: PinFieldKind, value: String) {
        val digits = value.filter { it.isDigit() }.take(SecurityPinStore.PIN_MAX_LENGTH)
        _uiState.update { state ->
            val cleared = state.copy(errorMessage = null, infoMessage = null, pinFormErrorRes = null)
            when (field) {
                PinFieldKind.UnlockCurrent -> cleared.copy(unlockCurrentPin = digits)
                PinFieldKind.UnlockNew -> cleared.copy(unlockPin = digits)
                PinFieldKind.UnlockConfirm -> cleared.copy(unlockPinConfirm = digits)
                PinFieldKind.PanicCurrent -> cleared.copy(panicCurrentPin = digits)
                PinFieldKind.PanicNew -> cleared.copy(panicPin = digits)
                PinFieldKind.PanicConfirm -> cleared.copy(panicPinConfirm = digits)
            }
        }
    }

    private fun saveUnlock() {
        val state = _uiState.value
        if (state.hasUnlockPin) {
            val currentError = currentPinValidationError(
                current = state.unlockCurrentPin,
                matches = securityPinStore.matchesUnlockPin(state.unlockCurrentPin),
            )
            if (currentError != null) {
                _uiState.update { it.copy(pinFormErrorRes = currentError, errorMessage = null) }
                return
            }
        }
        val formError = pinFormValidationError(state.unlockPin, state.unlockPinConfirm)
        if (formError != null) {
            _uiState.update { it.copy(pinFormErrorRes = formError, errorMessage = null) }
            return
        }
        runCatching { securityPinStore.setUnlockPin(state.unlockPin) }
            .onSuccess {
                _uiState.value = refreshState(state.anonymousId, state.blockedPeers)
                    .copy(infoMessage = "Unlock PIN saved")
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        pinFormErrorRes = null,
                        errorMessage = e.message ?: e::class.java.simpleName,
                    )
                }
            }
    }

    private fun savePanic() {
        val state = _uiState.value
        if (state.hasPanicPin) {
            val currentError = currentPinValidationError(
                current = state.panicCurrentPin,
                matches = securityPinStore.matchesPanicPin(state.panicCurrentPin),
            )
            if (currentError != null) {
                _uiState.update { it.copy(pinFormErrorRes = currentError, errorMessage = null) }
                return
            }
        }
        val formError = pinFormValidationError(state.panicPin, state.panicPinConfirm)
        if (formError != null) {
            _uiState.update { it.copy(pinFormErrorRes = formError, errorMessage = null) }
            return
        }
        runCatching { securityPinStore.setPanicPin(state.panicPin) }
            .onSuccess {
                _uiState.value = refreshState(state.anonymousId, state.blockedPeers)
                    .copy(infoMessage = "Panic PIN saved")
            }
            .onFailure { e ->
                val sameAsUnlock = e.message == SecurityPinStore.PANIC_SAME_AS_UNLOCK
                _uiState.update {
                    it.copy(
                        pinFormErrorRes = if (sameAsUnlock) {
                            R.string.security_panic_pin_rejected
                        } else {
                            null
                        },
                        errorMessage = if (sameAsUnlock) {
                            null
                        } else {
                            e.message ?: e::class.java.simpleName
                        },
                    )
                }
            }
    }

    private fun currentPinValidationError(current: String, matches: Boolean): Int? = when {
        current.length != SecurityPinStore.PIN_LENGTH -> R.string.security_pin_length_error
        !matches -> R.string.security_clear_pin_wrong
        else -> null
    }

    private fun pinFormValidationError(pin: String, confirm: String): Int? = when {
        pin.length != SecurityPinStore.PIN_LENGTH ||
            confirm.length != SecurityPinStore.PIN_LENGTH -> R.string.security_pin_length_error
        pin != confirm -> R.string.security_pin_mismatch_error
        else -> null
    }

    private fun confirmClearPin() {
        val state = _uiState.value
        val target = state.pendingClearPin ?: return
        val draft = state.clearPinDraft
        if (draft.length != SecurityPinStore.PIN_LENGTH) {
            _uiState.update { it.copy(clearPinErrorRes = R.string.security_pin_length_error) }
            return
        }
        val matches = when (target) {
            PendingClearPin.Unlock -> securityPinStore.matchesUnlockPin(draft)
            PendingClearPin.Panic -> securityPinStore.matchesPanicPin(draft)
        }
        if (!matches) {
            _uiState.update {
                it.copy(
                    clearPinDraft = "",
                    clearPinErrorRes = R.string.security_clear_pin_wrong,
                )
            }
            return
        }
        when (target) {
            PendingClearPin.Unlock -> clearUnlock()
            PendingClearPin.Panic -> clearPanic()
        }
    }

    private fun clearUnlock() {
        val state = _uiState.value
        securityPinStore.clearUnlockPin()
        _uiState.value = refreshState(state.anonymousId, state.blockedPeers)
            .copy(infoMessage = "Unlock PIN removed")
    }

    private fun clearPanic() {
        val state = _uiState.value
        securityPinStore.clearPanicPin()
        _uiState.value = refreshState(state.anonymousId, state.blockedPeers)
            .copy(infoMessage = "Panic PIN removed")
    }

    private fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun unblockPeer(peerPub: String) {
        flow {
            blockRepository.unblock(peerPub)
            emit(blockRepository.listBlocked())
        }
            .flowOn(dispatchersProvider.io)
            .onEach { list ->
                _uiState.update { it.copy(blockedPeers = list, errorMessage = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: e::class.java.simpleName) }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshBlockedPeers() {
        flow { emit(blockRepository.listBlocked()) }
            .flowOn(dispatchersProvider.io)
            .onEach { list -> _uiState.update { it.copy(blockedPeers = list) } }
            .catch { /* empty list stays */ }
            .launchIn(viewModelScope)
    }

    private fun refreshState(
        anonymousId: String?,
        blockedPeers: List<BlockedPeer> = emptyList(),
    ): SecuritySettingsUiState =
        SecuritySettingsUiState(
            anonymousId = anonymousId,
            hasUnlockPin = securityPinStore.hasUnlockPin(),
            hasPanicPin = securityPinStore.hasPanicPin(),
            flagSecureEnabled = securityPinStore.isFlagSecureEnabled(),
            autoWipeEnabled = securityPinStore.isAutoWipeEnabled(),
            biometricEnabled = securityPinStore.isBiometricEnabled(),
            blockedPeers = blockedPeers,
            showProStubToggle = isProStubToggleEnabled(),
            isProStub = proEntitlement.isProNow(),
        )

    private companion object {
        fun isProStubToggleEnabled(): Boolean =
            BuildConfig.DEBUG && BuildConfig.FLAVOR == "staging"
    }
}
