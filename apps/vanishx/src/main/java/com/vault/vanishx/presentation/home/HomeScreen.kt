package com.vault.vanishx.presentation.home

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.VanishXTheme
import com.vault.vanishx.presentation.util.formatRemainingMs

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navigator: (destination: BaseDestination) -> Unit,
    createdInviteUri: String? = null,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.error.collectAsEffect { /* reserved */ }
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    LaunchedEffect(createdInviteUri) {
        if (createdInviteUri != null) {
            viewModel.onReturnedFromCreate(createdInviteUri)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermission.status.isGranted) {
                notificationPermission.launchPermissionRequest()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(HomeAction.Resume)
    }

    HomeScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onCopyInvite = { uri ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("VanishX invite", uri))
            Toast.makeText(context, context.getString(R.string.create_copied), Toast.LENGTH_SHORT).show()
            viewModel.onAction(HomeAction.DismissShareHint)
        },
        onShareInvite = { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, uri)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.create_share)),
            )
            viewModel.onAction(HomeAction.DismissShareHint)
        },
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                )
            }
            TextButton(onClick = { onAction(HomeAction.OpenSettings) }) {
                Text(text = stringResource(R.string.home_settings))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onAction(HomeAction.CreateRoom) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.home_create_room))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.inviteDraft,
                onValueChange = { onAction(HomeAction.InviteDraftChanged(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.home_invite_hint)) },
            )
            Button(
                onClick = { onAction(HomeAction.JoinFromDraft) },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.home_join_go))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onAction(HomeAction.ScanQr) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = stringResource(R.string.home_scan_qr))
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = VanishXColors.Error, style = MaterialTheme.typography.bodySmall)
        }

        uiState.shareHintUri?.let { uri ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VanishXColors.Surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.home_share_hint),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onCopyInvite(uri) }) {
                            Text(text = stringResource(R.string.create_copy))
                        }
                        TextButton(onClick = { onShareInvite(uri) }) {
                            Text(text = stringResource(R.string.create_share))
                        }
                        TextButton(onClick = { onAction(HomeAction.DismissShareHint) }) {
                            Text(text = stringResource(R.string.action_back))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_recent_title),
            style = MaterialTheme.typography.titleMedium,
            color = VanishXColors.OnSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.recentRooms.isEmpty()) {
            Text(
                text = stringResource(R.string.home_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
        } else {
            uiState.recentRooms.forEach { room ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VanishXColors.Surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onAction(HomeAction.OpenRoom(room.id)) },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = room.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (room.isExpired) {
                                    stringResource(R.string.badge_expired)
                                } else {
                                    stringResource(
                                        R.string.badge_remaining,
                                        formatRemainingMs(room.remainingMs),
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (room.isExpired) VanishXColors.Error else VanishXColors.Primary,
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_open),
                            color = VanishXColors.Primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        if (uiState.hasMoreRooms) {
            TextButton(
                onClick = { onAction(HomeAction.OpenHistory) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = stringResource(R.string.home_see_more))
            }
        } else {
            TextButton(
                onClick = { onAction(HomeAction.OpenHistory) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = stringResource(R.string.home_history))
            }
        }

        if (uiState.showProStubToggle) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { onAction(HomeAction.ToggleProStub) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isProStub) R.string.home_pro_stub_on else R.string.home_pro_stub_off,
                    ),
                )
            }
        }

        uiState.anonymousId?.let { id ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_anonymous_id, id),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    VanishXTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                anonymousId = "vx_demo",
                recentRooms = listOf(
                    HomeRoomItem("1", "Lan", 3_600_000, false, "creator"),
                ),
                showProStubToggle = true,
            ),
            onAction = {},
            onCopyInvite = {},
            onShareInvite = {},
        )
    }
}
