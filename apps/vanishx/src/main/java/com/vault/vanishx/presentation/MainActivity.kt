package com.vault.vanishx.presentation

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.rememberNavController
import com.miniapp.core.ui.theme.ComposeTheme
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import com.vault.vanishx.presentation.security.LockScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var consumePendingInvite: ConsumePendingInviteUseCase

    @Inject
    lateinit var securityPinStore: SecurityPinStore

    @Inject
    lateinit var appLockSession: AppLockSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        captureInviteIntent(intent)
        setContent {
            ComposeTheme {
                var lockTick by remember { mutableIntStateOf(0) }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    if (!isChangingConfigurations && securityPinStore.hasUnlockPin()) {
                        appLockSession.lock()
                        lockTick++
                    }
                }
                @Suppress("UNUSED_EXPRESSION")
                lockTick
                val showLock = securityPinStore.hasUnlockPin() && !appLockSession.isUnlocked
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(navController = rememberNavController())
                    if (showLock) {
                        LockScreen(onUnlocked = { lockTick++ })
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

    private fun captureInviteIntent(intent: Intent?) {
        consumePendingInvite.captureIfInvite(intent?.dataString)
    }
}
