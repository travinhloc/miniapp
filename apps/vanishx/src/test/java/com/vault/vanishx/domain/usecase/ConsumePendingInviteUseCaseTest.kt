package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ConsumePendingInviteUseCaseTest {

    private val store: PendingInviteStore = mockk(relaxed = true)
    private val useCase = ConsumePendingInviteUseCase(store)

    @Before
    fun setHost() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
    }

    @Test
    fun `captureIfInvite stores vanishx as https canonical`() {
        val invite = RoomInvite("room1", "key1", 1L)
        useCase.captureIfInvite("vanishx://r/room1?k=key1&e=1") shouldBe true
        verify { store.save(InviteUriCodec.format(invite)) }
    }

    @Test
    fun `captureIfInvite stores https canonical`() {
        val https = RoomInvite("room1", "key1", 1L).toUriString()
        useCase.captureIfInvite(https) shouldBe true
        verify { store.save(https) }
    }

    @Test
    fun `captureIfInvite ignores open deep link`() {
        useCase.captureIfInvite("vanishx://open/room1") shouldBe false
        verify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `topic name is stable`() {
        RoomPushTopics.topicFor("abc") shouldBe "vx_room_abc"
    }
}
