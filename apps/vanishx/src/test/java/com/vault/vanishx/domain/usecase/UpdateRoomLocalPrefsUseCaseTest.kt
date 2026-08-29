package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.FakeRoomPushTopics
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateRoomLocalPrefsUseCaseTest {

    private val mailboxRepository: MailboxRepository = mockk()
    private val pushTopics = FakeRoomPushTopics()
    private val useCase = UpdateRoomLocalPrefsUseCase(mailboxRepository, pushTopics)

    private val room = MailboxRoom(id = "room1", roomKey = "k")

    @Test
    fun `mute unsubscribes FCM topic`() = runTest {
        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val updated = useCase.setMuted("room1", muted = true)

        updated.muted shouldBe true
        pushTopics.unsubscribed shouldBe listOf("room1")
        pushTopics.subscribed.isEmpty() shouldBe true
        coVerify { mailboxRepository.upsertRoom(match { it.muted }) }
    }

    @Test
    fun `unmute subscribes FCM topic`() = runTest {
        coEvery { mailboxRepository.getRoom("room1") } returns room.copy(muted = true)
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val updated = useCase.setMuted("room1", muted = false)

        updated.muted shouldBe false
        pushTopics.resubscribed shouldBe listOf("room1")
        pushTopics.subscribed shouldBe listOf("room1")
    }
}
