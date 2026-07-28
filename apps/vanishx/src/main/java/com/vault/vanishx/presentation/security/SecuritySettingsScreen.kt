@file:Suppress("TooManyFunctions", "ComplexMethod")

package com.vault.vanishx.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.R
import com.vault.vanishx.presentation.components.SettingsCard
import com.vault.vanishx.presentation.components.SettingsDangerNote
import com.vault.vanishx.presentation.components.SettingsGroupLabel
import com.vault.vanishx.presentation.components.SettingsIdentityRow
import com.vault.vanishx.presentation.components.SettingsLeadingTone
import com.vault.vanishx.presentation.components.SettingsNavRow
import com.vault.vanishx.presentation.components.SettingsRowDivider
import com.vault.vanishx.presentation.components.SettingsTopBar
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors

private enum class SettingsPanel {
    Root,
    UnlockPin,
    PanicPin,
}

@Composable
fun SecuritySettingsScreen(
    viewModel: SecuritySettingsViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    var panel by rememberSaveable { mutableStateOf(SettingsPanel.Root) }

    SecuritySettingsContent(
        uiState = uiState,
        panel = panel,
        onPanelChange = { panel = it },
        onAction = viewModel::onAction,
    )
}

@Composable
private fun SecuritySettingsContent(
    uiState: SecuritySettingsUiState,
    panel: SettingsPanel,
    onPanelChange: (SettingsPanel) -> Unit,
    onAction: (SecuritySettingsAction) -> Unit,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VanishXColors.Primary,
        unfocusedBorderColor = VanishXColors.Outline,
        focusedTextColor = VanishXColors.OnSurface,
        unfocusedTextColor = VanishXColors.OnSurface,
        focusedLabelColor = VanishXColors.Primary,
        unfocusedLabelColor = VanishXColors.Muted,
        cursorColor = VanishXColors.Primary,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg),
    ) {
        when (panel) {
            SettingsPanel.Root -> {
                SettingsTopBar(
                    title = stringResource(R.string.security_title),
                    onBack = { onAction(SecuritySettingsAction.Back) },
                )
                SettingsRootPanel(
                    uiState = uiState,
                    onPanelChange = onPanelChange,
                    onAction = onAction,
                )
            }
            SettingsPanel.UnlockPin -> {
                SettingsTopBar(
                    title = stringResource(R.string.settings_unlock_screen_title),
                    onBack = { onPanelChange(SettingsPanel.Root) },
                )
                PinFormPanel(
                    hint = stringResource(R.string.settings_unlock_form_hint),
                    pin = uiState.unlockPin,
                    pinConfirm = uiState.unlockPinConfirm,
                    pinLabel = stringResource(R.string.security_unlock_pin),
                    pinConfirmLabel = stringResource(R.string.security_unlock_pin_confirm),
                    saveLabel = stringResource(R.string.security_save_unlock),
                    clearLabel = stringResource(R.string.security_clear_unlock),
                    showClear = uiState.hasUnlockPin,
                    fieldColors = fieldColors,
                    onCancel = { onPanelChange(SettingsPanel.Root) },
                    onPinChange = { onAction(SecuritySettingsAction.UnlockPinChanged(it)) },
                    onPinConfirmChange = { onAction(SecuritySettingsAction.UnlockPinConfirmChanged(it)) },
                    onSave = { onAction(SecuritySettingsAction.SaveUnlockPin) },
                    onClear = { onAction(SecuritySettingsAction.ClearUnlockPin) },
                )
            }
            SettingsPanel.PanicPin -> {
                SettingsTopBar(
                    title = stringResource(R.string.settings_panic_screen_title),
                    onBack = { onPanelChange(SettingsPanel.Root) },
                )
                PinFormPanel(
                    hint = stringResource(R.string.security_panic_hint),
                    pin = uiState.panicPin,
                    pinConfirm = uiState.panicPinConfirm,
                    pinLabel = stringResource(R.string.security_panic_pin),
                    pinConfirmLabel = stringResource(R.string.security_panic_pin_confirm),
                    saveLabel = stringResource(R.string.security_save_panic),
                    clearLabel = stringResource(R.string.security_clear_panic),
                    showClear = uiState.hasPanicPin,
                    fieldColors = fieldColors,
                    onCancel = { onPanelChange(SettingsPanel.Root) },
                    onPinChange = { onAction(SecuritySettingsAction.PanicPinChanged(it)) },
                    onPinConfirmChange = { onAction(SecuritySettingsAction.PanicPinConfirmChanged(it)) },
                    onSave = { onAction(SecuritySettingsAction.SavePanicPin) },
                    onClear = { onAction(SecuritySettingsAction.ClearPanicPin) },
                )
            }
        }

        FeedbackMessages(uiState = uiState)
    }
}

