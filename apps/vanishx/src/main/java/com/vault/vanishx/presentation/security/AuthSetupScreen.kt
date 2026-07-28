package com.vault.vanishx.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vault.vanishx.presentation.components.PinDots
import com.vault.vanishx.presentation.components.PinPad
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
fun AuthSetupScreen(
    onFinished: () -> Unit,
    viewModel: AuthSetupViewModel = hiltViewModel(),
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onFinished()
    }

    AuthSetupContent(
        uiState = uiState,
        biometricAvailable = activity != null &&
            BiometricUnlockHelper.canAuthenticate(activity),
        onAction = viewModel::onAction,
    )
}

private val SetupCardCorner = 16.dp
private val SetupPinPadMaxWidth = 280.dp
private val BioCardMaxWidth = 320.dp
private const val SETUP_STEP_COUNT = 3
private val StepChipSize = 8.dp

@Composable
private fun AuthSetupContent(
    uiState: AuthSetupUiState,
    biometricAvailable: Boolean,
    onAction: (AuthSetupAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AuthBrandHeader(
                title = stringResource(R.string.auth_setup_title),
                subtitle = stringResource(
                    when (uiState.step) {
                        AuthSetupStep.Enter -> R.string.auth_setup_enter
                        AuthSetupStep.Confirm -> R.string.auth_setup_confirm
                        AuthSetupStep.Biometric -> R.string.auth_setup_biometric
                    },
                ),
            )
            StepChips(step = uiState.step)
            if (uiState.step != AuthSetupStep.Biometric) {
                PinSetupSection(uiState = uiState)
            }
        }

        when (uiState.step) {
            AuthSetupStep.Biometric -> BiometricStep(
                biometricAvailable = biometricAvailable,
                onAction = onAction,
            )
            else -> PinPad(
                enabled = !uiState.isBusy,
                onDigit = { onAction(AuthSetupAction.Digit(it)) },
                onBackspace = { onAction(AuthSetupAction.Backspace) },
                modifier = Modifier.widthIn(max = SetupPinPadMaxWidth),
            )
        }
    }
}

@Composable
private fun PinSetupSection(uiState: AuthSetupUiState) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(
            if (uiState.step == AuthSetupStep.Confirm) {
                R.string.auth_setup_round_confirm
            } else {
                R.string.auth_setup_round_enter
            },
        ),
        style = MaterialTheme.typography.labelMedium,
        color = if (uiState.step == AuthSetupStep.Confirm) {
            VanishXColors.Accent
        } else {
            VanishXColors.Primary
        },
    )
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(SetupCardCorner),
        color = VanishXColors.Surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            PinDots(
                filled = uiState.pin.length,
                isError = uiState.showMismatch,
                shakeToken = uiState.shakeToken,
            )
            if (uiState.showMismatch) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_setup_mismatch),
                    color = VanishXColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StepChips(step: AuthSetupStep) {
    val index = when (step) {
        AuthSetupStep.Enter -> 0
        AuthSetupStep.Confirm -> 1
        AuthSetupStep.Biometric -> 2
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(SETUP_STEP_COUNT) { i ->
            Box(
                modifier = Modifier
                    .size(StepChipSize)
                    .background(
                        color = if (i <= index) VanishXColors.Primary else VanishXColors.Outline,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun BiometricStep(
    biometricAvailable: Boolean,
    onAction: (AuthSetupAction) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(SetupCardCorner),
        color = VanishXColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = BioCardMaxWidth),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.auth_setup_bio_hint),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (biometricAvailable) {
                Button(
                    onClick = { onAction(AuthSetupAction.EnableBiometric(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanishXColors.Primary,
                        contentColor = VanishXColors.OnPrimary,
                    ),
                ) {
                    Text(text = stringResource(R.string.auth_setup_enable_bio))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            TextButton(
                onClick = { onAction(AuthSetupAction.EnableBiometric(false)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.auth_setup_skip_bio),
                    color = VanishXColors.Muted,
                )
            }
        }
    }
}
