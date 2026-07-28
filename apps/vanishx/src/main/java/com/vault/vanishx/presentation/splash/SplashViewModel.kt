package com.vault.vanishx.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniapp.core.common.DispatchersProvider
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val ensureIdentity: EnsureIdentityUseCase,
    private val dispatchersProvider: DispatchersProvider,
) : ViewModel() {

    private val _bootstrapReady = MutableStateFlow(false)
    val bootstrapReady: StateFlow<Boolean> = _bootstrapReady.asStateFlow()

    init {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { ensureIdentity() }
                .onFailure { Timber.w(it, "Splash identity bootstrap failed") }
            // Always release the gate — splash must not hang on crypto errors.
            _bootstrapReady.value = true
        }
    }
}
