package com.vault.vanishx.presentation.security

import com.miniapp.core.mvvm.BaseDestination

sealed class SecurityDestination {
    object Lock : BaseDestination("security/lock")
    object Settings : BaseDestination("security/settings")
}
