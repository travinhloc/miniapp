package com.vault.vanishx.domain.usecase

import io.kotest.matchers.shouldBe
import org.junit.Test

class PingPeerUseCaseTest {

    private val pingPeer = PingPeerUseCase()

    @Test
    fun `first ping is always sent`() {
        val result = pingPeer(lastPingAtMs = 0L, nowMs = 1_000L)

        result.sent shouldBe true
        result.cooldownRemainingMs shouldBe 0L
    }

    @Test
    fun `ping within cooldown window is rejected with remaining time`() {
        val lastPing = 10_000L
        val now = lastPing + 5_000L

        val result = pingPeer(lastPingAtMs = lastPing, nowMs = now)

        result.sent shouldBe false
        result.cooldownRemainingMs shouldBe PingPeerUseCase.COOLDOWN_MS - 5_000L
    }

    @Test
    fun `ping right at cooldown boundary is sent`() {
        val lastPing = 10_000L
        val now = lastPing + PingPeerUseCase.COOLDOWN_MS

        val result = pingPeer(lastPingAtMs = lastPing, nowMs = now)

        result.sent shouldBe true
        result.cooldownRemainingMs shouldBe 0L
    }

    @Test
    fun `ping after cooldown window is sent again`() {
        val lastPing = 10_000L
        val now = lastPing + PingPeerUseCase.COOLDOWN_MS + 1L

        val result = pingPeer(lastPingAtMs = lastPing, nowMs = now)

        result.sent shouldBe true
    }
}
