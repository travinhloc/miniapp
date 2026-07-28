@file:Suppress("TooManyFunctions", "ComplexMethod", "ComplexCondition")

package com.vault.vanishx.presentation.mailbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.util.formatRemainingMs
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun RoomScreen(
    viewModel: RoomViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    RoomContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun RoomContent(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
) {
    val showComposer = !uiState.isLoading &&
        uiState.room?.status == MailboxRoom.STATUS_ACTIVE &&
        !uiState.isExpired

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg),
    ) {
        ChatTopBar(uiState = uiState, onAction = onAction)

        when {
            uiState.isLoading -> RoomLoading(modifier = Modifier.weight(1f))
            uiState.room?.status == MailboxRoom.STATUS_LEFT -> RoomLeft(modifier = Modifier.weight(1f))
            uiState.isExpired && !uiState.isPro -> RoomExpiredFree(
                modifier = Modifier.weight(1f),
                onAction = onAction,
            )
            uiState.isExpired && uiState.isPro -> RoomExpiredProArchive(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
            uiState.isExpired -> RoomExpired(modifier = Modifier.weight(1f))
            else -> RoomActiveBody(
                uiState = uiState,
                modifier = Modifier.weight(1f),
            )
        }

        FeedbackMessages(uiState = uiState)

        if (showComposer) {
            ChatComposer(
                draft = uiState.draft,
                isSending = uiState.isSending,
                onAction = onAction,
            )
        }
    }

    if (uiState.showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(RoomAction.DismissBlockConfirm) },
            title = { Text(text = stringResource(R.string.room_block_title)) },
            text = { Text(text = stringResource(R.string.room_block_body)) },
            confirmButton = {
                TextButton(onClick = { onAction(RoomAction.ConfirmBlock) }) {
                    Text(text = stringResource(R.string.room_block_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(RoomAction.DismissBlockConfirm) }) {
                    Text(text = stringResource(R.string.action_back))
                }
            },
        )
    }

    if (uiState.showReportDialog) {
        AlertDialog(
            onDismissRequest = { onAction(RoomAction.DismissReport) },
            title = { Text(text = stringResource(R.string.room_report_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.room_report_body))
                    Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
                    OutlinedTextField(
                        value = uiState.reportReason,
                        onValueChange = { onAction(RoomAction.ReportReasonChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(text = stringResource(R.string.room_report_reason_hint))
                        },
                        maxLines = REPORT_REASON_MAX_LINES,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(RoomAction.SubmitReport) },
                    enabled = !uiState.isReporting,
                ) {
                    Text(text = stringResource(R.string.room_report_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(RoomAction.DismissReport) }) {
                    Text(text = stringResource(R.string.action_back))
                }
            },
        )
    }

    if (uiState.showPingConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(RoomAction.DismissPing) },
            title = { Text(text = stringResource(R.string.room_ping_title)) },
            text = { Text(text = stringResource(R.string.room_ping_body)) },
            confirmButton = {
                TextButton(onClick = { onAction(RoomAction.PingRoom) }) {
                    Text(text = stringResource(R.string.room_ping_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(RoomAction.DismissPing) }) {
                    Text(text = stringResource(R.string.action_back))
                }
            },
        )
    }
}

@Composable
private fun ChatTopBar(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val showMenu = uiState.room?.status == MailboxRoom.STATUS_ACTIVE && !uiState.isExpired
    val title = resolveRoomTitle(uiState.room)
    val recallableMessage = remember(uiState.messages, uiState.isPro, uiState.isExpired, uiState.isRecalling) {
        findRecallableMessage(
            messages = uiState.messages,
            isPro = uiState.isPro,
            isExpired = uiState.isExpired,
            isRecalling = uiState.isRecalling,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VanishXColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RoomUiDimens.topBarHeight)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(RoomAction.Back) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }

            RoomAvatar(letter = resolveAvatarLetter(title))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.15.sp,
                    ),
                    color = VanishXColors.OnSurface,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        R.string.join_preview_room_id,
                        uiState.roomId.takeLast(ROOM_ID_DISPLAY_SUFFIX).uppercase(),
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp,
                    ),
                    color = VanishXColors.Primary,
                    maxLines = 1,
                )
            }

            if (showMenu) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.room_menu_cd),
                            tint = VanishXColors.OnSurface,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.room_block)) },
                            onClick = {
                                menuExpanded = false
                                onAction(RoomAction.OpenBlockConfirm)
                            },
                            enabled = !uiState.isBlocking,
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.room_report)) },
                            onClick = {
                                menuExpanded = false
                                onAction(RoomAction.OpenReport)
                            },
                            enabled = !uiState.isReporting,
                        )
                        if (recallableMessage != null) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.room_recall)) },
                                onClick = {
                                    menuExpanded = false
                                    onAction(RoomAction.RecallMessage(recallableMessage.id))
                                },
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            color = VanishXColors.Outline.copy(alpha = RoomUiDimens.dividerAlpha),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun RoomAvatar(letter: String) {
    Box(
        modifier = Modifier
            .size(RoomUiDimens.avatarSize)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(VanishXColors.Primary, VanishXColors.Accent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun RoomLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.room_loading),
            color = VanishXColors.Muted,
        )
    }
}

