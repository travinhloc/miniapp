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

    fun unlock() {
        isUnlocked = true
    }

    fun lock() {
        isUnlocked = false
    }
}
