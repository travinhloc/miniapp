package com.miniapp.ui.screens.main.home

import androidx.lifecycle.viewModelScope
import com.miniapp.domain.usecases.UseCase
import com.miniapp.core.mvvm.BaseViewModel
import com.miniapp.ui.models.UiModel
import com.miniapp.ui.models.toUiModel
import com.miniapp.core.common.DispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    useCase: UseCase,
) : BaseViewModel() {

    private val _uiModels = MutableStateFlow<ImmutableList<UiModel>>(persistentListOf())
    val uiModels = _uiModels.asStateFlow()

    init {
        useCase()
            .injectLoading()
            .onEach { result ->
                val uiModels = result.map { it.toUiModel() }
                _uiModels.emit(uiModels.toImmutableList())
            }
            .flowOn(dispatchersProvider.io)
            .catch { e -> _error.emit(e) }
            .launchIn(viewModelScope)
    }
}
