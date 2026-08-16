package com.vault.vanishx.domain.model

import com.vault.vanishx.presentation.invite.InviteBootstrapSession
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test

/**
 * Epic 14.5 edge matrix — unit coverage for the checklist in stories/14.5.
 * Panic wipe + pending-through-Unlock-then-leave live in PanicWipeUseCaseTest /
 * JoinRoomViewModelTest / ConsumePendingInviteUseCaseTest.
 */
class Epic14EdgeMatrixTest {

    @Before
    fun resetSession() {
        InviteBootstrapSession.uriCapturedThisProcess = false
        InviteBootstrapSession.clipboardHandledThisProcess = false
    }

    @Test
    fun `clipboard without PREFIX is ignored`() {
        InviteClipboardParser.parse("https://example.com", nowMs = 1L) shouldBe
            InviteClipboardParser.Result.Ignore
    }

    @Test
    fun `expired clipboard payload is discarded`() {
        val token = InviteTokenCodec.encode(RoomInvite("r", "k", 9_999L))
        InviteClipboardParser.parse("VANISHX_INVITE:JOIN_ROOM:$token:100", nowMs = 100) shouldBe
            InviteClipboardParser.Result.Discard("expired")
    }

    @Test
    fun `App Link capture wins over clipboard this process`() {
        InviteBootstrapSession.onUriCaptureResult(saved = true)
        InviteBootstrapSession.takeClipboardAttempt() shouldBe false
    }

    @Test
    fun `token decode fail rejects garbage`() {
        InviteTokenCodec.decode("%%%not-base64%%%") shouldBe null
        InviteTokenCodec.decode("YWJj") shouldBe null
    }

    @Test
    fun `pending codec never stores clipboard PREFIX`() {
        val invite = RoomInvite("room1", "key1", 9_999_999_999_999L)
        val token = InviteTokenCodec.encode(invite)
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
        val payload = "VANISHX_INVITE:JOIN_ROOM:$token:${Long.MAX_VALUE}"
        InvitePendingCodec.canonicalize(payload) shouldBe InviteUriCodec.format(invite)
        InvitePendingCodec.canonicalize(payload)?.startsWith("VANISHX_INVITE:") shouldBe false
        InvitePendingCodec.canonicalize("VANISHX_INVITE:JOIN_ROOM:bad:1") shouldBe null
    }

    @Test
    fun `HTTPS host is App Links domain not custom scheme`() {
        InviteUriCodec.httpsHost = "vanihx-staging.web.app"
        val uri = InviteUriCodec.format(RoomInvite("r", "k"))
        uri.startsWith("https://vanihx-staging.web.app/join?token=") shouldBe true
        uri shouldNotBe "vanishx://r/r?k=k"
    }
}
