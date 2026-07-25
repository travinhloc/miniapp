package com.vault.vanishx.presentation

import com.miniapp.core.mvvm.BaseDestination

sealed class AppDestination {

    object RootNavGraph : BaseDestination("rootNavGraph")

    object MainNavGraph : BaseDestination("mainNavGraph")
}
