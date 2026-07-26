package com.vault.vanishx.presentation.mailbox

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect

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
private fun JoinRoomContent(
    uiState: JoinRoomUiState,
    onAction: (JoinRoomAction) -> Unit,
    onScanClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.spacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.join_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        Text(
            text = stringResource(R.string.join_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(dimensions.spacingMedium))
        OutlinedTextField(
            value = uiState.input,
            onValueChange = { onAction(JoinRoomAction.InputChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.join_input_label)) },
            singleLine = false,
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        OutlinedButton(
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.join_scan_qr))
        }
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        Button(
            onClick = { onAction(JoinRoomAction.Join) },
            enabled = !uiState.isJoining,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (uiState.isJoining) {
                    stringResource(R.string.join_joining)
                } else {
                    stringResource(R.string.join_action)
                },
            )
        }
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(dimensions.spacingMedium))
        TextButton(onClick = { onAction(JoinRoomAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}
