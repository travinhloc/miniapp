package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.ChatMessage
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

        val result = RecallRoomMessageUseCase(mailboxRepository, pro, remote)
            .invoke("room1", "m1")

        result.message.recalled shouldBe true
        result.message.body shouldBe ""
        result.remoteRemoved shouldBe true
        remote.listMessages("room1").isEmpty() shouldBe true
        coVerify {
            mailboxRepository.upsertMessage(match { it.recalled && it.body.isEmpty() })
        }
    }

    @Test
    fun `free cannot recall`() = runTest {
        val pro: ProEntitlementRepository = mockk()
        every { pro.isProNow() } returns false

        shouldThrow<IllegalStateException> {
            RecallRoomMessageUseCase(mockk(), pro, InMemoryMailboxRemoteDataSource())
                .invoke("room1", "m1")
        }
    }
}
