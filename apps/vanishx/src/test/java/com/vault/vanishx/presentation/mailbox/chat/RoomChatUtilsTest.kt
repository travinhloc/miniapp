package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.RecallPolicy
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import java.util.concurrent.TimeUnit

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

    @Test
    fun `timeline inserts today separator`() {
        val now = System.currentTimeMillis()
        val messages = listOf(
            ChatMessage("a", "r", "hi", now - 1_000L, now + 1_000L, ChatMessage.DIRECTION_OUT),
            ChatMessage("b", "r", "yo", now, now + 1_000L, ChatMessage.DIRECTION_IN),
        )
        val timeline = buildRoomTimeline(messages, now)
        timeline.first().shouldBeInstanceOf<RoomTimelineItem.DaySeparator>()
        (timeline.first() as RoomTimelineItem.DaySeparator).kind shouldBe DaySeparatorKind.TODAY
        timeline.count { it is RoomTimelineItem.Message } shouldBe 2
    }

    @Test
    fun `findRecallableMessage allows free within window`() {
        val now = 10_000L
        val messages = listOf(
            ChatMessage("m", "r", "x", now - 100L, now + 1L, ChatMessage.DIRECTION_OUT),
        )
        findRecallableMessage(messages, isPro = false, isExpired = false, isRecalling = false, nowMs = now)
            ?.id shouldBe "m"
        findRecallableMessage(
            messages = listOf(
                ChatMessage(
                    "old",
                    "r",
                    "x",
                    now - RecallPolicy.FREE_WINDOW_MS - 1L,
                    now + 1L,
                    ChatMessage.DIRECTION_OUT,
                ),
            ),
            isPro = false,
            isExpired = false,
            isRecalling = false,
            nowMs = now,
        ) shouldBe null
    }

    @Test
    fun `day separator kind yesterday`() {
        val today = dayStartMs(System.currentTimeMillis())
        val yesterday = today - TimeUnit.DAYS.toMillis(1)
        daySeparatorKind(yesterday, today + 1_000L) shouldBe DaySeparatorKind.YESTERDAY
    }
}
