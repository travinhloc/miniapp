@file:Suppress("TooManyFunctions", "MagicNumber")

package com.vault.vanishx.presentation.home

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalClipboardManager
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
import kotlin.math.min

private val CardCorner = 16.dp
private val VaultCorner = 20.dp
private val ButtonCorner = 12.dp
private val QuickCorner = 10.dp
private val ContentPaddingH = 16.dp
private val AccentEdgeWidth = 3.dp
private val TtlRingSize = 44.dp
private val RadarSize = 148.dp
private const val GLOW_TOP_X = 0.5f
private const val GLOW_TOP_Y = -0.2f
private const val GLOW_TOP_R = 0.85f
private const val GLOW_BOTTOM_X = 0.1f
private const val GLOW_BOTTOM_Y = 0.9f
private const val GLOW_BOTTOM_R = 0.7f
private const val GLOW_ACCENT_A = 0.12f
private const val GLOW_PRIMARY_A = 0.08f
private const val TTL_WARN_BELOW = 0.5f
private const val TTL_ERROR_BELOW = 0.12f

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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        HomeTopBar(onSettings = { onAction(HomeAction.OpenSettings) })

        Column(
            modifier = Modifier.padding(horizontal = ContentPaddingH),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VaultControlCenter(
                vaporizedToday = uiState.vaporizedToday,
                errorMessage = when {
                    uiState.inviteDraftEmpty -> stringResource(R.string.home_invite_empty)
                    else -> uiState.errorMessage
                },
                onCreate = { onAction(HomeAction.CreateRoom) },
                onPaste = { text -> onAction(HomeAction.PasteInvite(text)) },
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
private fun VaultControlCenter(
    vaporizedToday: Int,
    errorMessage: String?,
    onCreate: () -> Unit,
    onPaste: (String) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VaultCorner),
            color = VanishXColors.Surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = 0.14f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RadarPulse()

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ButtonCorner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanishXColors.Primary.copy(alpha = 0.92f),
                        contentColor = VanishXColors.OnPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_create_room),
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = clipboard.getText()?.text
                            onPaste(text.orEmpty())
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(QuickCorner),
                        border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VanishXColors.Primary,
                            containerColor = VanishXColors.Surface.copy(alpha = 0.55f),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Create,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_paste_link),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = onScan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(QuickCorner),
                        border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VanishXColors.Primary,
                            containerColor = VanishXColors.Surface.copy(alpha = 0.55f),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_scan_qr),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = VanishXColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        PrivacyWidget(vaporizedToday = vaporizedToday)
    }
}

