package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.push.FakeRoomPushTopics
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BlockReportUseCaseTest {

    @Test
    fun `block stores peer and leaves room`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val blockRepository: BlockRepository = mockk(relaxed = true)
        val pushTopics = FakeRoomPushTopics()
        pushTopics.subscribe("room1")
        val room = MailboxRoom(
            id = "room1",
            roomKey = "k",
            peerPub = "peerPub",
            status = MailboxRoom.STATUS_ACTIVE,
        )
        coEvery { mailboxRepository.getRoom("room1") } returns room
        coEvery { mailboxRepository.deleteMessagesForRoom("room1") } returns 3

        val result = BlockPeerUseCase(mailboxRepository, blockRepository, pushTopics).invoke("room1")

        result.peerPub shouldBe "peerPub"
        coVerify { blockRepository.block(eq("peerPub"), any()) }
        coVerify {
            mailboxRepository.upsertRoom(
                match { it.status == MailboxRoom.STATUS_LEFT && it.peerPub == "peerPub" },
            )
        }
        pushTopics.unsubscribed shouldBe listOf("room1")
    }

    @Test
    fun `block fails when peer unknown`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        coEvery { mailboxRepository.getRoom("room1") } returns MailboxRoom(
            id = "room1",
            roomKey = "k",
            peerPub = null,
        )

        shouldThrow<IllegalStateException> {
            BlockPeerUseCase(mailboxRepository, mockk(), FakeRoomPushTopics()).invoke("room1")
        }
    }

    @Test
    fun `join rejects blocked creatorPub`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val blockRepository: BlockRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        coEvery { mailboxRepository.getRoom("r1") } returns null
        coEvery { blockRepository.isBlocked("badPeer") } returns true
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = 1L,
                expiresAt = System.currentTimeMillis() + 60_000,
                creatorPub = "badPeer",
            ),
        )

        shouldThrow<IllegalStateException> {
            JoinRoomUseCase(
                mailboxRepository,
                remote,
                FakeRoomPushTopics(),
                blockRepository,
            ).invoke(RoomInvite(roomId = "r1", roomKey = "k1"))
        }
    }

    @Test
    fun `join stores creatorPub as peerPub`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val blockRepository: BlockRepository = mockk(relaxed = true)
        val remote = InMemoryMailboxRemoteDataSource()
        coEvery { mailboxRepository.getRoom("r1") } returns null
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = 1L,
                expiresAt = System.currentTimeMillis() + 60_000,
                creatorPub = "creatorPub",
            ),
        )

        val room = JoinRoomUseCase(
            mailboxRepository,
            remote,
            FakeRoomPushTopics(),
            blockRepository,
        ).invoke(RoomInvite(roomId = "r1", roomKey = "k1"))

        room.peerPub shouldBe "creatorPub"
        coVerify { mailboxRepository.upsertRoom(match { it.peerPub == "creatorPub" }) }
    }

    @Test
    fun `report writes remote node`() = runTest {
        val mailboxRepository: MailboxRepository = mockk(relaxed = true)
        val identityRepository: IdentityRepository = mockk()
        val remote = InMemoryMailboxRemoteDataSource()
        coEvery { mailboxRepository.getRoom("room1") } returns MailboxRoom(
            id = "room1",
            roomKey = "k",
            peerPub = "peer",
        )
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_me", "mePub")

        val result = ReportRoomUseCase(mailboxRepository, identityRepository, remote)
            .invoke("room1", "spam")

        result.reportId.isNotBlank() shouldBe true
        val stored = remote.reportFor(result.reportId)
        stored shouldNotBe null
        stored!!.roomId shouldBe "room1"
        stored.reporterPub shouldBe "mePub"
        stored.peerPub shouldBe "peer"
        stored.reason shouldBe "spam"
    }
}
