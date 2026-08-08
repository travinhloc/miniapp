package com.vault.vanishx.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory unlock gate for the current process. Defaults to locked.
 * Observable so Compose can dismiss the lock UI without relying on ViewModel edge events.
 */
@Singleton
class AppLockSession @Inject constructor() {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlockedFlow: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val isUnlocked: Boolean
        get() = _isUnlocked.value

    /** >0 while an external UI (e.g. QR scanner Activity) should not trigger lock-on-stop. */
    @Volatile
    private var suppressStopLockCount: Int = 0

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun beginExternalUi() {
        suppressStopLockCount += 1
    }

    fun endExternalUi() {
        if (suppressStopLockCount > 0) {
            suppressStopLockCount -= 1
        }
    }

    fun shouldLockOnStop(): Boolean = suppressStopLockCount == 0
}
