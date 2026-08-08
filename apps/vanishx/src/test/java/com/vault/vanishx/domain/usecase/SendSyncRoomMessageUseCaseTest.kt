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
        activatedAt = 1L,
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
    fun `send sensitive encodes envelope then stores plain local body`() = runTest {
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
        ).invoke("room1", "hidden note", sensitive = true)

        sent.body shouldBe "hidden note"
        sent.sensitive shouldBe true
        val remoteMsg = remote.listMessages("room1").single()
        val decrypted = cipher.decrypt("room1", roomKey, remoteMsg.ciphertext)
        decrypted.startsWith("{\"v\":1,") shouldBe true
        coVerify {
            mailboxRepository.upsertMessage(match { it.sensitive && it.body == "hidden note" })
        }
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
            purgeExpiredRoom = PurgeExpiredRoomUseCase(
                mailboxRepository,
                remote,
                com.vault.vanishx.data.push.FakeRoomPushTopics(),
            ),
            blockRepository = mockk(relaxed = true),
            refreshRoomMeta = RefreshRoomMetaUseCase(mailboxRepository, remote),
        ).invoke("room1")

        result.ingested shouldBe 1
        remote.listMessages("room1").isEmpty() shouldBe true
        coVerify {
            mailboxRepository.upsertMessage(
                match { it.body == "from peer" && it.direction == ChatMessage.DIRECTION_IN },
            )
        }
    }

    @Test
    fun `sync decodes sensitive envelope from peer`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        val cipher = RoomMessageCipher()

        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.getMessage(any()) } returns null
        coEvery { mailboxRepository.getMessages("room1") } returns emptyList()
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_me", "pubMe")
        coEvery { mailboxRepository.deleteExpiredMessages(any()) } returns 0

        val wire = cipher.encrypt(
            "room1",
            roomKey,
            com.vault.vanishx.domain.model.MessagePlaintextCodec.encode("peek", sensitive = true),
        )
        remote.writeMessage(
            "room1",
            com.vault.vanishx.data.remote.RemoteMailboxMessage(
                messageId = "m2",
                ciphertext = wire,
                senderPub = "pubPeer",
                createdAt = 3L,
                expiresAt = room.expiresAt,
            ),
        )

        SyncRoomMailboxUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            cipher = cipher,
            purgeExpiredRoom = PurgeExpiredRoomUseCase(
                mailboxRepository,
                remote,
                com.vault.vanishx.data.push.FakeRoomPushTopics(),
            ),
            blockRepository = mockk(relaxed = true),
            refreshRoomMeta = RefreshRoomMetaUseCase(mailboxRepository, remote),
        ).invoke("room1")

        coVerify {
            mailboxRepository.upsertMessage(
                match { it.body == "peek" && it.sensitive && it.direction == ChatMessage.DIRECTION_IN },
            )
        }
    }

    @Test
    fun `sender sync keeps own outbound on remote for offline peer`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        val cipher = RoomMessageCipher()

        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.getMessage("m-out") } returns ChatMessage(
            id = "m-out",
            roomId = "room1",
            body = "hello offline",
            sentAt = 2L,
            expiresAt = room.expiresAt,
            direction = ChatMessage.DIRECTION_OUT,
        )
        coEvery { mailboxRepository.getMessages("room1") } returns emptyList()
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pubA")
        coEvery { mailboxRepository.deleteExpiredMessages(any()) } returns 0

        val wire = cipher.encrypt("room1", roomKey, "hello offline")
        remote.writeMessage(
            "room1",
            com.vault.vanishx.data.remote.RemoteMailboxMessage(
                messageId = "m-out",
                ciphertext = wire,
                senderPub = "pubA",
                createdAt = 2L,
                expiresAt = room.expiresAt,
            ),
        )

        val result = SyncRoomMailboxUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            cipher = cipher,
            purgeExpiredRoom = PurgeExpiredRoomUseCase(
                mailboxRepository,
                remote,
                com.vault.vanishx.data.push.FakeRoomPushTopics(),
            ),
            blockRepository = mockk(relaxed = true),
            refreshRoomMeta = RefreshRoomMetaUseCase(mailboxRepository, remote),
        ).invoke("room1")

        result.removedRemote shouldBe 0
        remote.listMessages("room1").single().messageId shouldBe "m-out"
    }

    @Test
    fun `sender observe race keeps own outbound on remote`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        val cipher = RoomMessageCipher()

        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.getMessage(any()) } returns null
        coEvery { mailboxRepository.getMessages("room1") } returns emptyList()
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pubA")
        coEvery { mailboxRepository.deleteExpiredMessages(any()) } returns 0

        val wire = cipher.encrypt("room1", roomKey, "race")
        val remoteMsg = com.vault.vanishx.data.remote.RemoteMailboxMessage(
            messageId = "m-race",
            ciphertext = wire,
            senderPub = "pubA",
            createdAt = 2L,
            expiresAt = room.expiresAt,
        )
        remote.writeMessage("room1", remoteMsg)

        val sync = SyncRoomMailboxUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            cipher = cipher,
            purgeExpiredRoom = PurgeExpiredRoomUseCase(
                mailboxRepository,
                remote,
                com.vault.vanishx.data.push.FakeRoomPushTopics(),
            ),
            blockRepository = mockk(relaxed = true),
            refreshRoomMeta = RefreshRoomMetaUseCase(mailboxRepository, remote),
        )
        val result = sync.ingestRemoteList("room1", listOf(remoteMsg))

        result.ingested shouldBe 1
        result.removedRemote shouldBe 0
        remote.listMessages("room1").single().messageId shouldBe "m-race"
        coVerify {
            mailboxRepository.upsertMessage(
                match { it.id == "m-race" && it.direction == ChatMessage.DIRECTION_OUT },
            )
        }
    }

    private fun randomRoomKey(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
