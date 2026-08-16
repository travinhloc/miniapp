package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.InviteJoinCleanup
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class VerifyInviteUseCaseTest {

    private val remote = InMemoryMailboxRemoteDataSource()
    private val pending: PendingInviteStore = mockk(relaxed = true)
    private val cleanup: InviteJoinCleanup = mockk(relaxed = true)
    private val useCase = VerifyInviteUseCase(remote, pending, cleanup)
    private val invite = RoomInvite("roomVerify", "keyVerify")
    private val now = 1_700_000_000_000L

    @Before
    fun setHost() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
    }

    private suspend fun writeMeta(
        expiresAt: Long = 0L,
        hostPro: Boolean = false,
    ) {
        remote.writeRoomMeta(
            invite.roomId,
            RemoteRoomMeta(
                createdAt = now,
                expiresAt = expiresAt,
                creatorPub = "hostPub",
                hostPro = hostPro,
                icebreaker = "hello",
            ),
        )
    }

    @Test
    fun `live waiting room is Message Request ready`() = runTest {
        writeMeta(expiresAt = 0L)
        val result = useCase(InviteUriCodec.format(invite), now)
        result.shouldBeInstanceOf<VerifyInviteUseCase.Result.Live>()
        (result as VerifyInviteUseCase.Result.Live).meta.icebreaker shouldBe "hello"
        verify(exactly = 0) { cleanup.clearPendingAndClipboard() }
    }

    @Test
    fun `missing meta is JOIN-2 and clears pending`() = runTest {
        val result = useCase(InviteUriCodec.format(invite), now)
        result shouldBe VerifyInviteUseCase.Result.Dead(VerifyInviteUseCase.NOT_FOUND)
        verify { cleanup.clearPendingAndClipboard() }
    }

    @Test
    fun `expired Free clock is JOIN-2 and clears pending`() = runTest {
        writeMeta(expiresAt = now - 1L)
        val result = useCase(InviteUriCodec.format(invite), now)
        result shouldBe VerifyInviteUseCase.Result.Dead(VerifyInviteUseCase.EXPIRED)
        verify { cleanup.clearPendingAndClipboard() }
    }

    @Test
    fun `Pro Host with expiresAt is still live`() = runTest {
        writeMeta(expiresAt = now - 1L, hostPro = true)
        useCase(InviteUriCodec.format(invite), now)
            .shouldBeInstanceOf<VerifyInviteUseCase.Result.Live>()
        verify(exactly = 0) { cleanup.clearPendingAndClipboard() }
    }

    @Test
    fun `garbage typed invite does not clear a different pending`() = runTest {
        io.mockk.every { pending.peek() } returns InviteUriCodec.format(invite)
        val result = useCase("not-an-invite", now)
        result shouldBe VerifyInviteUseCase.Result.Dead(VerifyInviteUseCase.INVALID)
        verify(exactly = 0) { cleanup.clearPendingAndClipboard() }
    }

    @Test
    fun `undecodable pending token clears store`() = runTest {
        val raw = "https://vanihx-staging.web.app/join?token=not-a-token"
        io.mockk.every { pending.peek() } returns raw
        val result = useCase(raw, now)
        result shouldBe VerifyInviteUseCase.Result.Dead(VerifyInviteUseCase.INVALID)
        verify { cleanup.clearPendingAndClipboard() }
    }
}