@Composable
private fun RoomExpiredFree(
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(RoomUiDimens.spacingMedium),
        ) {
            Text(
                text = stringResource(R.string.room_locked_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            Text(
                text = stringResource(R.string.room_locked_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
            Button(
                onClick = { onAction(RoomAction.OpenPaywall) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Text(text = stringResource(R.string.history_open_pro))
            }
            TextButton(onClick = { onAction(RoomAction.Back) }) {
                Text(text = stringResource(R.string.room_back_history))
            }
        }
    }
}

@Composable
private fun RoomExpiredProArchive(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.room_archive_banner),
            style = MaterialTheme.typography.labelLarge,
            color = VanishXColors.Accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RoomUiDimens.spacingSmall),
            textAlign = TextAlign.Center,
        )
        RoomMessageList(
            messages = uiState.messages,
            expiresAt = uiState.room?.expiresAt,
            isExpired = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { onAction(RoomAction.PingRoom) },
            enabled = !uiState.pingBusy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoomUiDimens.spacingMedium),
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.room_ping))
        }
    }
}

@Composable
private fun RoomExpired(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.room_expired_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            Text(
                text = stringResource(R.string.room_expired_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = VanishXColors.Muted,
            )
        }
    }
}

@Composable
private fun RoomLeft(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.room_left_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
            Text(
                text = stringResource(R.string.room_left_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = VanishXColors.Muted,
            )
        }
    }
}

@Composable
private fun RoomActiveBody(
    uiState: RoomUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RoomMessageList(
            messages = uiState.messages,
            expiresAt = uiState.room?.expiresAt,
            isExpired = uiState.isExpired,
            modifier = Modifier.weight(1f),
        )

        if (uiState.isSyncing) {
            Text(
                text = stringResource(R.string.room_syncing),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RoomUiDimens.spacingMedium, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@Suppress("UnstableCollections")
private fun RoomMessageList(
    messages: List<ChatMessage>,
    expiresAt: Long?,
    isExpired: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val showTtl = !isExpired && expiresAt != null && expiresAt > 0L

    LaunchedEffect(showTtl, expiresAt) {
        if (!showTtl) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(TTL_TICK_MS)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex + if (showTtl) 1 else 0)
        }
    }

    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = RoomUiDimens.spacingMedium),
            contentAlignment = Alignment.Center,
        ) {
            E2eEmptyCard()
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = RoomUiDimens.spacingMedium,
                vertical = RoomUiDimens.spacingSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showTtl && expiresAt != null) {
                item(key = "ttl-chip") {
                    TtlChip(remainingMs = (expiresAt - nowMs).coerceAtLeast(0L))
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }
    }
}

@Composable
private fun E2eEmptyCard() {
    Surface(
        shape = RoundedCornerShape(RoomUiDimens.cardCorner),
        color = VanishXColors.Surface2,
        border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = RoomUiDimens.e2eBorderAlpha)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = RoomUiDimens.e2eLockTint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.room_empty_e2e_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.room_empty_e2e_body),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                textAlign = TextAlign.Center,
                color = VanishXColors.Muted,
            )
        }
    }
}

@Composable
private fun TtlChip(remainingMs: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VanishXColors.Accent.copy(alpha = RoomUiDimens.ttlBgAlpha),
            border = BorderStroke(1.dp, VanishXColors.Accent.copy(alpha = RoomUiDimens.ttlBorderAlpha)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.room_ttl_chip, formatRemainingMs(remainingMs)),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    ),
                    color = VanishXColors.Accent,
                )
            }
        }
    }
}

