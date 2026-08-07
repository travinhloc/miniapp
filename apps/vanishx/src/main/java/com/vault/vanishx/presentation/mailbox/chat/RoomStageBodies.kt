package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.mailbox.RoomUiState
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomLoading(modifier: Modifier = Modifier) {
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
internal fun RoomExpiredFree(
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
internal fun RoomExpiredProArchive(
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
            expiresAt = uiState.room?.expiresAt?.takeIf { uiState.room?.hasRoomClock() == true },
            activatedAt = uiState.room?.activatedAt,
            isExpired = true,
            onOpenSafety = { onAction(RoomAction.OpenSafetySheet) },
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
internal fun RoomExpired(modifier: Modifier = Modifier) {
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
internal fun RoomLeft(modifier: Modifier = Modifier) {
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
internal fun FeedbackMessages(uiState: RoomUiState) {
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
