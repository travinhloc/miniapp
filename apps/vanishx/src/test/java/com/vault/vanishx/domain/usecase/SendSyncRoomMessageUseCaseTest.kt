package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomMessageCipher
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.util.Base64
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SendSyncRoomMessageUseCaseTest {

    private val roomKey = randomRoomKey()
    private val room = MailboxRoom(
        id = "room1",
        roomKey = roomKey,
        createdAt = 1L,
        expiresAt = System.currentTimeMillis() + 3_600_000L,
        role = MailboxRoom.ROLE_CREATOR,
    )

    @Test
    fun `send writes ciphertext remote and plaintext local`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        val cipher = RoomMessageCipher()

        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pubA")

        val sent = SendRoomMessageUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            cipher = cipher,
        ).invoke("room1", "arrow text")

        sent.body shouldBe "arrow text"
        sent.direction shouldBe ChatMessage.DIRECTION_OUT
        val remoteMsg = remote.listMessages("room1").single()
        remoteMsg.ciphertext shouldStartWith "vx1."
        remoteMsg.ciphertext.contains("arrow text") shouldBe false
        coVerify { mailboxRepository.upsertMessage(match { it.body == "arrow text" }) }
    }

    @Test
    fun `sync decrypts peer message then removes remote`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        val cipher = RoomMessageCipher()

        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.getMessage(any()) } returns null
        coEvery { mailboxRepository.getMessages("room1") } returns emptyList() andThen listOf(
            ChatMessage(
                id = "m1",
                roomId = "room1",
                body = "from peer",
                sentAt = 2L,
                expiresAt = room.expiresAt,
                direction = ChatMessage.DIRECTION_IN,
            ),
        )
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_me", "pubMe")
        coEvery { mailboxRepository.deleteExpiredMessages(any()) } returns 0

        val wire = cipher.encrypt("room1", roomKey, "from peer")
        remote.writeMessage(
            "room1",
            com.vault.vanishx.data.remote.RemoteMailboxMessage(
                messageId = "m1",
                ciphertext = wire,
                senderPub = "pubPeer",
                createdAt = 2L,
                expiresAt = room.expiresAt,
            ),
        )

        val result = SyncRoomMailboxUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            cipher = cipher,
        ).invoke("room1")

        result.ingested shouldBe 1
        remote.listMessages("room1").isEmpty() shouldBe true
        coVerify {
            mailboxRepository.upsertMessage(
                match { it.body == "from peer" && it.direction == ChatMessage.DIRECTION_IN },
            )
        }
    }

    private fun randomRoomKey(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
