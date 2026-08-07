package com.vault.vanishx.data.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory unlock gate for the current process. Defaults to locked.
 */
@Singleton
class AppLockSession @Inject constructor() {
    @Volatile
    var isUnlocked: Boolean = false
        private set

    /** >0 while an external UI (e.g. QR scanner Activity) should not trigger lock-on-stop. */
    @Volatile
    private var suppressStopLockCount: Int = 0

    fun unlock() {
        isUnlocked = true
    }

    fun lock() {
        isUnlocked = false
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
