package com.vault.vanishx.presentation.invite

/**
 * Process-scoped invite bootstrap: clipboard is attempted at most once per process.
 * Native URI (App Link / vanishx://) wins and skips clipboard (story 14.3).
 */
object InviteBootstrapSession {
    @Volatile
    var uriCapturedThisProcess: Boolean = false

    @Volatile
    var clipboardHandledThisProcess: Boolean = false

    fun onUriCaptureResult(saved: Boolean) {
        if (saved) uriCapturedThisProcess = true
    }

    /** @return true if clipboard should be read now. */
    fun takeClipboardAttempt(): Boolean {
        if (clipboardHandledThisProcess) return false
        clipboardHandledThisProcess = true
        return !uriCapturedThisProcess
    }
}
