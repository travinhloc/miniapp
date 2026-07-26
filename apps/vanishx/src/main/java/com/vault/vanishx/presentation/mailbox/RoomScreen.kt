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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
            uiState.isExpired -> RoomExpired(modifier = Modifier.weight(1f))
            else -> RoomActiveBody(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
        }

        FeedbackMessages(uiState = uiState)
    }
}

@Composable
private fun RoomHeader(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
) {
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
        TextButton(onClick = { onAction(RoomAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

private fun shouldShowTtl(uiState: RoomUiState): Boolean {
    val expiresAt = uiState.room?.expiresAt ?: return false
    return expiresAt > 0L && !uiState.isExpired
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
private fun RoomActiveBody(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RoomMessageList(
            messages = uiState.messages,
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
            Text(
                text = stringResource(R.string.room_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
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
private fun MessageBubble(message: ChatMessage) {
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
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .padding(dimensions.spacingSmall),
        ) {
            if (mine) {
                Text(
                    text = stringResource(R.string.room_sent_tag),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(RoomUiDimens.sentTagGap))
            }
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
            )
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

private object RoomUiDimens {
    val composerGap = 8.dp
    val bubbleMaxWidth = 280.dp
    val bubbleCorner = 12.dp
    val sentTagGap = 4.dp
}
