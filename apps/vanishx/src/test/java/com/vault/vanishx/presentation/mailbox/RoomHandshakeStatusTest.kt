package com.vault.vanishx.presentation.mailbox

import com.vault.vanishx.domain.model.MailboxRoom
import io.kotest.matchers.shouldBe
import org.junit.Test

class RoomHandshakeStatusTest {

    private fun room(
        status: String = MailboxRoom.STATUS_ACTIVE,
        role: String = MailboxRoom.ROLE_CREATOR,
        peerPub: String? = null,
        activatedAt: Long = 0L,
    ) = MailboxRoom(
        id = "room1",
        roomKey = "key1",
        status = status,
        role = role,
        peerPub = peerPub,
        activatedAt = activatedAt,
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
    fun `creator with own pub stored as peer is still WAITING until activate`() {
        val result = handshakeStatus(
            room = room(peerPub = "creatorPub"),
            isExpired = false,
        )

        result shouldBe RoomHandshakeStatus.WAITING
    }

    @Test
    fun `active room with peerPub is LIVE after guest enter`() {
        val result = handshakeStatus(
            room = room(peerPub = "peerPubKey", activatedAt = 1_700_000_000_000L),
            isExpired = false,
        )

        result shouldBe RoomHandshakeStatus.LIVE
    }

    @Test
    fun `active room with activatedAt is LIVE without peerPub`() {
        val result = handshakeStatus(
            room = room(peerPub = null, activatedAt = 1_700_000_000_000L),
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