@Composable
private fun ChatComposer(
    draft: String,
    isSending: Boolean,
    onAction: (RoomAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(RoomUiDimens.composerGap),
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(RoomUiDimens.composerPillRadius),
            color = VanishXColors.Surface,
            border = BorderStroke(1.dp, VanishXColors.Outline),
        ) {
            Row(
                modifier = Modifier
                    .height(RoomUiDimens.composerFieldHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { onAction(RoomAction.DraftChanged(it)) },
                    enabled = !isSending,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textStyle = TextStyle(
                        color = VanishXColors.OnSurface,
                        fontSize = 15.sp,
                        letterSpacing = 0.15.sp,
                    ),
                    cursorBrush = SolidColor(VanishXColors.Primary),
                    singleLine = false,
                    maxLines = COMPOSER_MAX_LINES,
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.room_composer_hint),
                                    style = TextStyle(
                                        color = VanishXColors.Muted,
                                        fontSize = 15.sp,
                                    ),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }
        IconButton(
            onClick = { onAction(RoomAction.Send) },
            enabled = !isSending && draft.isNotBlank(),
            modifier = Modifier
                .size(RoomUiDimens.sendButtonSize)
                .clip(CircleShape)
                .background(
                    if (!isSending && draft.isNotBlank()) {
                        VanishXColors.Primary
                    } else {
                        VanishXColors.Primary.copy(alpha = RoomUiDimens.sendDisabledAlpha)
                    },
                ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.room_send_cd),
                tint = VanishXColors.OnPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun FeedbackMessages(uiState: RoomUiState) {
    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = VanishXColors.Error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoomUiDimens.spacingMedium, vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
    uiState.infoMessage?.let { info ->
        Text(
            text = info,
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.Muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoomUiDimens.spacingMedium, vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val mine = message.direction == ChatMessage.DIRECTION_OUT
    val bubbleShape = if (mine) {
        RoundedCornerShape(
            topStart = RoomUiDimens.bubbleCorner,
            topEnd = RoomUiDimens.bubbleCorner,
            bottomStart = RoomUiDimens.bubbleCorner,
            bottomEnd = RoomUiDimens.bubbleTailCorner,
        )
    } else {
        RoundedCornerShape(
            topStart = RoomUiDimens.bubbleCorner,
            topEnd = RoomUiDimens.bubbleCorner,
            bottomStart = RoomUiDimens.bubbleTailCorner,
            bottomEnd = RoomUiDimens.bubbleCorner,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = RoomUiDimens.bubbleMaxWidth)
                .clip(bubbleShape)
                .background(
                    if (mine) VanishXColors.Primary else RoomUiDimens.bubbleInColor,
                )
                .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        ) {
            when {
                message.recalled -> {
                    Text(
                        text = stringResource(R.string.room_recalled_tag),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = if (mine) {
                            VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.recalledAlpha)
                        } else {
                            VanishXColors.Muted
                        },
                    )
                }
                else -> {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        ),
                        color = if (mine) VanishXColors.OnPrimary else VanishXColors.OnSurface,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatMessageTime(message.sentAt),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (mine) {
                                VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.timeAlpha)
                            } else {
                                VanishXColors.Muted
                            },
                        )
                        if (mine) {
                            Text(
                                text = stringResource(R.string.room_sent_checks),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.checkAlpha),
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun findRecallableMessage(
    messages: List<ChatMessage>,
    isPro: Boolean,
    isExpired: Boolean,
    isRecalling: Boolean,
): ChatMessage? {
    if (!isPro || isExpired || isRecalling) return null
    return messages.lastOrNull { it.direction == ChatMessage.DIRECTION_OUT && !it.recalled }
}

private fun resolveRoomTitle(room: MailboxRoom?): String {
    if (room == null) return ""
    return room.nickname?.takeIf { it.isNotBlank() }
        ?: room.title?.takeIf { it.isNotBlank() }
        ?: "···${room.id.takeLast(ROOM_ID_DISPLAY_SUFFIX)}"
}

private fun resolveAvatarLetter(title: String): String =
    title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

private fun formatMessageTime(epochMs: Long): String {
    val format = DateFormat.getTimeInstance(DateFormat.SHORT)
    return format.format(Date(epochMs))
}

private const val ROOM_ID_DISPLAY_SUFFIX = 6
private const val COMPOSER_MAX_LINES = 4
private const val REPORT_REASON_MAX_LINES = 3
private const val TTL_TICK_MS = 1_000L

private object RoomUiDimens {
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val composerGap = 8.dp
    val bubbleMaxWidth = 280.dp
    val bubbleCorner = 16.dp
    val bubbleTailCorner = 4.dp
    val cardCorner = 16.dp
    val topBarHeight = 56.dp
    val avatarSize = 36.dp
    val composerPillRadius = 24.dp
    val composerFieldHeight = 48.dp
    val sendButtonSize = 48.dp
    val bubbleInColor = Color(0xFF21262D)
    val e2eLockTint = Color(0xFF5B9FFF)
    const val dividerAlpha = 0.6f
    const val e2eBorderAlpha = 0.1f
    const val ttlBgAlpha = 0.12f
    const val ttlBorderAlpha = 0.25f
    const val timeAlpha = 0.55f
    const val checkAlpha = 0.85f
    const val recalledAlpha = 0.7f
    const val sendDisabledAlpha = 0.35f
}
