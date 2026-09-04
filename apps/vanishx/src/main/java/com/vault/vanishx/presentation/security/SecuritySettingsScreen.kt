@file:Suppress("TooManyFunctions", "ComplexMethod", "UnstableCollections")

package com.vault.vanishx.presentation.security

import com.vault.vanishx.presentation.icons.VanishXIcons

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.BuildConfig
import com.vault.vanishx.R
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.model.BlockedPeer
import com.vault.vanishx.presentation.MainActivity
import com.vault.vanishx.presentation.components.SettingsCard
import com.vault.vanishx.presentation.components.SettingsDangerNote
import com.vault.vanishx.presentation.components.SettingsGroupLabel
import com.vault.vanishx.presentation.components.SettingsIdentityRow
import com.vault.vanishx.presentation.components.SettingsLeadingTone
import com.vault.vanishx.presentation.components.SettingsNavRow
import com.vault.vanishx.presentation.components.SettingsRowDivider
import com.vault.vanishx.presentation.components.SettingsSwitchRow
import com.vault.vanishx.presentation.components.SettingsTopBar
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets

private enum class SettingsPanel {
    Root,
    UnlockPin,
    PanicPin,
    Blocked,
}

@Composable
fun SecuritySettingsScreen(
    viewModel: SecuritySettingsViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var panel by rememberSaveable { mutableStateOf(SettingsPanel.Root) }

    LaunchedEffect(uiState.promptBiometricEnable) {
        if (!uiState.promptBiometricEnable) return@LaunchedEffect
        if (activity == null || !BiometricUnlockHelper.canAuthenticate(activity)) {
            viewModel.onAction(
                SecuritySettingsAction.BiometricEnableFailed(
                    context.getString(R.string.settings_bio_unavailable),
                ),
            )
            return@LaunchedEffect
        }
        BiometricUnlockHelper.prompt(
            activity = activity,
            request = BiometricPromptRequest(
                title = context.getString(R.string.settings_bio_enable_title),
                subtitle = context.getString(R.string.settings_bio_enable_subtitle),
                negative = context.getString(R.string.action_cancel),
                onSuccess = { viewModel.onAction(SecuritySettingsAction.BiometricEnableSuccess) },
                onError = { viewModel.onAction(SecuritySettingsAction.BiometricEnableFailed(it)) },
            ),
        )
        viewModel.onAction(SecuritySettingsAction.BiometricEnablePromptShown)
    }

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
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
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
                    requireCurrentPin = uiState.hasUnlockPin,
                    currentPin = uiState.unlockCurrentPin,
                    currentPinLabel = stringResource(R.string.security_clear_pin_current_label),
                    pin = uiState.unlockPin,
                    pinConfirm = uiState.unlockPinConfirm,
                    pinLabel = stringResource(R.string.security_unlock_pin),
                    pinConfirmLabel = stringResource(R.string.security_unlock_pin_confirm),
                    saveLabel = stringResource(R.string.security_save_unlock),
                    clearLabel = stringResource(R.string.security_clear_unlock),
                    showClear = uiState.hasUnlockPin,
                    formError = uiState.pinFormErrorRes?.let { stringResource(it) }
                        ?: uiState.errorMessage,
                    formInfo = uiState.infoMessage,
                    fieldColors = fieldColors,
                    onCancel = { onPanelChange(SettingsPanel.Root) },
                    onCurrentPinChange = { onAction(SecuritySettingsAction.UnlockCurrentPinChanged(it)) },
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
                    requireCurrentPin = uiState.hasPanicPin,
                    currentPin = uiState.panicCurrentPin,
                    currentPinLabel = stringResource(R.string.security_clear_pin_current_label),
                    pin = uiState.panicPin,
                    pinConfirm = uiState.panicPinConfirm,
                    pinLabel = stringResource(R.string.security_panic_pin),
                    pinConfirmLabel = stringResource(R.string.security_panic_pin_confirm),
                    saveLabel = stringResource(R.string.security_save_panic),
                    clearLabel = stringResource(R.string.security_clear_panic),
                    showClear = uiState.hasPanicPin,
                    formError = uiState.pinFormErrorRes?.let { stringResource(it) }
                        ?: uiState.errorMessage,
                    formInfo = uiState.infoMessage,
                    fieldColors = fieldColors,
                    onCancel = { onPanelChange(SettingsPanel.Root) },
                    onCurrentPinChange = { onAction(SecuritySettingsAction.PanicCurrentPinChanged(it)) },
                    onPinChange = { onAction(SecuritySettingsAction.PanicPinChanged(it)) },
                    onPinConfirmChange = { onAction(SecuritySettingsAction.PanicPinConfirmChanged(it)) },
                    onSave = { onAction(SecuritySettingsAction.SavePanicPin) },
                    onClear = { onAction(SecuritySettingsAction.ClearPanicPin) },
                )
            }
            SettingsPanel.Blocked -> {
                SettingsTopBar(
                    title = stringResource(R.string.settings_blocked_title),
                    onBack = { onPanelChange(SettingsPanel.Root) },
                )
                BlockedPeersPanel(
                    peers = uiState.blockedPeers,
                    onUnblock = { onAction(SecuritySettingsAction.UnblockPeer(it)) },
                )
            }
        }

        FeedbackMessages(uiState = uiState)
    }

    when (uiState.pendingClearPin) {
        PendingClearPin.Unlock -> ClearPinConfirmDialog(
            title = stringResource(R.string.security_clear_unlock_confirm_title),
            body = stringResource(R.string.security_clear_unlock_confirm_body),
            confirmLabel = stringResource(R.string.action_ok),
            pinLabel = stringResource(R.string.security_clear_pin_current_label),
            draft = uiState.clearPinDraft,
            error = uiState.clearPinErrorRes?.let { stringResource(it) },
            fieldColors = fieldColors,
            onDraftChange = { onAction(SecuritySettingsAction.ClearPinDraftChanged(it)) },
            onConfirm = { onAction(SecuritySettingsAction.ConfirmClearPin) },
            onDismiss = { onAction(SecuritySettingsAction.DismissClearPin) },
        )
        PendingClearPin.Panic -> ClearPinConfirmDialog(
            title = stringResource(R.string.security_clear_panic_confirm_title),
            body = stringResource(R.string.security_clear_panic_confirm_body),
            confirmLabel = stringResource(R.string.action_ok),
            pinLabel = stringResource(R.string.security_clear_pin_current_label),
            draft = uiState.clearPinDraft,
            error = uiState.clearPinErrorRes?.let { stringResource(it) },
            fieldColors = fieldColors,
            onDraftChange = { onAction(SecuritySettingsAction.ClearPinDraftChanged(it)) },
            onConfirm = { onAction(SecuritySettingsAction.ConfirmClearPin) },
            onDismiss = { onAction(SecuritySettingsAction.DismissClearPin) },
        )
        null -> Unit
    }
}

