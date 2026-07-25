package com.vault.vanishx.presentation.home

import com.miniapp.core.mvvm.BaseDestination

sealed class HomeDestination {

    object Home : BaseDestination("home")
}
