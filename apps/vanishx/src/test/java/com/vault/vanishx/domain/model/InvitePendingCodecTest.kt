package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Before
import org.junit.Test

class InvitePendingCodecTest {

    private val invite = RoomInvite("room1", "key1", 9_999_999_999_999L)
    private val now = 1_700_000_000_000L

    @Before
    fun setHost() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
    }

    @Test
    fun `https round-trips to canonical`() {
        val https = InviteUriCodec.format(invite)
        InvitePendingCodec.canonicalize(https) shouldBe https
    }

    @Test
    fun `vanishx custom scheme becomes https`() {
        val canonical = InvitePendingCodec.canonicalize("vanishx://r/room1?k=key1&e=1")
        canonical.shouldStartWith("https://vanihx-staging.web.app/join?token=")
        InviteUriCodec.parse(canonical!!) shouldBe RoomInvite("room1", "key1", 1L)
    }

    @Test
    fun `clipboard payload becomes https and drops prefix`() {
        val token = InviteTokenCodec.encode(invite)
        val payload = "VANISHX_INVITE:JOIN_ROOM:$token:${now + 60_000}"
        val canonical = InvitePendingCodec.canonicalize(payload, now)
        canonical.shouldStartWith("https://")
        canonical!!.contains("VANISHX_INVITE") shouldBe false
        InviteUriCodec.parse(canonical) shouldBe invite
    }

    @Test
    fun `open deep link is not an invite`() {
        InvitePendingCodec.canonicalize("vanishx://open/room1") shouldBe null
    }
}
