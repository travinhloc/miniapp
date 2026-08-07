package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.RoomSecretsGenerator
import com.vault.vanishx.data.push.FakeRoomPushTopics
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import io.kotest.matchers.longs.shouldBeGreaterThan
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
    private val proEntitlement: ProEntitlementRepository = mockk()

    private fun createUseCase() = CreateRoomUseCase(
        mailboxRepository = mailboxRepository,
        identityRepository = identityRepository,
        remote = remote,
        secretsGenerator = secrets,
        roomPushTopics = pushTopics,
        proEntitlement = proEntitlement,
    )

    private fun joinUseCase() = JoinRoomUseCase(
        mailboxRepository = mailboxRepository,
        remote = remote,
        roomPushTopics = pushTopics,
        blockRepository = mockk(relaxed = true),
    )

    @Test
    fun `create Free Host leaves expiresAt zero until guest enter`() = runTest {
        every { proEntitlement.isProNow() } returns false
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pub")
        every { secrets.newRoomId() } returns "roomIdFixed____________"
        every { secrets.newRoomKey() } returns "roomKeyFixed"
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val created = createUseCase().invoke(RoomTtlOption.ONE_DAY)

        created.room.expiresAt shouldBe 0L
        created.room.hostPro shouldBe false
        created.room.activatedAt shouldBe 0L
        remote.metaFor(created.room.id)?.expiresAt shouldBe 0L
        remote.metaFor(created.room.id)?.hostPro shouldBe false
        created.invite.expiresAt shouldBe null
    }

    @Test
    fun `create Pro Host has no room clock`() = runTest {
        every { proEntitlement.isProNow() } returns true
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pub")
        every { secrets.newRoomId() } returns "roomIdFixed____________"
        every { secrets.newRoomKey() } returns "roomKeyFixed"
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val created = createUseCase().invoke(RoomTtlOption.ONE_DAY)

        created.room.hostPro shouldBe true
        created.room.expiresAt shouldBe 0L
        remote.metaFor(created.room.id)?.hostPro shouldBe true
    }

    @Test
    fun `join Free Host activates 24h clock`() = runTest {
        coEvery { mailboxRepository.getRoom("r1") } returns null
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit
        val now = System.currentTimeMillis()
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = now,
                expiresAt = 0L,
                creatorPub = "hostPub",
                hostPro = false,
            ),
        )

        val room = joinUseCase().invoke(RoomInvite(roomId = "r1", roomKey = "k1"))

        room.role shouldBe MailboxRoom.ROLE_MEMBER
        room.activatedAt shouldBeGreaterThan 0L
        room.expiresAt shouldBeGreaterThan now
        room.hostPro shouldBe false
        remote.metaFor("r1")?.activatedAt shouldNotBe null
        pushTopics.subscribed shouldBe listOf("r1")
    }

    @Test
    fun `join Pro Host activates without expiresAt`() = runTest {
        coEvery { mailboxRepository.getRoom("r2") } returns null
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit
        val now = System.currentTimeMillis()
        remote.writeRoomMeta(
            "r2",
            RemoteRoomMeta(
                createdAt = now,
                expiresAt = 0L,
                creatorPub = "hostPub",
                hostPro = true,
                icebreaker = "Hi there — still interested?",
            ),
        )

        val room = joinUseCase().invoke(RoomInvite(roomId = "r2", roomKey = "k2"))

        room.icebreaker shouldBe "Hi there — still interested?"
        room.peerPub shouldBe "hostPub"
        room.hostPro shouldBe true
        room.activatedAt shouldBeGreaterThan 0L
        room.expiresAt shouldBe 0L
        remote.metaFor("r2")?.expiresAt shouldBe 0L
    }

    @Test
    fun `create persists local room and remote meta`() = runTest {
        every { proEntitlement.isProNow() } returns false
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_a", "pub")
        every { secrets.newRoomId() } returns "roomIdFixed____________"
        every { secrets.newRoomKey() } returns "roomKeyFixed"
        coEvery { mailboxRepository.upsertRoom(any()) } returns Unit

        val created = createUseCase().invoke(RoomTtlOption.ONE_HOUR)

        created.room.id shouldBe "roomIdFixed____________"
        created.room.role shouldBe MailboxRoom.ROLE_CREATOR
        remote.metaFor(created.room.id) shouldNotBe null
        pushTopics.subscribed shouldBe listOf(created.room.id)
        coVerify { mailboxRepository.upsertRoom(match { it.id == created.room.id }) }
    }
}
