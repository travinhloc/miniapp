package com.vault.vanishx.data.remote

import com.vault.vanishx.domain.usecase.SmokeMailboxRemoteUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InMemoryMailboxRemoteDataSourceTest {

    @Test
    fun `deleteAllMessages clears room mailbox`() = runTest {
        val remote = InMemoryMailboxRemoteDataSource()
        val now = 1_700_000_000_000L
        remote.writeMessage(
            "room1",
            RemoteMailboxMessage(
                messageId = "m1",
                ciphertext = RemoteMailboxMessage.SMOKE_CIPHERTEXT,
                senderPub = "pub",
                createdAt = now,
                expiresAt = now + 60_000,
            ),
        )
        remote.writeMessage(
            "room1",
            RemoteMailboxMessage(
                messageId = "m2",
                ciphertext = RemoteMailboxMessage.SMOKE_CIPHERTEXT,
                senderPub = "pub",
                createdAt = now,
                expiresAt = now + 60_000,
            ),
        )

        remote.deleteAllMessages("room1")
        remote.listMessages("room1").isEmpty() shouldBe true
    }

    @Test
    fun `write read delete message round trip`() = runTest {
        val remote = InMemoryMailboxRemoteDataSource()
        val now = 1_700_000_000_000L
        val message = RemoteMailboxMessage(
            messageId = "m1",
            ciphertext = RemoteMailboxMessage.SMOKE_CIPHERTEXT,
            senderPub = "pub",
            createdAt = now,
            expiresAt = now + 60_000,
        )

        remote.writeMessage("room1", message)
        remote.readMessage("room1", "m1") shouldBe message

        remote.deleteMessage("room1", "m1")
        remote.readMessage("room1", "m1") shouldBe null
    }

    @Test
    fun `rejects oversized ciphertext`() = runTest {
        val remote = InMemoryMailboxRemoteDataSource()
        val now = 1L
        val oversized = "a".repeat(RemoteMailboxMessage.MAX_CIPHERTEXT_LENGTH + 1)

        runCatching {
            remote.writeMessage(
                "room1",
                RemoteMailboxMessage(
                    messageId = "m1",
                    ciphertext = oversized,
                    senderPub = "pub",
                    createdAt = now,
                    expiresAt = now + 1,
                ),
            )
        }.isFailure shouldBe true
    }

    @Test
    fun `smoke use case succeeds against in-memory remote`() = runTest {
        val remote = InMemoryMailboxRemoteDataSource()
        val result = SmokeMailboxRemoteUseCase(remote)()

        result shouldStartWith "ok "
        remote.isAuthenticated() shouldBe true
        remote.metaFor(result.substringAfter("ok ").substringBefore("/")) shouldNotBe null
    }
}
