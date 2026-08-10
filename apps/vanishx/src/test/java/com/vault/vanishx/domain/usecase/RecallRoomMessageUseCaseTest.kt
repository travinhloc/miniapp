package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.InMemoryMediaStorageRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.RecallPolicy
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecallRoomMessageUseCaseTest {

    @Test
    fun `pro recalls outbound and deletes remote`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val pro: ProEntitlementRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        every { pro.isProNow() } returns true
        every { pro.isPro } returns MutableStateFlow(true)
        val message = ChatMessage(
            id = "m1",
            roomId = "room1",
            body = "secret",
            sentAt = 1L,
            expiresAt = 2L,
            direction = ChatMessage.DIRECTION_OUT,
        )
        coEvery { mailboxRepository.getMessage("m1") } returns message
        remote.writeMessage(
            "room1",
            RemoteMailboxMessage(
                messageId = "m1",
                ciphertext = "vx1.cipher",
                senderPub = "me",
                createdAt = 1L,
                expiresAt = 2L,
            ),
        )

        val result = RecallRoomMessageUseCase(
            mailboxRepository,
            pro,
            remote,
            mediaRemote = InMemoryMediaStorageRemoteDataSource(),
        ).invoke("room1", "m1")

        result.message.recalled shouldBe true
        result.message.body shouldBe ""
        result.remoteRemoved shouldBe true
        remote.listMessages("room1").isEmpty() shouldBe true
        coVerify {
            mailboxRepository.upsertMessage(match { it.recalled && it.body.isEmpty() })
        }
    }

    @Test
    fun `free recalls within 24h`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val pro: ProEntitlementRepository = mockk()
        every { pro.isProNow() } returns false
        val now = 1_000_000L
        val message = ChatMessage(
            id = "m1",
            roomId = "room1",
            body = "fresh",
            sentAt = now - 1_000L,
            expiresAt = now + 1_000L,
            direction = ChatMessage.DIRECTION_OUT,
        )
        coEvery { mailboxRepository.getMessage("m1") } returns message

        val result = RecallRoomMessageUseCase(
            mailboxRepository,
            pro,
            InMemoryMailboxRemoteDataSource(),
            mediaRemote = InMemoryMediaStorageRemoteDataSource(),
        ).invoke("room1", "m1", nowMs = now)

        result.message.recalled shouldBe true
    }

    @Test
    fun `free cannot recall after 24h`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val pro: ProEntitlementRepository = mockk()
        every { pro.isProNow() } returns false
        val now = RecallPolicy.FREE_WINDOW_MS + 10_000L
        val message = ChatMessage(
            id = "m1",
            roomId = "room1",
            body = "old",
            sentAt = 1L,
            expiresAt = now + 1L,
            direction = ChatMessage.DIRECTION_OUT,
        )
        coEvery { mailboxRepository.getMessage("m1") } returns message

        shouldThrow<IllegalStateException> {
            RecallRoomMessageUseCase(
                mailboxRepository,
                pro,
                InMemoryMailboxRemoteDataSource(),
                mediaRemote = InMemoryMediaStorageRemoteDataSource(),
            ).invoke("room1", "m1", nowMs = now)
        }
    }
}
