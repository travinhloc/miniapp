@file:Suppress("TooManyFunctions", "ComplexMethod")

package com.vault.vanishx.presentation.home

import com.vault.vanishx.presentation.icons.VanishXIcons

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.vault.vanishx.domain.model.ConversationPreview
import com.vault.vanishx.domain.model.ConversationPreviewKind
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.conversation.ConversationRow
import com.vault.vanishx.presentation.conversation.ConversationRowModel
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.mailbox.chat.RoomInviteSheet
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.VanishXTheme
import com.vault.vanishx.presentation.theme.vanishxScreenInsets

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
            viewModel.onAction(HomeAction.DismissWaitingInvite)
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
            viewModel.onAction(HomeAction.DismissWaitingInvite)
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
    var pendingDelete by remember { mutableStateOf<ConversationRowModel?>(null) }
    var plusMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

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

    uiState.waitingInviteUri?.let { uri ->
        RoomInviteSheet(
            inviteUri = uri,
            onCopy = onCopyInvite,
            onShare = onShareInvite,
            onDismiss = { onAction(HomeAction.DismissWaitingInvite) },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        HomeSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { onAction(HomeAction.SearchQueryChanged(it)) },
            onScanQr = { onAction(HomeAction.ScanQr) },
            plusExpanded = plusMenu,
            onPlusClick = { plusMenu = true },
            onPlusDismiss = { plusMenu = false },
            onCreate = {
                plusMenu = false
                onAction(HomeAction.CreateRoom)
            },
            onPaste = {
                plusMenu = false
                val text = clipboard.getText()?.text.orEmpty()
                onAction(HomeAction.PasteInvite(text))
            },
            onSettings = { onAction(HomeAction.OpenSettings) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    when (uiState.listFilter) {
                        HomeListFilter.All -> R.string.home_filter_all
                        HomeListFilter.Open -> R.string.home_filter_open
                        HomeListFilter.Expired -> R.string.home_filter_expired
                        HomeListFilter.Favorite -> R.string.home_filter_favorite
                    },
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VanishXColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            Box {
                TextButton(onClick = { filterMenu = true }) {
                    Text(stringResource(R.string.home_filter_all), color = VanishXColors.Muted)
                }
                DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                    HomeListFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        when (filter) {
                                            HomeListFilter.All -> R.string.home_filter_all
                                            HomeListFilter.Open -> R.string.home_filter_open
                                            HomeListFilter.Expired -> R.string.home_filter_expired
                                            HomeListFilter.Favorite -> R.string.home_filter_favorite
                                        },
                                    ),
                                )
                            },
                            onClick = {
                                filterMenu = false
                                onAction(HomeAction.SetFilter(filter))
                            },
                        )
                    }
                }
            }
        }

        uiState.pendingInviteUri?.let {
            HomePendingInviteBanner(
                onOpen = { onAction(HomeAction.OpenPendingInvite) },
                onDismiss = { onAction(HomeAction.DismissPendingInvite) },
            )
        }

        if (uiState.visibleRooms.isEmpty()) {
            HomeEmptyState(
                onCreate = { onAction(HomeAction.CreateRoom) },
                onPaste = {
                    val text = clipboard.getText()?.text.orEmpty()
                    onAction(HomeAction.PasteInvite(text))
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.visibleRooms, key = { it.id }) { room ->
                    SwipeDeleteConversation(
                        room = room,
                        onOpen = { onAction(HomeAction.OpenRoom(room.id)) },
                        onDelete = { pendingDelete = room },
                        onQr = if (room.isWaiting) {
                            { onAction(HomeAction.ShareWaiting(room.id)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.home_history_link),
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.Primary,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clickable { onAction(HomeAction.OpenHistory) },
        )

        if (uiState.showProStubToggle) {
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
    }
}

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onScanQr: () -> Unit,
    plusExpanded: Boolean,
    onPlusClick: () -> Unit,
    onPlusDismiss: () -> Unit,
    onCreate: () -> Unit,
    onPaste: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HomeCompactSearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        )
        IconButton(onClick = onScanQr) {
            Icon(
                imageVector = VanishXIcons.QrScan,
                contentDescription = stringResource(R.string.home_scan_qr),
                tint = VanishXColors.OnSurface,
            )
        }
        Box {
            IconButton(onClick = onPlusClick) {
                Icon(
                    VanishXIcons.Plus,
                    contentDescription = stringResource(R.string.home_menu_create),
                    tint = VanishXColors.OnSurface,
                )
            }
            DropdownMenu(expanded = plusExpanded, onDismissRequest = onPlusDismiss) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_menu_create)) },
                    onClick = onCreate,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_menu_paste)) },
                    onClick = onPaste,
                )
            }
        }
        IconButton(onClick = onSettings) {
            Icon(
                VanishXIcons.Settings,
                contentDescription = stringResource(R.string.home_settings),
                tint = VanishXColors.OnSurface,
            )
        }
    }
}

@Composable
private fun HomeCompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .background(VanishXColors.Surface, shape)
            .border(1.dp, VanishXColors.Outline, shape),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = VanishXColors.OnSurface),
        cursorBrush = SolidColor(VanishXColors.Primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = VanishXIcons.Search,
                    contentDescription = null,
                    tint = VanishXColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = VanishXColors.Muted,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun HomePendingInviteBanner(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(VanishXColors.Surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_pending_invite),
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpen) {
            Text(stringResource(R.string.home_pending_invite_open), color = VanishXColors.Primary)
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_cancel), color = VanishXColors.Muted)
        }
    }
}

@Composable
private fun HomeEmptyState(
    onCreate: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = VanishXColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCreate) {
            Text(stringResource(R.string.home_empty_create), color = VanishXColors.Primary)
        }
        TextButton(onClick = onPaste) {
            Text(stringResource(R.string.home_empty_paste), color = VanishXColors.Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteConversation(
    room: ConversationRowModel,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onQr: (() -> Unit)?,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VanishXColors.Error)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.home_delete_confirm),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    ) {
        ConversationRow(
            model = room,
            onClick = onOpen,
            onQrClick = onQr,
            modifier = Modifier.background(VanishXColors.Bg),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    VanishXTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                visibleRooms = listOf(
                    ConversationRowModel(
                        id = "1",
                        displayName = "Khách BĐS",
                        initials = "K",
                        avatarLocalPath = null,
                        preview = ConversationPreview(
                            kind = ConversationPreviewKind.Text,
                            snippet = "Gửi hợp đồng nhé",
                            lastActivityAt = 1L,
                            outbound = true,
                        ),
                        unreadCount = 2,
                        isFavorite = true,
                        isMuted = false,
                        isWaiting = false,
                        isExpired = false,
                        isLeft = false,
                        hasRoomClock = true,
                        ttlFraction = 0.82f,
                        remainingMs = 50_000_000L,
                    ),
                    ConversationRowModel(
                        id = "2",
                        displayName = "···0XpMoA",
                        initials = "..",
                        avatarLocalPath = null,
                        preview = ConversationPreview(kind = ConversationPreviewKind.Waiting),
                        unreadCount = 0,
                        isFavorite = false,
                        isMuted = false,
                        isWaiting = true,
                        isExpired = false,
                        isLeft = false,
                        hasRoomClock = false,
                        ttlFraction = 1f,
                        remainingMs = 0L,
                    ),
                ),
            ),
            onAction = {},
            onCopyInvite = {},
            onShareInvite = {},
        )
    }
}
