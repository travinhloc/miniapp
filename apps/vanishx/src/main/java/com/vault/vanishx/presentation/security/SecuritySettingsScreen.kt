package com.vault.vanishx.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

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
            .background(VanishXColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.security_title),
            style = MaterialTheme.typography.headlineSmall,
            color = VanishXColors.OnSurface,
        )

        SettingsSection(title = stringResource(R.string.settings_section_identity)) {
            uiState.anonymousId?.let { id ->
                Text(
                    text = stringResource(R.string.home_anonymous_id, id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VanishXColors.Muted,
                )
            } ?: Text(
                text = stringResource(R.string.settings_identity_loading),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_security)) {
            Text(
                text = stringResource(
                    if (uiState.hasUnlockPin) {
                        R.string.security_unlock_status_on
                    } else {
                        R.string.security_unlock_status_off
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
                color = VanishXColors.OnSurface,
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
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Text(text = stringResource(R.string.security_save_unlock))
            }
            if (uiState.hasUnlockPin) {
                OutlinedButton(
                    onClick = { onAction(SecuritySettingsAction.ClearUnlockPin) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = stringResource(R.string.security_clear_unlock))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (uiState.hasPanicPin) {
                        R.string.security_panic_status_on
                    } else {
                        R.string.security_panic_status_off
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
                color = VanishXColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.security_panic_hint),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
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
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.security_save_panic))
            }
            if (uiState.hasPanicPin) {
                OutlinedButton(
                    onClick = { onAction(SecuritySettingsAction.ClearPanicPin) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = stringResource(R.string.security_clear_panic))
                }
            }
        }

        if (uiState.showProStubToggle) {
            SettingsSection(title = stringResource(R.string.settings_section_pro)) {
                TextButton(
                    onClick = { onAction(SecuritySettingsAction.ToggleProStub) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (uiState.isProStub) R.string.home_pro_stub_on else R.string.home_pro_stub_off,
                        ),
                    )
                }
                Text(
                    text = stringResource(R.string.home_pro_stub_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_data)) {
            OutlinedButton(
                onClick = { onAction(SecuritySettingsAction.OpenHistory) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.home_history))
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = VanishXColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
            )
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = VanishXColors.Error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        uiState.infoMessage?.let { info ->
            Text(
                text = info,
                color = VanishXColors.Primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(onClick = { onAction(SecuritySettingsAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = VanishXColors.Primary,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VanishXColors.Surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
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
