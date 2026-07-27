package com.vault.vanishx.presentation.paywall

import com.miniapp.core.mvvm.BaseDestination

sealed class PaywallDestination {
    object Paywall : BaseDestination("paywall")
}
