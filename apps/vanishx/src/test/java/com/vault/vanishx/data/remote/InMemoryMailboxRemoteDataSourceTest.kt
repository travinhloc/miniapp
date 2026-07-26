package com.vault.vanishx.data.remote

import io.kotest.matchers.shouldBe
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
                ciphertext = SAMPLE_CIPHERTEXT,
                senderPub = "pub",
                createdAt = now,
                expiresAt = now + 60_000,
            ),
        )
        remote.writeMessage(
            "room1",
            RemoteMailboxMessage(
                messageId = "m2",
                ciphertext = SAMPLE_CIPHERTEXT,
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
            ciphertext = SAMPLE_CIPHERTEXT,
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

    private companion object {
        const val SAMPLE_CIPHERTEXT = "dGVzdA=="
    }
}
