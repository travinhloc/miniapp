package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.push.FakeRoomPushTopics
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.data.push.RoomPushTopics
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConsumePendingInviteUseCaseTest {

    @Test
    fun `captureIfInvite stores valid vanishx uri`() {
        val store: PendingInviteStore = mockk(relaxed = true)
        val join: JoinRoomUseCase = mockk()
        val useCase = ConsumePendingInviteUseCase(store, join)

        useCase.captureIfInvite("vanishx://r/room1?k=key1&e=1") shouldBe true
        verify { store.save("vanishx://r/room1?k=key1&e=1") }
    }

    @Test
    fun `captureIfInvite ignores open deep link`() {
        val store: PendingInviteStore = mockk(relaxed = true)
        val useCase = ConsumePendingInviteUseCase(store, mockk())

        useCase.captureIfInvite("vanishx://open/room1") shouldBe false
        verify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `invoke joins and returns room`() = runTest {
        val store: PendingInviteStore = mockk(relaxed = true)
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val remote = InMemoryMailboxRemoteDataSource()
        val pushTopics = FakeRoomPushTopics()
        every { store.consume() } returns "vanishx://r/room1?k=key1&e=${System.currentTimeMillis() + 60_000}"
        coEvery { mailboxRepository.getRoom("room1") } returns null
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit
        remote.writeRoomMeta(
            "room1",
            RemoteRoomMeta(
                createdAt = System.currentTimeMillis(),
                expiresAt = 0L,
                creatorPub = "hostPub",
                hostPro = false,
            ),
        )

        val join = JoinRoomUseCase(mailboxRepository, remote, pushTopics, mockk(relaxed = true))
        val room = ConsumePendingInviteUseCase(store, join).invoke()

        room?.id shouldBe "room1"
        pushTopics.subscribed shouldBe listOf("room1")
    }

    @Test
    fun `topic name is stable`() {
        RoomPushTopics.topicFor("abc") shouldBe "vx_room_abc"
    }
}
