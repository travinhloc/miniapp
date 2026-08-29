@file:Suppress("MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.mailbox.RoomUiState
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RoomUiDimens.spacingMedium, vertical = RoomUiDimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) { index ->
            val mine = index % 2 == 1
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (mine) 0.62f else 0.72f)
                        .height(if (index == 2) 72.dp else 44.dp)
                        .clip(RoundedCornerShape(RoomUiDimens.bubbleCorner))
                        .background(VanishXColors.Surface2.copy(alpha = 0.85f)),
                )
            }
        }
    }
}

@Composable
internal fun RoomExpiredFree(
    onNeedPro: () -> Unit,
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
                onClick = onNeedPro,
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
        if (error == "media_unsupported" || error == "media_too_large") return@let
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
            text = when (info) {
                "ttl_stub" -> stringResource(R.string.room_bento_ttl_stub)
                else -> info
            },
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.Muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoomUiDimens.spacingMedium, vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}
