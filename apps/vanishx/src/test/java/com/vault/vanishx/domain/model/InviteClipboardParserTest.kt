package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class InviteClipboardParserTest {

    private val invite = RoomInvite("room1", "key1", 9_999_999_999_999L)
    private val token = InviteTokenCodec.encode(invite)
    private val now = 1_700_000_000_000L

    private fun payload(action: String = "JOIN_ROOM", tok: String = token, exp: Long = now + 60_000) =
        "VANISHX_INVITE:$action:$tok:$exp"

    @Test
    fun `parses valid join payload`() {
        val parsed = InviteClipboardParser.parse(payload(), now)
        parsed.shouldBeInstanceOf<InviteClipboardParser.Result.Valid>()
        (parsed as InviteClipboardParser.Result.Valid).invite shouldBe invite
    }

    @Test
    fun `prefix mismatch is ignore`() {
        InviteClipboardParser.parse("hello world", now) shouldBe InviteClipboardParser.Result.Ignore
        InviteClipboardParser.parse(null, now) shouldBe InviteClipboardParser.Result.Ignore
    }

    @Test
    fun `unknown action is discard`() {
        val parsed = InviteClipboardParser.parse(payload(action = "OPEN_ROOM"), now)
        parsed shouldBe InviteClipboardParser.Result.Discard("unknown_action")
    }

    @Test
    fun `expired payload is discard`() {
        val parsed = InviteClipboardParser.parse(payload(exp = now), now)
        parsed shouldBe InviteClipboardParser.Result.Discard("expired")
    }

    @Test
    fun `malformed after prefix is discard`() {
        InviteClipboardParser.parse("VANISHX_INVITE:not-enough", now)
            .shouldBeInstanceOf<InviteClipboardParser.Result.Discard>()
    }
}
