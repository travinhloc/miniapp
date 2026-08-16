package com.vault.vanishx.domain.model

/**
 * Free Host clock: [expiresAt] > 0 after guest activate. Waiting (`0`) and Pro Host are live.
 */
object RemoteRoomClock {
    fun isExpired(expiresAt: Long, hostPro: Boolean, nowMs: Long): Boolean {
        if (hostPro || expiresAt <= 0L) return false
        return nowMs >= expiresAt
    }
}
