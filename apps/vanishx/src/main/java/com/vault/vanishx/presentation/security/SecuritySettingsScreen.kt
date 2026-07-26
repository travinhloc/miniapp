package com.vault.vanishx.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect

@Composable
fun SecuritySettingsScreen(
    viewModel: SecuritySettingsViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    SecuritySettingsContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun SecuritySettingsContent(
    uiState: SecuritySettingsUiState,
    onAction: (SecuritySettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Text(
            text = stringResource(R.string.security_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.security_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(
                if (uiState.hasUnlockPin) {
                    R.string.security_unlock_status_on
                } else {
                    R.string.security_unlock_status_off
                },
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        PinField(
            value = uiState.unlockPin,
            label = stringResource(R.string.security_unlock_pin),
            onChange = { onAction(SecuritySettingsAction.UnlockPinChanged(it)) },
        )
        PinField(
            value = uiState.unlockPinConfirm,
            label = stringResource(R.string.security_unlock_pin_confirm),
            onChange = { onAction(SecuritySettingsAction.UnlockPinConfirmChanged(it)) },
        )
        Button(
            onClick = { onAction(SecuritySettingsAction.SaveUnlockPin) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.security_save_unlock))
        }
        if (uiState.hasUnlockPin) {
            OutlinedButton(
                onClick = { onAction(SecuritySettingsAction.ClearUnlockPin) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.security_clear_unlock))
            }
        }

        Spacer(modifier = Modifier.height(dimensions.spacingMedium))
        Text(
            text = stringResource(
                if (uiState.hasPanicPin) {
                    R.string.security_panic_status_on
                } else {
                    R.string.security_panic_status_off
                },
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.security_panic_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        PinField(
            value = uiState.panicPin,
            label = stringResource(R.string.security_panic_pin),
            onChange = { onAction(SecuritySettingsAction.PanicPinChanged(it)) },
        )
        PinField(
            value = uiState.panicPinConfirm,
            label = stringResource(R.string.security_panic_pin_confirm),
            onChange = { onAction(SecuritySettingsAction.PanicPinConfirmChanged(it)) },
        )
        Button(
            onClick = { onAction(SecuritySettingsAction.SavePanicPin) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.security_save_panic))
        }
        if (uiState.hasPanicPin) {
            OutlinedButton(
                onClick = { onAction(SecuritySettingsAction.ClearPanicPin) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.security_clear_panic))
            }
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        uiState.infoMessage?.let { info ->
            Text(
                text = info,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(onClick = { onAction(SecuritySettingsAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun PinField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
    )
}
