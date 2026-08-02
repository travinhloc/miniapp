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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.rememberNavController
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
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
    lateinit var securityPinStore: SecurityPinStore

    @Inject
    lateinit var appLockSession: AppLockSession

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
                var lockTick by remember { mutableIntStateOf(0) }

                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    if (!isChangingConfigurations && securityPinStore.hasUnlockPin()) {
                        appLockSession.lock()
                        lockTick++
                    }
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    applyFlagSecure(securityPinStore.isFlagSecureEnabled())
                }
                @Suppress("UNUSED_EXPRESSION")
                lockTick

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
                        val showLock = securityPinStore.hasUnlockPin() &&
                            !appLockSession.isUnlocked
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppNavGraph(navController = rememberNavController())
                            if (showLock) {
                                LockScreen(
                                    onUnlocked = { lockTick++ },
                                    onWiped = {
                                        // Silent panic / wipe → AuthSetup (empty Home after new PIN)
                                        lockTick++
                                        phase = RootPhase.AuthSetup
                                    },
                                )
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
        consumePendingInvite.captureIfInvite(intent?.dataString)
    }
}
