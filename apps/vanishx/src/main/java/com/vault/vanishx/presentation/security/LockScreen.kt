@file:Suppress("ComplexMethod", "MagicNumber")

package com.vault.vanishx.presentation.security

import com.vault.vanishx.presentation.icons.VanishXIcons

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.presentation.components.PinDots
import com.vault.vanishx.presentation.components.PinPad
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onAction(LockAction.ClearPinDraft)
    }

    LaunchedEffect(Unit) {
        // Activity-scoped VM survives unlock; reset so a later lock cycle can unlock again.
        viewModel.onAction(LockAction.PrepareChallenge)
    }

    LaunchedEffect(uiState.cooldownRemainingMs > 0L) {
        if (uiState.cooldownRemainingMs <= 0L) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(250L)
            viewModel.onAction(LockAction.TickCooldown)
            if (viewModel.uiState.value.cooldownRemainingMs <= 0L) break
        }
    }

    LaunchedEffect(uiState.unlocked, uiState.wiped) {
        when {
            uiState.wiped -> onWiped()
            uiState.unlocked -> onUnlocked()
        }
    }

    LaunchedEffect(uiState.promptBiometric, uiState.cooldownRemainingMs) {
        val shouldPrompt = uiState.promptBiometric &&
            uiState.cooldownRemainingMs <= 0L &&
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
                uiState.cooldownRemainingMs <= 0L &&
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
    val coolingDown = uiState.cooldownRemainingMs > 0L
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AuthBrandHeader(
                title = stringResource(
                    if (coolingDown) R.string.lock_cooldown_title else R.string.lock_title,
                ),
                subtitle = stringResource(
                    if (coolingDown) R.string.lock_cooldown_subtitle else R.string.lock_subtitle,
                ),
            )
            if (coolingDown) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = formatCooldown(uiState.cooldownRemainingMs),
                    style = MaterialTheme.typography.displaySmall,
                    color = VanishXColors.Primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.lock_cooldown_tier_hint,
                        uiState.cooldownTierIndex + 1,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                    textAlign = TextAlign.Center,
                )
            } else {
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
                        else -> stringResource(R.string.lock_ok_hint)
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
            }
            uiState.biometricError?.let { error ->
                Text(
                    text = error,
                    color = VanishXColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (!coolingDown) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = PinPadMaxWidth),
            ) {
                PinPad(
                    enabled = !uiState.isBusy && !uiState.showBurnOverlay,
                    onDigit = { onAction(LockAction.Digit(it)) },
                    onBackspace = { onAction(LockAction.Backspace) },
                    onSubmit = { onAction(LockAction.Submit) },
                    submitEnabled = uiState.pin.length == SecurityPinStore.PIN_LENGTH,
                    submitLabel = stringResource(R.string.lock_ok),
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
        } else {
            Spacer(modifier = Modifier.height(1.dp))
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
                imageVector = VanishXIcons.Alert,
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

private fun formatCooldown(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
