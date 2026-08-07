package com.vault.vanishx.presentation.mailbox

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.scan.VanishXScanActivity
import com.vault.vanishx.presentation.theme.VanishXColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    viewModel: JoinRoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val context = LocalContext.current
    val appLockSession = remember(context) {
        EntryPointAccessors.fromActivity(
            context as android.app.Activity,
            AppLockEntryPoint::class.java,
        ).appLockSession()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        appLockSession.endExternalUi()
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            viewModel.onAction(JoinRoomAction.Scanned(contents))
        }
    }

    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }
    viewModel.toast.collectAsEffect { toast ->
        val message = when (toast) {
            JoinRoomToast.SAVED_FOR_LATER -> context.getString(R.string.join_later_toast)
            JoinRoomToast.BLOCKED -> context.getString(R.string.join_blocked_toast)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    JoinRoomContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onScanClick = {
            if (cameraPermission.status.isGranted) {
                appLockSession.beginExternalUi()
                scanLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt(context.getString(R.string.join_scan_prompt))
                        setBeepEnabled(false)
                        setOrientationLocked(true)
                        setCaptureActivity(VanishXScanActivity::class.java)
                    },
                )
            } else {
                cameraPermission.launchPermissionRequest()
                Toast.makeText(
                    context,
                    context.getString(R.string.join_camera_permission),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )

    uiState.preview?.let { preview ->
        MessageRequestSheet(
            preview = preview,
            uiState = uiState,
            onAction = viewModel::onAction,
        )
    }

    if (uiState.showBlockConfirm) {
        VanishXAlertDialog(
            title = stringResource(R.string.join_block_title),
            body = stringResource(R.string.join_block_body),
            confirmLabel = stringResource(R.string.join_block_confirm),
            dismissLabel = stringResource(R.string.action_back),
            tone = VanishXAlertTone.Danger,
            onConfirm = { viewModel.onAction(JoinRoomAction.ConfirmBlock) },
            onDismiss = { viewModel.onAction(JoinRoomAction.DismissBlockConfirm) },
        )
    }
}

/** Bottom sheet Message Request (story 7.6): icebreaker · privacy line · nick auto + Đổi · 3 actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageRequestSheet(
    preview: JoinInvitePreview,
    uiState: JoinRoomUiState,
    onAction: (JoinRoomAction) -> Unit,
) {
    val busy = uiState.isJoining || uiState.isBlocking
    ModalBottomSheet(
        onDismissRequest = {
            if (!busy) onAction(JoinRoomAction.DismissPreview)
        },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !busy },
        ),
        containerColor = VanishXColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = VanishXColors.Primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.join_request_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = VanishXColors.OnSurface,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.join_request_subtitle, preview.roomIdLabel),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(14.dp))

            preview.icebreaker?.takeIf { it.isNotBlank() }?.let { icebreaker ->
                IcebreakerCard(icebreaker = icebreaker)
                Spacer(modifier = Modifier.height(12.dp))
            }

            PrivacyLine()
            Spacer(modifier = Modifier.height(14.dp))

            NicknameRow(
                nickname = uiState.nickname,
                isEditing = uiState.isNicknameEditing,
                isBusy = uiState.isJoining || uiState.isBlocking,
                onNicknameChange = { onAction(JoinRoomAction.NicknameChanged(it)) },
                onToggleEdit = { onAction(JoinRoomAction.ToggleNicknameEdit) },
            )
            Spacer(modifier = Modifier.height(18.dp))

            MessageRequestActions(
                isJoining = uiState.isJoining,
                isBlocking = uiState.isBlocking,
                isPeerBlocked = preview.isPeerBlocked,
                onAccept = { onAction(JoinRoomAction.AcceptAndChat) },
                onUnblockAndChat = { onAction(JoinRoomAction.UnblockAndChat) },
                onLater = { onAction(JoinRoomAction.SaveForLater) },
                onBlock = { onAction(JoinRoomAction.OpenBlockConfirm) },
            )

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = joinErrorText(error),
                    color = VanishXColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun IcebreakerCard(icebreaker: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VanishXColors.Accent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, VanishXColors.Accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.join_request_icebreaker_label),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = VanishXColors.Accent,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = icebreaker,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = VanishXColors.OnSurface,
            )
        }
    }
}

@Composable
private fun PrivacyLine() {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = VanishXColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.join_request_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.Muted,
        )
    }
}

@Composable
private fun NicknameRow(
    nickname: String,
    isEditing: Boolean,
    isBusy: Boolean,
    onNicknameChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = VanishXColors.Surface2,
        border = BorderStroke(1.dp, VanishXColors.Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = VanishXColors.Muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isBusy,
                    placeholder = { Text(text = stringResource(R.string.join_nickname_hint)) },
                )
            } else {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleSmall,
                    color = VanishXColors.OnSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = onToggleEdit, enabled = !isBusy) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.join_nickname_save else R.string.join_nickname_change,
                    ),
                    color = VanishXColors.Primary,
                )
            }
        }
    }
}

@Composable
private fun MessageRequestActions(
    isJoining: Boolean,
    isBlocking: Boolean,
    isPeerBlocked: Boolean,
    onAccept: () -> Unit,
    onUnblockAndChat: () -> Unit,
    onLater: () -> Unit,
    onBlock: () -> Unit,
) {
    val busy = isJoining || isBlocking
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isPeerBlocked) {
            Text(
                text = stringResource(R.string.join_peer_blocked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.NeonAmber,
            )
            Button(
                onClick = onUnblockAndChat,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isJoining) {
                        stringResource(R.string.join_joining)
                    } else {
                        stringResource(R.string.join_unblock_and_chat)
                    },
                )
            }
        } else {
            Button(
                onClick = onAccept,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isJoining) {
                        stringResource(R.string.join_joining)
                    } else {
                        stringResource(R.string.join_accept_chat)
                    },
                )
            }
        }
        OutlinedButton(
            onClick = onLater,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = stringResource(R.string.join_save_for_later))
        }
        if (!isPeerBlocked) {
            OutlinedButton(
                onClick = onBlock,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, VanishXColors.Error.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VanishXColors.Error),
            ) {
                Text(
                    text = if (isBlocking) {
                        stringResource(R.string.join_blocking)
                    } else {
                        stringResource(R.string.join_block)
                    },
                )
            }
        }
    }
}

@Composable
private fun JoinRoomContent(
    uiState: JoinRoomUiState,
    onAction: (JoinRoomAction) -> Unit,
    onScanClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onAction(JoinRoomAction.Back) }) {
                Text(text = stringResource(R.string.action_back))
            }
            Text(
                text = stringResource(R.string.join_title),
                style = MaterialTheme.typography.titleLarge,
                color = VanishXColors.OnSurface,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.join_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VanishXColors.Surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.input,
                        onValueChange = { onAction(JoinRoomAction.InputChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(R.string.join_input_label)) },
                        singleLine = false,
                        minLines = 2,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onScanClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VanishXColors.Primary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.join_scan_qr))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onAction(JoinRoomAction.RequestPreview) },
                        enabled = !uiState.isJoining,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VanishXColors.Primary,
                            contentColor = VanishXColors.OnPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.join_action))
                    }
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = joinErrorText(error),
                            color = VanishXColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
interface AppLockEntryPoint {
    fun appLockSession(): AppLockSession
}

@Composable
private fun joinErrorText(error: String): String =
    when {
        error.contains("Peer is blocked", ignoreCase = true) ->
            stringResource(R.string.join_error_peer_blocked)
        else -> error
    }
