package com.vault.vanishx.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.miniapp.core.ui.theme.ComposeTheme
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var consumePendingInvite: ConsumePendingInviteUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureInviteIntent(intent)
        setContent {
            ComposeTheme {
                AppNavGraph(navController = rememberNavController())
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
