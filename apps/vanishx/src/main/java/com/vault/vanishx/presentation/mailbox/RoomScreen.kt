package com.vault.vanishx.presentation.mailbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.miniapp.core.ui.theme.AppTheme.dimensions
import com.vault.vanishx.R
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            uiState.isLoading -> {
                Text(text = stringResource(R.string.room_loading))
            }
            uiState.isExpired -> {
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
            else -> {
                Text(
                    text = stringResource(R.string.room_title, uiState.roomId.takeLast(6)),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                val remaining = uiState.room?.expiresAt?.let { expiresAt ->
                    (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
                }
                if (remaining != null && uiState.room?.expiresAt != 0L) {
                    Text(
                        text = stringResource(
                            R.string.room_ttl_remaining,
                            formatRemaining(remaining),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                Text(
                    text = stringResource(R.string.room_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        TextButton(onClick = { onAction(RoomAction.Back) }) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return "%02d:%02d".format(hours, minutes)
}
