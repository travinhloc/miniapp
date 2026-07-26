package com.vault.vanishx.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Pro entitlement gate. MVP uses a local stub until real IAP (RevenueCat) lands.
 */
interface ProEntitlementRepository {
    val isPro: StateFlow<Boolean>

    fun isProNow(): Boolean

    /** Staging/debug stub only — no-op / ignored when stub UI is disabled. */
    fun setProStub(enabled: Boolean)
}
