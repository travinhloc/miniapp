package com.vault.vanishx.domain.usecase

import javax.inject.Inject

data class PingPeerResult(
    val sent: Boolean,
    val cooldownRemainingMs: Long,
)

/**
 * Handshake nudge stub (story 7.7).
 *
 * There is no client-triggered push to a specific peer device yet — fan-out send stays
 * out of band (Console / Functions), same as [com.vault.vanishx.data.push.RoomPushTopics].
 * Until that lands, "Ping" is a best-effort, in-app notify stub: this use case only
 * rate-limits repeated taps so the caller can surface a simple cooldown message.
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