@Composable
private fun SettingsRootPanel(
    uiState: SecuritySettingsUiState,
    onPanelChange: (SettingsPanel) -> Unit,
    onAction: (SecuritySettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsGroupLabel(text = stringResource(R.string.settings_section_identity))
        SettingsCard {
            SettingsIdentityRow(
                title = stringResource(R.string.settings_identity_title),
                subtitle = stringResource(R.string.settings_identity_sub),
                anonymousId = uiState.anonymousId,
            )
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_security))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_unlock_row_title),
                subtitle = stringResource(R.string.settings_unlock_row_sub),
                onClick = { onPanelChange(SettingsPanel.UnlockPin) },
                leadingIcon = Icons.Filled.Lock,
                trailingStatus = stringResource(
                    if (uiState.hasUnlockPin) R.string.settings_status_on else R.string.settings_status_off,
                ),
            )
            SettingsRowDivider()
            SettingsNavRow(
                title = stringResource(R.string.settings_panic_row_title),
                subtitle = stringResource(R.string.settings_panic_row_sub),
                onClick = { onPanelChange(SettingsPanel.PanicPin) },
                leadingIcon = Icons.Filled.Warning,
                leadingTone = SettingsLeadingTone.Warn,
                trailingStatus = stringResource(
                    if (uiState.hasPanicPin) R.string.settings_status_on else R.string.settings_status_off,
                ),
            )
            SettingsDangerNote(text = stringResource(R.string.settings_pin_wrong_warning))
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_pro))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_pro_row_title),
                subtitle = stringResource(R.string.settings_pro_row_sub),
                onClick = { onAction(SecuritySettingsAction.OpenPaywall) },
                leadingIcon = Icons.Filled.Lock,
                leadingTone = SettingsLeadingTone.Accent,
                trailingStatus = stringResource(
                    if (uiState.isProStub) R.string.settings_pro_active else R.string.settings_pro_free,
                ),
                trailingStatusAccent = uiState.isProStub,
            )
            SettingsRowDivider()
            SettingsNavRow(
                title = stringResource(R.string.settings_restore_purchases),
                subtitle = stringResource(R.string.settings_restore_sub),
                onClick = { onAction(SecuritySettingsAction.RestorePurchases) },
                leadingIcon = Icons.Filled.Lock,
                leadingTone = SettingsLeadingTone.Accent,
            )
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_data))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_history_row_title),
                subtitle = stringResource(R.string.settings_history_row_sub),
                onClick = { onAction(SecuritySettingsAction.OpenHistory) },
                leadingIcon = Icons.Filled.Lock,
            )
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_about))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_version_title),
                subtitle = stringResource(R.string.settings_version_sub),
                onClick = null,
                leadingIcon = Icons.Filled.Lock,
                trailingStatus = BuildConfig.VERSION_NAME,
                showChevron = false,
            )
        }
    }
}

@Composable
private fun PinFormPanel(
    hint: String,
    pin: String,
    pinConfirm: String,
    pinLabel: String,
    pinConfirmLabel: String,
    saveLabel: String,
    clearLabel: String,
    showClear: Boolean,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onCancel: () -> Unit,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = VanishXColors.Muted,
        )
        PinField(
            value = pin,
            label = pinLabel,
            onChange = onPinChange,
            colors = fieldColors,
        )
        PinField(
            value = pinConfirm,
            label = pinConfirmLabel,
            onChange = onPinConfirmChange,
            colors = fieldColors,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Text(text = saveLabel)
            }
        }
        if (showClear) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VanishXColors.Error),
                border = androidx.compose.foundation.BorderStroke(1.dp, VanishXColors.Error),
            ) {
                Text(text = clearLabel)
            }
        }
    }
}

@Composable
private fun PinField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        colors = colors,
    )
}

@Composable
private fun FeedbackMessages(uiState: SecuritySettingsUiState) {
    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = VanishXColors.Error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    uiState.infoMessage?.let { info ->
        Text(
            text = info,
            color = VanishXColors.Primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
