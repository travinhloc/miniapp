package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomSecretsGenerator
import com.vault.vanishx.data.push.FakeRoomPushTopics
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateJoinRoomUseCaseTest {

    private val mailboxRepository: MailboxRepository = mockk(relaxed = true)
    private val identityRepository: IdentityRepository = mockk()
    private val secrets: RoomSecretsGenerator = mockk()
    private val remote = InMemoryMailboxRemoteDataSource()
    private val pushTopics = FakeRoomPushTopics()

    @Test
    fun `create persists local room and remote meta`() = runTest {
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pub")
        every { secrets.newRoomId() } returns "roomIdFixed____________"
        every { secrets.newRoomKey() } returns "roomKeyFixed"
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val created = CreateRoomUseCase(
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            remote = remote,
            secretsGenerator = secrets,
            roomPushTopics = pushTopics,
        ).invoke(RoomTtlOption.ONE_HOUR)

        created.room.id shouldBe "roomIdFixed____________"
        created.room.role shouldBe MailboxRoom.ROLE_CREATOR
        remote.metaFor(created.room.id) shouldNotBe null
        pushTopics.subscribed shouldBe listOf(created.room.id)
        coVerify { mailboxRepository.upsertRoom(match { it.id == created.room.id }) }
    }

    @Test
    fun `join saves member room from invite`() = runTest {
        coEvery { mailboxRepository.getRoom("r1") } returns null
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val room = JoinRoomUseCase(
            mailboxRepository = mailboxRepository,
            remote = remote,
            roomPushTopics = pushTopics,
        ).invoke(RoomInvite(roomId = "r1", roomKey = "k1", expiresAt = System.currentTimeMillis() + 60_000))

        room.role shouldBe MailboxRoom.ROLE_MEMBER
        room.roomKey shouldBe "k1"
        pushTopics.subscribed shouldBe listOf("r1")
        coVerify { mailboxRepository.upsertRoom(match { it.id == "r1" && it.role == MailboxRoom.ROLE_MEMBER }) }
    }
}
