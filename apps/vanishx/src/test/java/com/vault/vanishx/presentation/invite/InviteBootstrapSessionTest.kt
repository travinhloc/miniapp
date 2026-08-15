package com.vault.vanishx.presentation.invite

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test

class InviteBootstrapSessionTest {

    @Before
    fun reset() {
        InviteBootstrapSession.uriCapturedThisProcess = false
        InviteBootstrapSession.clipboardHandledThisProcess = false
    }

    @Test
    fun `native uri wins over clipboard`() {
        InviteBootstrapSession.onUriCaptureResult(saved = true)
        InviteBootstrapSession.takeClipboardAttempt() shouldBe false
        InviteBootstrapSession.takeClipboardAttempt() shouldBe false
    }

    @Test
    fun `clipboard is attempted once when no uri`() {
        InviteBootstrapSession.takeClipboardAttempt() shouldBe true
        InviteBootstrapSession.takeClipboardAttempt() shouldBe false
    }

    @Test
    fun `failed uri capture still allows clipboard`() {
        InviteBootstrapSession.onUriCaptureResult(saved = false)
        InviteBootstrapSession.takeClipboardAttempt() shouldBe true
    }
}
