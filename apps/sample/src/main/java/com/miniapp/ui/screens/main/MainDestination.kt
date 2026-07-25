package com.miniapp.ui.screens.main

import com.miniapp.core.mvvm.BaseDestination

sealed class MainDestination {

    object Home : BaseDestination("home")
}
