package com.vault.vanishx.presentation.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

private val ButtonCorner = 8.dp
private val PinPadMaxWidth = 280.dp
private val BurnIconSize = 56.dp
private const val LOW_ATTEMPTS_THRESHOLD = 2
private const val BURN_GRADIENT_ALPHA = 0.22f

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = hiltViewModel(),
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

    LaunchedEffect(uiState.promptBiometric) {
        val shouldPrompt = uiState.promptBiometric &&
            activity != null &&
            BiometricUnlockHelper.canAuthenticate(activity)
        if (!shouldPrompt) return@LaunchedEffect
        BiometricUnlockHelper.prompt(
            activity = activity,
            request = BiometricPromptRequest(
                title = context.getString(R.string.lock_bio_title),
                subtitle = context.getString(R.string.lock_bio_subtitle),
                negative = context.getString(R.string.lock_bio_negative),
                onSuccess = { viewModel.onAction(LockAction.BiometricSuccess) },
                onError = { viewModel.onAction(LockAction.BiometricFailed(it)) },
            ),
        )
        viewModel.onAction(LockAction.BiometricPromptShown)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LockContent(
            uiState = uiState,
            showBiometric = activity != null &&
                uiState.biometricEnabled &&
                BiometricUnlockHelper.canAuthenticate(activity),
            onAction = viewModel::onAction,
        )
        if (uiState.showBurnOverlay) {
            BurnOverlay(isBusy = uiState.isBusy)
        }
    }
}

@Suppress("ComplexMethod")
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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AuthBrandHeader(
                title = stringResource(R.string.lock_title),
                subtitle = stringResource(R.string.lock_subtitle),
            )
            PinDots(
                filled = uiState.pin.length,
                isError = uiState.showWrongPin,
                shakeToken = uiState.shakeToken,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    uiState.showWrongPin -> stringResource(
                        R.string.lock_wrong_pin,
                        uiState.attemptsLeft,
                    )
                    uiState.attemptsLeft in 1 until SecurityPinStore.MAX_UNLOCK_ATTEMPTS ->
                        stringResource(R.string.lock_attempts_left, uiState.attemptsLeft)
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    uiState.showWrongPin || uiState.attemptsLeft <= LOW_ATTEMPTS_THRESHOLD ->
                        VanishXColors.Error
                    else -> VanishXColors.Muted
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )
            uiState.biometricError?.let { error ->
                Text(
                    text = error,
                    color = VanishXColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = PinPadMaxWidth),
        ) {
            PinPad(
                enabled = !uiState.isBusy && !uiState.showBurnOverlay,
                onDigit = { onAction(LockAction.Digit(it)) },
                onBackspace = { onAction(LockAction.Backspace) },
            )
            if (showBiometric) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onAction(LockAction.RequestBiometric) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ButtonCorner),
                    border = BorderStroke(1.dp, VanishXColors.Accent),
                ) {
                    Text(
                        text = stringResource(R.string.lock_use_bio),
                        color = VanishXColors.Accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun BurnOverlay(isBusy: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        VanishXColors.Error.copy(alpha = BURN_GRADIENT_ALPHA),
                        VanishXColors.Bg,
                    ),
                ),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = VanishXColors.Error,
                modifier = Modifier.size(BurnIconSize),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lock_burn_title),
                style = MaterialTheme.typography.headlineSmall,
                color = VanishXColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (isBusy) R.string.lock_burn_progress else R.string.lock_burn_body,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
            )
            if (isBusy) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(color = VanishXColors.Error)
            }
        }
    }
}
