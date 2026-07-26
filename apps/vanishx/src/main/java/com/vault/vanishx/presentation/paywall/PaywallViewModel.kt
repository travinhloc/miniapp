package com.vault.vanishx.presentation.paywall

import androidx.lifecycle.viewModelScope
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseViewModel
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PaywallUiState(
    val isPro: Boolean = false,
    val showStubActivate: Boolean = false,
)

sealed interface PaywallAction {
    data object Back : PaywallAction
    data object ActivateProStub : PaywallAction
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val proEntitlement: ProEntitlementRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        PaywallUiState(
            isPro = proEntitlement.isProNow(),
            showStubActivate = isProStubToggleEnabled(),
        ),
    )
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        proEntitlement.isPro
            .onEach { pro -> _uiState.update { it.copy(isPro = pro) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: PaywallAction) {
        when (action) {
            PaywallAction.Back -> launch { _navigator.emit(BaseDestination.Up()) }
            PaywallAction.ActivateProStub -> {
                if (!isProStubToggleEnabled()) return
                proEntitlement.setProStub(true)
                launch { _navigator.emit(BaseDestination.Up()) }
            }
        }
    }

    private companion object {
        fun isProStubToggleEnabled(): Boolean =
            BuildConfig.DEBUG && BuildConfig.FLAVOR == "staging"
    }
}
