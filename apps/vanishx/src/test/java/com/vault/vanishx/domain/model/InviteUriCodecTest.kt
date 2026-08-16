package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test

class InviteUriCodecTest {

    private val invite = RoomInvite(
        roomId = "abc123XYZ",
        roomKey = "roomKeyValue",
        expiresAt = 1_700_000_000_000L,
    )

    @Test
    fun `formats https canonical and round-trips`() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
        val uri = InviteUriCodec.format(invite)
        uri.shouldStartWith("https://vanihx-staging.web.app/join?token=")
        InviteUriCodec.parse(uri) shouldBe invite
    }

    @Test
    fun `parses short path with full token`() {
        val token = InviteTokenCodec.encode(invite)
        InviteUriCodec.parse("https://vanihx-staging.web.app/j/$token") shouldBe invite
    }

    @Test
    fun `rejects eight-char display prefix as join path`() {
        val token = InviteTokenCodec.encode(invite)
        val short = InviteTokenCodec.displayPrefix(token)
        InviteUriCodec.parse("https://vanihx-staging.web.app/j/$short") shouldBe null
    }

    @Test
    fun `rejects https urls with raw room key query`() {
        InviteUriCodec.parse(
            "https://vanihx-staging.web.app/join?token=abc&k=secret",
        ) shouldBe null
        InviteUriCodec.parse(
            "https://vanihx-staging.web.app/r/abc123XYZ?k=roomKeyValue",
        ) shouldBe null
    }

    @Test
    fun `display short is host slash j slash eight chars`() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
        val label = InviteUriCodec.displayShort(invite)
        val token = InviteTokenCodec.encode(invite)
        label shouldBe "vanihx-staging.web.app/j/${token.take(8)}"
    }

    @Test
    fun `parses legacy vanishx scheme`() {
        val parsed = InviteUriCodec.parse("vanishx://r/room1?k=key1")
        parsed shouldBe RoomInvite(roomId = "room1", roomKey = "key1", expiresAt = null)
        InviteUriCodec.parse("vanishx://r/abc123XYZ?k=roomKeyValue&e=1700000000000") shouldBe invite
    }

    @Test
    fun `rejects missing key on legacy scheme`() {
        InviteUriCodec.parse("vanishx://r/room1") shouldBe null
    }

    @Test
    fun `token codec rejects wrong version`() {
        val json = "{\"v\":2,\"r\":\"r\",\"k\":\"k\",\"e\":1}"
        val token = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray())
        InviteTokenCodec.decode(token) shouldBe null
    }

    @Test
    fun `room resolvedStatus becomes expired after ttl`() {
        val room = MailboxRoom(
            id = "r",
            roomKey = "k",
            expiresAt = 100L,
            status = MailboxRoom.STATUS_ACTIVE,
        )
        room.resolvedStatus(nowMs = 99L) shouldBe MailboxRoom.STATUS_ACTIVE
        room.resolvedStatus(nowMs = 100L) shouldBe MailboxRoom.STATUS_EXPIRED
        room.resolvedStatus(nowMs = 100L) shouldNotBe room.status
    }
}
