@file:Suppress("TooManyFunctions")

package com.vault.vanishx.presentation.mailbox

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.extensions.collectAsEffect

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions.spacingMedium),
    ) {
        RoomHeader(uiState = uiState, onAction = onAction)

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
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
        }

        FeedbackMessages(uiState = uiState)
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
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
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
private fun RoomHeader(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val showMenu = uiState.room?.status == MailboxRoom.STATUS_ACTIVE && !uiState.isExpired

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.room_title,
                    uiState.roomId.takeLast(ROOM_ID_DISPLAY_SUFFIX),
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (shouldShowTtl(uiState)) {
                val remaining = (uiState.room!!.expiresAt - System.currentTimeMillis())
                    .coerceAtLeast(0L)
                Text(
                    text = stringResource(R.string.room_ttl_remaining, formatRemaining(remaining)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (showMenu) {
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text(text = stringResource(R.string.room_menu))
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
                }
            }
        }
        TextButton(onClick = { onAction(RoomAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

private fun shouldShowTtl(uiState: RoomUiState): Boolean {
    val expiresAt = uiState.room?.expiresAt ?: return false
    return expiresAt > 0L && !uiState.isExpired &&
        uiState.room.status == MailboxRoom.STATUS_ACTIVE
}

@Composable
private fun RoomLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.room_loading))
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
            modifier = Modifier.padding(dimensions.spacingMedium),
        ) {
            Text(
                text = stringResource(R.string.room_locked_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = stringResource(R.string.room_locked_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            Button(onClick = { onAction(RoomAction.OpenPaywall) }) {
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
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions.spacingSmall),
            textAlign = TextAlign.Center,
        )
        RoomMessageList(
            messages = uiState.messages,
            isPro = true,
            isExpired = true,
            isRecalling = false,
            onAction = onAction,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { onAction(RoomAction.PingRoom) },
            enabled = !uiState.pingBusy,
            modifier = Modifier.fillMaxWidth(),
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
            )
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = stringResource(R.string.room_expired_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
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
            )
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = stringResource(R.string.room_left_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RoomActiveBody(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RoomMessageList(
            messages = uiState.messages,
            isPro = uiState.isPro,
            isExpired = uiState.isExpired,
            isRecalling = uiState.isRecalling,
            onAction = onAction,
            modifier = Modifier.weight(1f),
        )

        if (uiState.isSyncing) {
            Text(
                text = stringResource(R.string.room_syncing),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoomUiDimens.composerGap),
        ) {
            OutlinedTextField(
                value = uiState.draft,
                onValueChange = { onAction(RoomAction.DraftChanged(it)) },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSending,
                placeholder = { Text(text = stringResource(R.string.room_composer_hint)) },
                maxLines = COMPOSER_MAX_LINES,
            )
            Button(
                onClick = { onAction(RoomAction.Send) },
                enabled = !uiState.isSending && uiState.draft.isNotBlank(),
            ) {
                Text(text = stringResource(R.string.room_send))
            }
        }
        TextButton(
            onClick = { onAction(RoomAction.Refresh) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.room_retry_sync))
        }
    }
}

@Composable
@Suppress("UnstableCollections")
private fun RoomMessageList(
    messages: List<ChatMessage>,
    isPro: Boolean,
    isExpired: Boolean,
    isRecalling: Boolean,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    if (messages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(dimensions.spacingMedium),
            ) {
                Text(
                    text = stringResource(R.string.room_empty_e2e_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                Text(
                    text = stringResource(R.string.room_empty_e2e_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    canRecall = isPro &&
                        !isExpired &&
                        !isRecalling &&
                        message.direction == ChatMessage.DIRECTION_OUT &&
                        !message.recalled,
                    onRecall = { onAction(RoomAction.RecallMessage(message.id)) },
                )
            }
        }
    }
}

@Composable
private fun FeedbackMessages(uiState: RoomUiState) {
    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions.spacingSmall),
        )
    }
    uiState.infoMessage?.let { info ->
        Text(
            text = info,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions.spacingSmall),
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    canRecall: Boolean,
    onRecall: () -> Unit,
) {
    val mine = message.direction == ChatMessage.DIRECTION_OUT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = RoomUiDimens.bubbleMaxWidth)
                .clip(RoundedCornerShape(RoomUiDimens.bubbleCorner))
                .background(
                    if (mine) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .padding(dimensions.spacingSmall),
        ) {
            when {
                message.recalled -> {
                    Text(
                        text = stringResource(R.string.room_recalled_tag),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    if (mine) {
                        Spacer(modifier = Modifier.height(RoomUiDimens.sentTagGap))
                        Text(
                            text = "✓✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        )
                    }
                    if (canRecall) {
                        TextButton(onClick = onRecall) {
                            Text(text = stringResource(R.string.room_recall))
                        }
                    }
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalMinutes = ms / MS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return "%02d:%02d".format(hours, minutes)
}

private const val ROOM_ID_DISPLAY_SUFFIX = 6
private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val COMPOSER_MAX_LINES = 4
private const val REPORT_REASON_MAX_LINES = 3

private object RoomUiDimens {
    val composerGap = 8.dp
    val bubbleMaxWidth = 280.dp
    val bubbleCorner = 12.dp
    val sentTagGap = 4.dp
}
