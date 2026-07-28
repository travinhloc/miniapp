package com.vault.vanishx.presentation.mailbox

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun JoinRoomScreen(
    viewModel: JoinRoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            viewModel.onAction(JoinRoomAction.Scanned(contents))
        }
    }

    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    uiState.preview?.let { preview ->
        JoinNicknameDialog(
            preview = preview,
            nickname = uiState.nickname,
            isJoining = uiState.isJoining,
            onNicknameChange = { viewModel.onAction(JoinRoomAction.NicknameChanged(it)) },
            onEnterRoom = { viewModel.onAction(JoinRoomAction.EnterRoom) },
            onSaveForLater = { viewModel.onAction(JoinRoomAction.SaveForLater) },
            onDismiss = { viewModel.onAction(JoinRoomAction.DismissPreview) },
        )
    }

    JoinRoomContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onScanClick = {
            if (cameraPermission.status.isGranted) {
                scanLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt(context.getString(R.string.join_scan_prompt))
                        setBeepEnabled(false)
                        setOrientationLocked(true)
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
}

@Composable
private fun JoinNicknameDialog(
    preview: JoinInvitePreview,
    nickname: String,
    isJoining: Boolean,
    onNicknameChange: (String) -> Unit,
    onEnterRoom: () -> Unit,
    onSaveForLater: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VanishXColors.Surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.join_preview_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = stringResource(R.string.join_preview_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VanishXColors.Surface2, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = preview.roomTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = VanishXColors.OnSurface,
                    )
                    Text(
                        text = stringResource(R.string.join_preview_room_id, preview.roomIdLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = VanishXColors.Muted,
                    )
                    preview.remainingLabel?.let { remaining ->
                        Text(
                            text = stringResource(R.string.badge_remaining, remaining),
                            style = MaterialTheme.typography.bodySmall,
                            color = VanishXColors.Primary,
                        )
                    }
                }
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isJoining,
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.join_nickname_label)) },
                    placeholder = { Text(text = stringResource(R.string.join_nickname_hint)) },
                )
                RowActions(
                    isJoining = isJoining,
                    onSaveForLater = onSaveForLater,
                    onEnterRoom = onEnterRoom,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun RowActions(
    isJoining: Boolean,
    onSaveForLater: () -> Unit,
    onEnterRoom: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onEnterRoom,
            enabled = !isJoining,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
        ) {
            Text(
                text = if (isJoining) {
                    stringResource(R.string.join_joining)
                } else {
                    stringResource(R.string.join_enter_room)
                },
            )
        }
        OutlinedButton(
            onClick = onSaveForLater,
            enabled = !isJoining,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = stringResource(R.string.join_save_for_later))
        }
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.action_back))
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
            .background(VanishXColors.Bg),
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
                            text = error,
                            color = VanishXColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
