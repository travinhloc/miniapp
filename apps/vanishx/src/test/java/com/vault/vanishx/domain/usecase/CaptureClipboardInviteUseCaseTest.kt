package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.ClipboardInviteAccess
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.InviteTokenCodec
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.RoomInvite
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CaptureClipboardInviteUseCaseTest {

    private val invite = RoomInvite("room1", "key1", 9_999_999_999_999L)
    private val token = InviteTokenCodec.encode(invite)
    private val now = 1_700_000_000_000L
    private val store: PendingInviteStore = mockk(relaxed = true)

    @Before
    fun setHost() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
    }

    @Test
    fun `valid payload clears and saves canonical uri`() {
        val clip = FakeClipboard("VANISHX_INVITE:JOIN_ROOM:$token:${now + 60_000}")
        CaptureClipboardInviteUseCase(clip, store).invoke(now) shouldBe true
        clip.cleared shouldBe 1
        verify { store.save(InviteUriCodec.format(invite)) }
    }

    @Test
    fun `prefix mismatch does not clear`() {
        val clip = FakeClipboard("buy milk")
        CaptureClipboardInviteUseCase(clip, store).invoke(now) shouldBe false
        clip.cleared shouldBe 0
        verify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `unknown action clears and skips join`() {
        val clip = FakeClipboard("VANISHX_INVITE:OPEN_ROOM:$token:${now + 60_000}")
        CaptureClipboardInviteUseCase(clip, store).invoke(now) shouldBe false
        clip.cleared shouldBe 1
        verify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `expired payload clears and skips join`() {
        val clip = FakeClipboard("VANISHX_INVITE:JOIN_ROOM:$token:$now")
        CaptureClipboardInviteUseCase(clip, store).invoke(now) shouldBe false
        clip.cleared shouldBe 1
        verify(exactly = 0) { store.save(any()) }
    }

    private class FakeClipboard(var text: String?) : ClipboardInviteAccess {
        var cleared = 0
        override fun readPrimaryText(): String? = text
        override fun clearPrimaryClip() {
            cleared += 1
            text = null
        }
    }
}
