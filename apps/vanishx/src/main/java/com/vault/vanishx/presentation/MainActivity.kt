@file:Suppress("ComplexCondition")

package com.vault.vanishx.presentation

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.usecase.CaptureClipboardInviteUseCase
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import com.vault.vanishx.presentation.invite.InviteBootstrapSession
import com.vault.vanishx.presentation.security.AuthSetupScreen
import com.vault.vanishx.presentation.security.LockScreen
import com.vault.vanishx.presentation.splash.SplashScreen
import com.vault.vanishx.presentation.splash.SplashSession
import com.vault.vanishx.presentation.theme.VanishXTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private enum class RootPhase {
    Splash,
    AuthSetup,
    Main,
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var consumePendingInvite: ConsumePendingInviteUseCase

    @Inject
    lateinit var captureClipboardInvite: CaptureClipboardInviteUseCase

    @Inject
    lateinit var securityPinStore: SecurityPinStore

    @Inject
    lateinit var appLockSession: AppLockSession

    @Suppress("LongMethod", "ComplexMethod", "MagicNumber")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
        super.onCreate(savedInstanceState)
        applyFlagSecure(securityPinStore.isFlagSecureEnabled())
        captureInviteIntent(intent)
        setContent {
            VanishXTheme {
                var phase by remember {
                    mutableStateOf(
                        if (SplashSession.hasShownThisProcess) {
                            if (securityPinStore.hasUnlockPin()) {
                                RootPhase.Main
                            } else {
                                RootPhase.AuthSetup
                            }
                        } else {
                            RootPhase.Splash
                        },
                    )
                }
                val sessionUnlocked by appLockSession.isUnlockedFlow.collectAsStateWithLifecycle()

                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    if (!isChangingConfigurations &&
                        securityPinStore.hasUnlockPin() &&
                        appLockSession.shouldLockOnStop()
                    ) {
                        appLockSession.lock()
                    }
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    applyFlagSecure(securityPinStore.isFlagSecureEnabled())
                }

                when (phase) {
                    RootPhase.Splash -> SplashScreen(
                        onFinished = {
                            SplashSession.hasShownThisProcess = true
                            phase = if (securityPinStore.hasUnlockPin()) {
                                RootPhase.Main
                            } else {
                                RootPhase.AuthSetup
                            }
                        },
                    )
                    RootPhase.AuthSetup -> AuthSetupScreen(
                        onFinished = { phase = RootPhase.Main },
                    )
                    RootPhase.Main -> {
                        val showLock = securityPinStore.hasUnlockPin() && !sessionUnlocked
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppNavGraph(navController = rememberNavController())
                            // Dialog sits above ModalBottomSheet windows (Join Message Request).
                            if (showLock) {
                                Dialog(
                                    onDismissRequest = {},
                                    properties = DialogProperties(
                                        dismissOnBackPress = false,
                                        dismissOnClickOutside = false,
                                        usePlatformDefaultWidth = false,
                                        decorFitsSystemWindows = false,
                                    ),
                                ) {
                                    LockScreen(
                                        onUnlocked = {},
                                        onWiped = { phase = RootPhase.AuthSetup },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureInviteIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) maybeCaptureClipboardInvite()
    }

    fun applyFlagSecure(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun captureInviteIntent(intent: Intent?) {
        val saved = consumePendingInvite.captureIfInvite(intent?.dataString)
        InviteBootstrapSession.onUriCaptureResult(saved)
    }

    private fun maybeCaptureClipboardInvite() {
        if (!InviteBootstrapSession.takeClipboardAttempt()) return
        captureClipboardInvite()
    }
}
