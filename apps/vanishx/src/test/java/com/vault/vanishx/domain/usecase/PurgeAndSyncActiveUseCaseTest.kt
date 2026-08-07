package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomMessageCipher
import com.vault.vanishx.data.push.FakeRoomPushTopics
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteMailboxMessage
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.matchers.shouldBe
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
class PurgeAndSyncActiveUseCaseTest {

    private val roomKey = randomRoomKey()

    @Test
    fun `purge marks expired clears local and remote without decrypt`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val remote = InMemoryMailboxRemoteDataSource()
        val room = MailboxRoom(
            id = "room1",
            roomKey = roomKey,
            createdAt = 1L,
            expiresAt = 1L,
            status = MailboxRoom.STATUS_ACTIVE,
        )
        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.deleteMessagesForRoom("room1") } returns 2

        remote.writeMessage(
            "room1",
            RemoteMailboxMessage(
                messageId = "m1",
                ciphertext = "vx1.cipher",
                senderPub = "pub",
                createdAt = 1L,
                expiresAt = 1L,
            ),
        )

        val result = PurgeExpiredRoomUseCase(mailboxRepository, remote, FakeRoomPushTopics()).invoke("room1")

        result.localDeleted shouldBe 0
        result.remotePurged shouldBe true
        remote.listMessages("room1").isEmpty() shouldBe true
        coVerify {
            mailboxRepository.upsertRoom(
                match { it.status == MailboxRoom.STATUS_EXPIRED },
            )
        }
        coVerify(exactly = 0) { mailboxRepository.deleteMessagesForRoom(any()) }
    }

    @Test
    fun `sync expired room purges instead of decrypting`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        val cipher = RoomMessageCipher()
        val expiredRoom = MailboxRoom(
            id = "room1",
            roomKey = roomKey,
            createdAt = 1L,
            expiresAt = 1L,
            status = MailboxRoom.STATUS_ACTIVE,
        )
        coEvery { mailboxRepository.getRoom("room1") } returns expiredRoom
        coEvery { mailboxRepository.deleteMessagesForRoom("room1") } returns 1
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx", "pub")

        val wire = cipher.encrypt("room1", roomKey, "secret")
        remote.writeMessage(
            "room1",
            RemoteMailboxMessage(
                messageId = "m1",
                ciphertext = wire,
                senderPub = "peer",
                createdAt = 1L,
                expiresAt = System.currentTimeMillis() + 60_000,
            ),
        )

        val purge = PurgeExpiredRoomUseCase(mailboxRepository, remote, FakeRoomPushTopics())
        val result = SyncRoomMailboxUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            cipher = cipher,
            purgeExpiredRoom = purge,
            blockRepository = mockk(relaxed = true),
            refreshRoomMeta = RefreshRoomMetaUseCase(mailboxRepository, remote),
        ).invoke("room1")

        result.ingested shouldBe 0
        result.messages.isEmpty() shouldBe true
        remote.listMessages("room1").isEmpty() shouldBe true
        coVerify(exactly = 0) { mailboxRepository.upsertMessage(any()) }
    }

    @Test
    fun `sync active mailboxes purges expired and syncs live rooms`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val syncRoomMailbox: SyncRoomMailboxUseCase = mockk()
        val purgeExpiredRoom: PurgeExpiredRoomUseCase = mockk(relaxed = true)
        val now = System.currentTimeMillis()
        val live = MailboxRoom(
            id = "live",
            roomKey = roomKey,
            expiresAt = now + 60_000,
            status = MailboxRoom.STATUS_ACTIVE,
        )
        val dead = MailboxRoom(
            id = "dead",
            roomKey = roomKey,
            expiresAt = now - 1_000,
            status = MailboxRoom.STATUS_ACTIVE,
        )
        coEvery { mailboxRepository.getActiveRooms() } returnsMany listOf(
            listOf(live, dead),
            listOf(live),
        )
        coEvery { syncRoomMailbox("live") } returns SyncMailboxResult(
            messages = emptyList(),
            ingested = 0,
            removedRemote = 0,
            decryptFailures = 0,
        )
        coEvery { purgeExpiredRoom("dead") } returns PurgeExpiredRoomResult("dead", 0, true)

        val result = SyncActiveMailboxesUseCase(
            mailboxRepository = mailboxRepository,
            syncRoomMailbox = syncRoomMailbox,
            purgeExpiredRoom = purgeExpiredRoom,
        ).invoke()

        result.purgedCount shouldBe 1
        result.syncedCount shouldBe 1
        result.activeCount shouldBe 1
        coVerify { purgeExpiredRoom("dead") }
        coVerify { syncRoomMailbox("live") }
    }

    @Test
    fun `sync active mailboxes soft-fails network errors`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val syncRoomMailbox: SyncRoomMailboxUseCase = mockk()
        val purgeExpiredRoom: PurgeExpiredRoomUseCase = mockk()
        val live = MailboxRoom(
            id = "live",
            roomKey = roomKey,
            expiresAt = System.currentTimeMillis() + 60_000,
        )
        coEvery { mailboxRepository.getActiveRooms() } returns listOf(live)
        coEvery { syncRoomMailbox("live") } throws IllegalStateException("network down")

        val result = SyncActiveMailboxesUseCase(
            mailboxRepository,
            syncRoomMailbox,
            purgeExpiredRoom,
        ).invoke()

        result.syncFailures shouldBe 1
        result.syncedCount shouldBe 0
    }

    private fun randomRoomKey(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
