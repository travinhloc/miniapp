package com.vault.vanishx.presentation.splash

/**
 * Process-scoped splash gate: cold start shows splash once; warm Activity recreation skips it.
 */
object SplashSession {
    @Volatile
    var hasShownThisProcess: Boolean = false
}
