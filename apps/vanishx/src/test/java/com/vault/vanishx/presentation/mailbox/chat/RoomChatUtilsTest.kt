package com.vault.vanishx.presentation.mailbox.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class RoomChatUtilsTest {

    @Test
    fun `fingerprint groups sha256 hex`() {
        val fp = roomKeyFingerprint("test-room-key")
        fp.split(" ").size shouldBe 8
        fp shouldContain " "
    }

    @Test
    fun `blank key fingerprint is placeholder`() {
        roomKeyFingerprint("") shouldBe "—"
    }

    @Test
    fun `expiry progress and aura threshold`() {
        val now = 1_000_000L
        val activated = now - 20 * 60 * 60 * 1000L
        val expires = now + 2 * 60 * 60 * 1000L // 2h of 22h span left ≈ 9% remaining → progress high
        val progress = roomExpiryProgress(expires, activated, now)
        shouldShowBubbleAura(progress) shouldBe true
        shouldShowBubbleAura(0.5f) shouldBe false
        roomExpiryProgress(0L, null, now) shouldBe 0f
    }
}
