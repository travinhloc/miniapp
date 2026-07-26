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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.room_title, uiState.roomId.takeLast(6)),
                    style = MaterialTheme.typography.titleLarge,
                )
                val remaining = uiState.room?.expiresAt?.let { expiresAt ->
                    (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
                }
                if (remaining != null && (uiState.room?.expiresAt ?: 0L) > 0L && !uiState.isExpired) {
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

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.room_loading))
                }
            }
            uiState.isExpired -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
            else -> {
                val listState = rememberLazyListState()
                LaunchedEffect(uiState.messages.size) {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.lastIndex)
                    }
                }
                if (uiState.messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = dimensions.spacingSmall),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                    }
                }

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.draft,
                        onValueChange = { onAction(RoomAction.DraftChanged(it)) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSending,
                        placeholder = { Text(text = stringResource(R.string.room_composer_hint)) },
                        maxLines = 4,
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
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
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
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return "%02d:%02d".format(hours, minutes)
}
