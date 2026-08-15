package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RefreshRoomMetaUseCaseTest {

    private val mailboxRepository: MailboxRepository = mockk(relaxed = true)
    private val remote = InMemoryMailboxRemoteDataSource()

    @Test
    fun `creator does not treat own creatorPub as peer`() = runTest {
        val local = MailboxRoom(
            id = "r1",
            roomKey = "k",
            role = MailboxRoom.ROLE_CREATOR,
            peerPub = null,
        )
        coEvery { mailboxRepository.getRoom("r1") } returns local
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = 1L,
                expiresAt = 0L,
                creatorPub = "hostPub",
                activatedAt = null,
            ),
        )

        val updated = RefreshRoomMetaUseCase(mailboxRepository, remote)("r1")

        updated?.peerPub shouldBe null
        coVerify(exactly = 0) { mailboxRepository.upsertRoom(any()) }
    }

    @Test
    fun `creator clears peerPub that matches creatorPub`() = runTest {
        val local = MailboxRoom(
            id = "r1",
            roomKey = "k",
            role = MailboxRoom.ROLE_CREATOR,
            peerPub = "hostPub",
        )
        coEvery { mailboxRepository.getRoom("r1") } returns local
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = 1L,
                expiresAt = 0L,
                creatorPub = "hostPub",
                activatedAt = null,
            ),
        )

        val updated = RefreshRoomMetaUseCase(mailboxRepository, remote)("r1")

        updated?.peerPub shouldBe null
        coVerify {
            mailboxRepository.upsertRoom(match { it.peerPub == null })
        }
    }

    @Test
    fun `member copies creatorPub as peer`() = runTest {
        val local = MailboxRoom(
            id = "r1",
            roomKey = "k",
            role = MailboxRoom.ROLE_MEMBER,
            peerPub = null,
        )
        coEvery { mailboxRepository.getRoom("r1") } returns local
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = 1L,
                expiresAt = 0L,
                creatorPub = "hostPub",
                activatedAt = 9L,
            ),
        )

        val updated = RefreshRoomMetaUseCase(mailboxRepository, remote)("r1")

        updated?.peerPub shouldBe "hostPub"
    }
}
