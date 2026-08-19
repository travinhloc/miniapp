package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class IncomingPushPolicyTest {

    private val room = MailboxRoom(
        id = "room1",
        roomKey = "k",
        status = MailboxRoom.STATUS_ACTIVE,
    )

    private fun ctx(
        push: IncomingPush = IncomingPush("room1", IncomingPushType.MESSAGE, "peer"),
        room: MailboxRoom? = this.room,
        myPub: String? = "me",
        foregroundRoomId: String? = null,
        senderBlocked: Boolean = false,
        nowMs: Long = 1_000L,
    ) = IncomingPushContext(
        push = push,
        room = room,
        myPub = myPub,
        foregroundRoomId = foregroundRoomId,
        senderBlocked = senderBlocked,
        nowMs = nowMs,
    )

    @Test
    fun `missing room is dropped`() {
        IncomingPushPolicy.shouldNotify(ctx(room = null)) shouldBe false
    }

    @Test
    fun `muted room is dropped`() {
        IncomingPushPolicy.shouldNotify(ctx(room = room.copy(muted = true))) shouldBe false
    }

    @Test
    fun `left room is dropped`() {
        IncomingPushPolicy.shouldNotify(
            ctx(room = room.copy(status = MailboxRoom.STATUS_LEFT)),
        ) shouldBe false
    }

    @Test
    fun `expired room is dropped`() {
        IncomingPushPolicy.shouldNotify(
            ctx(
                room = room.copy(hostPro = false, expiresAt = 500L, activatedAt = 1L),
                nowMs = 1_000L,
            ),
        ) shouldBe false
    }

    @Test
    fun `self sender is dropped`() {
        IncomingPushPolicy.shouldNotify(
            ctx(push = IncomingPush("room1", IncomingPushType.MESSAGE, "me"), myPub = "me"),
        ) shouldBe false
    }

    @Test
    fun `blocked sender is dropped`() {
        IncomingPushPolicy.shouldNotify(ctx(senderBlocked = true)) shouldBe false
    }

    @Test
    fun `foreground same room is dropped`() {
        IncomingPushPolicy.shouldNotify(ctx(foregroundRoomId = "room1")) shouldBe false
    }

    @Test
    fun `active unmuted background is shown`() {
        IncomingPushPolicy.shouldNotify(ctx()) shouldBe true
    }

    @Test
    fun `parse missing type defaults to message`() {
        IncomingPushParser.parse(mapOf("roomId" to "r1")) shouldBe IncomingPush(
            roomId = "r1",
            type = IncomingPushType.MESSAGE,
            senderPub = null,
        )
    }

    @Test
    fun `parse unknown type is dropped`() {
        IncomingPushParser.parse(mapOf("roomId" to "r1", "type" to "joined")) shouldBe null
    }

    @Test
    fun `parse ping with fromPub`() {
        IncomingPushParser.parse(
            mapOf("roomId" to "r1", "type" to "ping", "fromPub" to "p"),
        ) shouldBe IncomingPush("r1", IncomingPushType.PING, "p")
    }
}
