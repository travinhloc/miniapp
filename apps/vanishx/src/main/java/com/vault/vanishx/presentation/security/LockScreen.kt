package com.vault.vanishx.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.presentation.components.PinDots
import com.vault.vanishx.presentation.components.PinPad
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
fun LockScreen(
    viewModel: LockViewModel = hiltViewModel(),
    onUnlocked: () -> Unit,
    onWiped: () -> Unit = {},
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(uiState.unlocked, uiState.wiped) {
        when {
            uiState.wiped -> onWiped()
            uiState.unlocked -> onUnlocked()
        }
    }

    LaunchedEffect(uiState.biometricAvailable, uiState.promptBiometric) {
        if (uiState.promptBiometric && activity != null &&
            BiometricUnlockHelper.canAuthenticate(activity)
        ) {
            BiometricUnlockHelper.prompt(
                activity = activity,
                title = context.getString(R.string.lock_bio_title),
                subtitle = context.getString(R.string.lock_bio_subtitle),
                negative = context.getString(R.string.lock_bio_negative),
                onSuccess = { viewModel.onAction(LockAction.BiometricSuccess) },
                onError = { viewModel.onAction(LockAction.BiometricFailed(it)) },
            )
            viewModel.onAction(LockAction.BiometricPromptShown)
        }
    }

    LockContent(
        uiState = uiState,
        showBiometric = activity != null &&
            uiState.biometricEnabled &&
            BiometricUnlockHelper.canAuthenticate(activity),
        onAction = viewModel::onAction,
    )
}

@Composable
private fun LockContent(
    uiState: LockUiState,
    showBiometric: Boolean,
    onAction: (LockAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(
                    if (uiState.showBurnOverlay) R.string.lock_burn_title else R.string.lock_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = VanishXColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    uiState.showBurnOverlay -> stringResource(R.string.lock_burn_body)
                    uiState.attemptsLeft in 1 until SecurityPinStore.MAX_UNLOCK_ATTEMPTS ->
                        stringResource(R.string.lock_attempts_left, uiState.attemptsLeft)
                    else -> stringResource(R.string.lock_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.attemptsLeft <= 2) VanishXColors.Warn else VanishXColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(28.dp))
            if (!uiState.showBurnOverlay) {
                PinDots(filled = uiState.pin.length)
            }
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = VanishXColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }

        when {
            uiState.isBusy || uiState.showBurnOverlay -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VanishXColors.Primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.lock_wiping),
                        color = VanishXColors.Muted,
                    )
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showBiometric) {
                        Button(
                            onClick = { onAction(LockAction.RequestBiometric) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VanishXColors.Accent,
                                contentColor = VanishXColors.OnSurface,
                            ),
                        ) {
                            Text(text = stringResource(R.string.lock_use_bio))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    PinPad(
                        enabled = !uiState.isBusy,
                        onDigit = { onAction(LockAction.Digit(it)) },
                        onBackspace = { onAction(LockAction.Backspace) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
