package com.vault.vanishx.domain.usecase

import javax.inject.Inject

data class PingPeerResult(
    val sent: Boolean,
    val cooldownRemainingMs: Long,
)

/**
 * Handshake Ping cooldown (story 7.7 / Epic 15.3).
 *
 * Client does not send FCM. After this use case allows a tap,
 * [WritePingSignalUseCase] writes RTDB `/signals` for Cloud Functions fan-out.
 */
class PingPeerUseCase @Inject constructor() {
    operator fun invoke(lastPingAtMs: Long, nowMs: Long = System.currentTimeMillis()): PingPeerResult {
        val elapsed = nowMs - lastPingAtMs
        return if (lastPingAtMs > 0L && elapsed < COOLDOWN_MS) {
            PingPeerResult(sent = false, cooldownRemainingMs = COOLDOWN_MS - elapsed)
        } else {
            PingPeerResult(sent = true, cooldownRemainingMs = 0L)
        }
    }

    companion object {
        const val COOLDOWN_MS = 30_000L
    }
}