@Composable
private fun RadarPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweepRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Box(
        modifier = modifier.size(RadarSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = min(size.width, size.height)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val stroke = Stroke(width = 1.5.dp.toPx())

            rotate(ringRotation, Offset(cx, cy)) {
                drawCircle(
                    color = VanishXColors.Primary.copy(alpha = 0.18f),
                    radius = minDim / 2f,
                    center = Offset(cx, cy),
                    style = stroke,
                )
            }
            rotate(-ringRotation * 0.7f, Offset(cx, cy)) {
                drawCircle(
                    color = VanishXColors.Accent.copy(alpha = 0.22f),
                    radius = minDim / 2f - 14.dp.toPx(),
                    center = Offset(cx, cy),
                    style = stroke,
                )
            }
            drawCircle(
                color = VanishXColors.Primary.copy(alpha = 0.28f),
                radius = minDim / 2f - 28.dp.toPx(),
                center = Offset(cx, cy),
                style = stroke,
            )

            rotate(sweepRotation, Offset(cx, cy)) {
                val sweepRadius = minDim / 2f - 8.dp.toPx()
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.08f to VanishXColors.Primary.copy(alpha = 0.28f),
                        0.2f to Color.Transparent,
                        1f to Color.Transparent,
                        center = Offset(cx, cy),
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(cx - sweepRadius, cy - sweepRadius),
                    size = Size(sweepRadius * 2f, sweepRadius * 2f),
                )
            }

            val coreR = (minDim / 2f - 42.dp.toPx()) * breathe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VanishXColors.Primary.copy(alpha = 0.35f),
                        VanishXColors.Accent.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = coreR,
                ),
                radius = coreR,
                center = Offset(cx, cy),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = VanishXColors.Primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_radar_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                ),
                color = VanishXColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_radar_sub),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.3.sp,
                ),
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrivacyWidget(
    vaporizedToday: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        color = VanishXColors.Surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, VanishXColors.Accent.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = vaporizedToday.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = VanishXColors.Primary,
                )
                Text(
                    text = stringResource(R.string.home_privacy_vaporized),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                    color = VanishXColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = VanishXColors.Primary,
                )
                Text(
                    text = stringResource(R.string.home_privacy_local),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                    color = VanishXColors.Muted,
                    textAlign = TextAlign.Center,
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rooms.forEach { room ->
                    SessionCard(
                        room = room,
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
            shape = RoundedCornerShape(8.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionCard(
    room: HomeRoomItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringColor = ttlRingColor(room.ttlFraction, room.isExpired)
    val remainingLabel = when {
        room.isExpired -> stringResource(R.string.badge_expired)
        room.hasRoomClock -> stringResource(R.string.badge_remaining, formatRemainingMs(room.remainingMs))
        room.isWaiting -> stringResource(R.string.badge_waiting_activate)
        else -> stringResource(R.string.badge_no_expiry)
    }
    val swipeHint = stringResource(R.string.home_swipe_delete_hint)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCorner))
            .background(VanishXColors.Surface.copy(alpha = 0.88f))
            .drawBehind {
                if (room.isWaiting) {
                    drawRect(
                        color = VanishXColors.Warn,
                        size = Size(AccentEdgeWidth.toPx(), size.height),
                    )
                }
            }
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onDelete,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TtlRing(
            fraction = room.ttlFraction.coerceIn(0f, 1f),
            initials = room.initials,
            color = ringColor,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = room.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = VanishXColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (room.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.room_options_favorite),
                        tint = VanishXColors.NeonAmber,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (room.isWaiting) {
                    Text(
                        text = stringResource(R.string.home_waiting_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                        ),
                        color = VanishXColors.Warn,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VanishXColors.Warn.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = "$remainingLabel · $swipeHint",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = if (room.isExpired) VanishXColors.Error else VanishXColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TtlRing(
    fraction: Float,
    initials: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(TtlRingSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = VanishXColors.Outline.copy(alpha = 0.9f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(VanishXColors.Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.Primary,
            )
        }
    }
}

private fun ttlRingColor(fraction: Float, isExpired: Boolean): Color = when {
    isExpired || fraction <= TTL_ERROR_BELOW -> VanishXColors.Error
    fraction < TTL_WARN_BELOW -> VanishXColors.Warn
    else -> VanishXColors.Primary
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    VanishXTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                anonymousId = "vx_demo",
                vaporizedToday = 12,
                recentRooms = listOf(
                    HomeRoomItem(
                        id = "1",
                        displayName = "Phòng #A9X2",
                        remainingMs = 299_000,
                        isExpired = false,
                        role = "creator",
                        isWaiting = true,
                        ttlFraction = 0.82f,
                        initials = "A9",
                    ),
                    HomeRoomItem(
                        id = "2",
                        displayName = "Kế hoạch cuối tuần",
                        remainingMs = 2_880_000,
                        isExpired = false,
                        role = "member",
                        isWaiting = false,
                        ttlFraction = 0.18f,
                        initials = "K",
                    ),
                    HomeRoomItem(
                        id = "3",
                        displayName = "Meet nhanh",
                        remainingMs = 720_000,
                        isExpired = false,
                        role = "member",
                        isWaiting = false,
                        ttlFraction = 0.06f,
                        initials = "M",
                    ),
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
