package com.vault.vanishx.presentation.mailbox

import com.vault.vanishx.domain.model.MailboxRoom
import io.kotest.matchers.shouldBe
import org.junit.Test

class RoomHandshakeStatusTest {

    private fun room(
        status: String = MailboxRoom.STATUS_ACTIVE,
        role: String = MailboxRoom.ROLE_CREATOR,
        peerPub: String? = null,
    ) = MailboxRoom(
        id = "room1",
        roomKey = "key1",
        status = status,
        role = role,
        peerPub = peerPub,
    )

    @Test
    fun `null room is NONE`() {
        handshakeStatus(room = null, isExpired = false) shouldBe RoomHandshakeStatus.NONE
    }

    @Test
    fun `active room without peerPub is WAITING for creator`() {
        val result = handshakeStatus(
            room = room(role = MailboxRoom.ROLE_CREATOR, peerPub = null),
            isExpired = false,
        )

        result shouldBe RoomHandshakeStatus.WAITING
    }

    @Test
    fun `active room without peerPub is WAITING for member too`() {
        val result = handshakeStatus(
            room = room(role = MailboxRoom.ROLE_MEMBER, peerPub = null),
            isExpired = false,
        )

        result shouldBe RoomHandshakeStatus.WAITING
    }

    @Test
    fun `active room with peerPub is LIVE`() {
        val result = handshakeStatus(
            room = room(peerPub = "peerPubKey"),
            isExpired = false,
        )

        result shouldBe RoomHandshakeStatus.LIVE
    }

    @Test
    fun `expired room is NONE even without peerPub`() {
        val result = handshakeStatus(
            room = room(peerPub = null),
            isExpired = true,
        )

        result shouldBe RoomHandshakeStatus.NONE
    }

    @Test
    fun `left room is NONE`() {
        val result = handshakeStatus(
            room = room(status = MailboxRoom.STATUS_LEFT, peerPub = null),
            isExpired = false,
        )

        result shouldBe RoomHandshakeStatus.NONE
    }
}
