package com.vault.vanishx.data.billing

import android.content.Context
import android.content.SharedPreferences
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Pro stub (story 4.2). Replace with RevenueCat / Play Billing later.
 */
@Singleton
class StubProEntitlementRepository @Inject constructor(
    @ApplicationContext context: Context,
) : ProEntitlementRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_PRO_STUB, false))
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    override fun isProNow(): Boolean = _isPro.value

    override fun setProStub(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRO_STUB, enabled).apply()
        _isPro.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "vanishx_pro_stub_prefs"
        const val KEY_PRO_STUB = "pro_stub_enabled"
    }
}
