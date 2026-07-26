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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun AuthSetupScreen(
    viewModel: AuthSetupViewModel = hiltViewModel(),
    onFinished: () -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.auth_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                color = VanishXColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    when (uiState.step) {
                        AuthSetupStep.Enter -> R.string.auth_setup_enter
                        AuthSetupStep.Confirm -> R.string.auth_setup_confirm
                        AuthSetupStep.Biometric -> R.string.auth_setup_biometric
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (uiState.step != AuthSetupStep.Biometric) {
                PinDots(filled = uiState.pin.length)
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
        }

        when (uiState.step) {
            AuthSetupStep.Biometric -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                    }
                    OutlinedButton(
                        onClick = { onAction(AuthSetupAction.EnableBiometric(false)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = stringResource(R.string.auth_setup_skip_bio))
                    }
                }
            }
            else -> {
                PinPad(
                    enabled = !uiState.isBusy,
                    onDigit = { onAction(AuthSetupAction.Digit(it)) },
                    onBackspace = { onAction(AuthSetupAction.Backspace) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
