package com.vault.vanishx.presentation.home

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.VanishXTheme
import com.vault.vanishx.presentation.util.formatRemainingMs

private val CardCorner = 16.dp
private val ButtonCorner = 8.dp
private val ContentPaddingH = 16.dp
private val HeroBorderAlpha = 0.08f
private val AccentEdgeWidth = 3.dp
private const val GLOW_TOP_X = 0.5f
private const val GLOW_TOP_Y = -0.2f
private const val GLOW_TOP_R = 0.85f
private const val GLOW_BOTTOM_X = 0.1f
private const val GLOW_BOTTOM_Y = 0.9f
private const val GLOW_BOTTOM_R = 0.7f
private const val GLOW_ACCENT_A = 0.12f
private const val GLOW_PRIMARY_A = 0.08f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navigator: (destination: BaseDestination) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
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

@Suppress("ComplexMethod")
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<HomeRoomItem?>(null) }

    pendingDelete?.let { room ->
        VanishXAlertDialog(
            title = stringResource(R.string.home_delete_title),
            body = stringResource(R.string.home_delete_body, room.displayName),
            confirmLabel = stringResource(R.string.home_delete_confirm),
            dismissLabel = stringResource(R.string.action_cancel),
            tone = VanishXAlertTone.Danger,
            onConfirm = {
                onAction(HomeAction.DeleteRoom(room.id))
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VanishXColors.Accent.copy(alpha = GLOW_ACCENT_A),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * GLOW_TOP_X, size.height * GLOW_TOP_Y),
                        radius = size.minDimension * GLOW_TOP_R,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VanishXColors.Primary.copy(alpha = GLOW_PRIMARY_A),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * GLOW_BOTTOM_X, size.height * GLOW_BOTTOM_Y),
                        radius = size.minDimension * GLOW_BOTTOM_R,
                    ),
                )
            }
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        HomeTopBar(onSettings = { onAction(HomeAction.OpenSettings) })

        Column(
            modifier = Modifier.padding(horizontal = ContentPaddingH),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HeroCard(
                inviteDraft = uiState.inviteDraft,
                errorMessage = when {
                    uiState.inviteDraftEmpty -> stringResource(R.string.home_invite_empty)
                    else -> uiState.errorMessage
                },
                onInviteChange = { onAction(HomeAction.InviteDraftChanged(it)) },
                onCreate = { onAction(HomeAction.CreateRoom) },
                onJoin = { onAction(HomeAction.JoinFromDraft) },
                onScan = { onAction(HomeAction.ScanQr) },
            )

            uiState.shareHintUri?.let { uri ->
                ShareHintCard(
                    onCopy = { onCopyInvite(uri) },
                    onShare = { onShareInvite(uri) },
                    onDismiss = { onAction(HomeAction.DismissShareHint) },
                )
            }

            RecentSection(
                rooms = uiState.recentRooms,
                shownCount = uiState.recentRooms.size,
                totalCount = uiState.totalRoomCount,
                onOpen = { onAction(HomeAction.OpenRoom(it)) },
                onDelete = { pendingDelete = it },
                onSeeMore = { onAction(HomeAction.OpenHistory) },
            )

            if (uiState.showProStubToggle) {
                TextButton(
                    onClick = { onAction(HomeAction.ToggleProStub) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (uiState.isProStub) {
                                R.string.home_pro_stub_on
                            } else {
                                R.string.home_pro_stub_off
                            },
                        ),
                    )
                }
            }

            uiState.anonymousId?.let { id ->
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
}

@Composable
private fun HomeTopBar(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = ContentPaddingH, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = VanishXColors.Primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.15.sp,
            ),
            color = VanishXColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.home_settings),
                tint = VanishXColors.OnSurface,
            )
        }
    }
}

@Composable
private fun HeroCard(
    inviteDraft: String,
    errorMessage: String?,
    onInviteChange: (String) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        color = VanishXColors.Surface,
        border = BorderStroke(
            1.dp,
            VanishXColors.Primary.copy(alpha = HeroBorderAlpha),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ButtonCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.home_create_room),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.25.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inviteDraft,
                    onValueChange = onInviteChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.home_invite_hint),
                            color = VanishXColors.Muted,
                        )
                    },
                    shape = RoundedCornerShape(ButtonCorner),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VanishXColors.Primary,
                        unfocusedBorderColor = VanishXColors.Outline,
                        focusedTextColor = VanishXColors.OnSurface,
                        unfocusedTextColor = VanishXColors.OnSurface,
                        cursorColor = VanishXColors.Primary,
                    ),
                )
                OutlinedButton(
                    onClick = onJoin,
                    shape = RoundedCornerShape(ButtonCorner),
                    border = BorderStroke(1.dp, VanishXColors.Accent),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VanishXColors.Accent,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.home_join_go),
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.25.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = VanishXColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ButtonCorner),
                border = BorderStroke(1.dp, VanishXColors.Primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VanishXColors.Primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.home_scan_qr),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.25.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ShareHintCard(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        color = VanishXColors.Surface,
        border = BorderStroke(
            1.dp,
            VanishXColors.Accent.copy(alpha = 0.35f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_share_hint),
                style = MaterialTheme.typography.titleSmall,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopy) {
                    Text(text = stringResource(R.string.create_copy))
                }
                TextButton(onClick = onShare) {
                    Text(text = stringResource(R.string.create_share))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_back))
                }
            }
        }
    }
}

@Composable
@Suppress("UnstableCollections")
private fun RecentSection(
    rooms: List<HomeRoomItem>,
    shownCount: Int,
    totalCount: Int,
    onOpen: (String) -> Unit,
    onDelete: (HomeRoomItem) -> Unit,
    onSeeMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                ),
                color = VanishXColors.Muted,
                modifier = Modifier.weight(1f),
            )
            if (totalCount > 0) {
                Text(
                    text = stringResource(R.string.home_recent_count, shownCount, totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                )
            }
        }

        if (rooms.isEmpty()) {
            Text(
                text = stringResource(R.string.home_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rooms.forEachIndexed { index, room ->
                    SessionCard(
                        room = room,
                        accentEdge = index == 0 && !room.isExpired,
                        onOpen = { onOpen(room.id) },
                        onDelete = { onDelete(room) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSeeMore,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ButtonCorner),
            border = BorderStroke(1.dp, VanishXColors.Outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VanishXColors.Primary,
            ),
        ) {
            Text(
                text = stringResource(R.string.home_see_more),
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.25.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun SessionCard(
    room: HomeRoomItem,
    accentEdge: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(CardCorner))
            .background(VanishXColors.Surface)
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accentEdge) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(AccentEdgeWidth)
                    .background(VanishXColors.Accent),
            )
        } else {
            Spacer(modifier = Modifier.width(AccentEdgeWidth))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 13.dp),
        ) {
            Text(
                text = room.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = VanishXColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (room.isExpired) {
                    stringResource(R.string.badge_expired)
                } else {
                    stringResource(
                        R.string.badge_remaining,
                        formatRemainingMs(room.remainingMs),
                    )
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = if (room.isExpired) VanishXColors.Error else VanishXColors.Muted,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.home_delete_cd, room.displayName),
                tint = VanishXColors.Error,
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
                    HomeRoomItem("1", "Phòng #A9X2", 299_000, false, "creator"),
                    HomeRoomItem("2", "Kế hoạch cuối tuần", 20_400_000, false, "member"),
                ),
                totalRoomCount = 6,
                showProStubToggle = true,
            ),
            onAction = {},
            onCopyInvite = {},
            onShareInvite = {},
        )
    }
}