@Composable
private fun ClearPinConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    pinLabel: String,
    draft: String,
    error: String?,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    VanishXAlertDialog(
        title = title,
        body = body,
        confirmLabel = confirmLabel,
        dismissLabel = stringResource(R.string.action_cancel),
        tone = VanishXAlertTone.Danger,
        confirmEnabled = draft.length == SecurityPinStore.PIN_LENGTH,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        extraContent = {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = pinLabel) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = !error.isNullOrBlank(),
                supportingText = error?.let {
                    {
                        Text(text = it, color = VanishXColors.Error)
                    }
                },
                colors = fieldColors,
            )
        },
    )
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
                leadingIcon = VanishXIcons.Lock,
                trailingStatus = stringResource(
                    if (uiState.hasUnlockPin) R.string.settings_status_on else R.string.settings_status_off,
                ),
            )
            val activity = LocalContext.current as? FragmentActivity
            if (activity != null && BiometricUnlockHelper.canAuthenticate(activity)) {
                SettingsRowDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_bio_title),
                    subtitle = stringResource(R.string.settings_bio_sub),
                    checked = uiState.biometricEnabled,
                    onCheckedChange = { onAction(SecuritySettingsAction.SetBiometric(it)) },
                )
            }
            SettingsRowDivider()
            SettingsNavRow(
                title = stringResource(R.string.settings_panic_row_title),
                subtitle = stringResource(R.string.settings_panic_row_sub),
                onClick = { onPanelChange(SettingsPanel.PanicPin) },
                leadingIcon = VanishXIcons.Alert,
                leadingTone = SettingsLeadingTone.Warn,
                trailingStatus = stringResource(
                    if (uiState.hasPanicPin) R.string.settings_status_on else R.string.settings_status_off,
                ),
            )
            SettingsRowDivider()
            val flagSecureActivity = LocalContext.current as? MainActivity
            SettingsSwitchRow(
                title = stringResource(R.string.settings_flag_secure_title),
                subtitle = stringResource(R.string.settings_flag_secure_sub),
                checked = uiState.flagSecureEnabled,
                onCheckedChange = { enabled ->
                    onAction(SecuritySettingsAction.SetFlagSecure(enabled))
                    flagSecureActivity?.applyFlagSecure(enabled)
                },
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_auto_wipe_title),
                subtitle = stringResource(R.string.settings_auto_wipe_sub),
                checked = uiState.autoWipeEnabled,
                onCheckedChange = { onAction(SecuritySettingsAction.SetAutoWipe(it)) },
                leadingTone = SettingsLeadingTone.Warn,
            )
            SettingsDangerNote(text = stringResource(R.string.settings_pin_wrong_warning))
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_privacy))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_blocked_row_title),
                subtitle = stringResource(R.string.settings_blocked_row_sub),
                onClick = { onPanelChange(SettingsPanel.Blocked) },
                leadingIcon = VanishXIcons.Account,
                trailingStatus = uiState.blockedPeers.size.toString(),
                showChevron = true,
            )
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_pro))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_pro_row_title),
                subtitle = stringResource(R.string.settings_pro_row_sub),
                onClick = { onAction(SecuritySettingsAction.OpenPaywall) },
                leadingIcon = VanishXIcons.Lock,
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
                leadingIcon = VanishXIcons.Lock,
                leadingTone = SettingsLeadingTone.Accent,
            )
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_data))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_history_row_title),
                subtitle = stringResource(R.string.settings_history_row_sub),
                onClick = { onAction(SecuritySettingsAction.OpenHistory) },
                leadingIcon = VanishXIcons.Lock,
            )
        }

        SettingsGroupLabel(text = stringResource(R.string.settings_section_about))
        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_version_title),
                subtitle = stringResource(R.string.settings_version_sub),
                onClick = null,
                leadingIcon = VanishXIcons.Lock,
                trailingStatus = BuildConfig.VERSION_NAME,
                showChevron = false,
            )
        }
    }
}

