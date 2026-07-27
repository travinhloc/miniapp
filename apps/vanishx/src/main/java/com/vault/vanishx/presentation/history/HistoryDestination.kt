package com.vault.vanishx.presentation.history

import com.miniapp.core.mvvm.BaseDestination

sealed class HistoryDestination {
    object History : BaseDestination("history")
}
