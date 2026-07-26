package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class InviteUriCodecTest {

    @Test
    fun `formats and parses invite with expiry`() {
        val invite = RoomInvite(
            roomId = "abc123XYZ",
            roomKey = "roomKeyValue",
            expiresAt = 1_700_000_000_000L,
        )
        val uri = InviteUriCodec.format(invite)
        uri shouldBe "vanishx://r/abc123XYZ?k=roomKeyValue&e=1700000000000"
        InviteUriCodec.parse(uri) shouldBe invite
    }

    @Test
    fun `parses invite without expiry`() {
        val parsed = InviteUriCodec.parse("vanishx://r/room1?k=key1")
        parsed shouldBe RoomInvite(roomId = "room1", roomKey = "key1", expiresAt = null)
    }

    @Test
    fun `rejects missing key`() {
        InviteUriCodec.parse("vanishx://r/room1") shouldBe null
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