@Composable
private fun BlockedPeersPanel(
    peers: List<BlockedPeer>,
    onUnblock: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_blocked_hint),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = VanishXColors.Muted,
        )
        if (peers.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_blocked_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
        } else {
            SettingsCard {
                peers.forEachIndexed { index, peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_blocked_peer_label,
                                peer.peerPub.takeLast(PUB_SUFFIX),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = VanishXColors.OnSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onUnblock(peer.peerPub) }) {
                            Text(text = stringResource(R.string.settings_blocked_unblock))
                        }
                    }
                    if (index < peers.lastIndex) {
                        SettingsRowDivider()
                    }
                }
            }
        }
    }
}

private const val PUB_SUFFIX = 8

@Composable
private fun PinFormPanel(
    hint: String,
    requireCurrentPin: Boolean,
    currentPin: String,
    currentPinLabel: String,
    pin: String,
    pinConfirm: String,
    pinLabel: String,
    pinConfirmLabel: String,
    saveLabel: String,
    clearLabel: String,
    showClear: Boolean,
    formError: String?,
    formInfo: String?,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onCancel: () -> Unit,
    onCurrentPinChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    val hasFormError = !formError.isNullOrBlank()
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
        if (requireCurrentPin) {
            PinField(
                value = currentPin,
                label = currentPinLabel,
                onChange = onCurrentPinChange,
                colors = fieldColors,
                isError = hasFormError,
            )
        }
        PinField(
            value = pin,
            label = pinLabel,
            onChange = onPinChange,
            colors = fieldColors,
            isError = hasFormError,
        )
        PinField(
            value = pinConfirm,
            label = pinConfirmLabel,
            onChange = onPinConfirmChange,
            colors = fieldColors,
            isError = hasFormError,
        )
        if (hasFormError) {
            Text(
                text = formError.orEmpty(),
                color = VanishXColors.Error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!formInfo.isNullOrBlank()) {
            Text(
                text = formInfo,
                color = VanishXColors.Primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        isError = isError,
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
