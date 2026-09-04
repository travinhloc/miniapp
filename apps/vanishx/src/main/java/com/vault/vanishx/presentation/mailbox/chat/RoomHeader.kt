@file:Suppress("ComplexMethod", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.mailbox.RoomHandshakeStatus
import com.vault.vanishx.presentation.mailbox.RoomUiState
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomHeader(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    handshakeStatus: RoomHandshakeStatus,
) {
    val showMenu = !uiState.isLoading &&
        uiState.room?.status == MailboxRoom.STATUS_ACTIVE &&
        !uiState.isExpired
    val title = resolveRoomTitle(uiState.room)
    val isWaiting = handshakeStatus == RoomHandshakeStatus.WAITING
    val isLive = handshakeStatus == RoomHandshakeStatus.LIVE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VanishXColors.Glass)
            .border(BorderStroke(1.dp, VanishXColors.GlassBorder.copy(alpha = RoomUiDimens.glassBorderAlpha))),
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
                    imageVector = VanishXIcons.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }

            if (uiState.isLoading || uiState.room == null) {
                HeaderIdentitySkeleton()
            } else {
                RoomAvatar(
                    letter = resolveAvatarLetter(title),
                    pulse = isWaiting,
                    neonColor = if (isWaiting) VanishXColors.NeonAmber else VanishXColors.NeonGreen,
                    imagePath = uiState.room?.avatarLocalPath,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clickable { onAction(RoomAction.OpenRenameDialog) },
                ) {
                    Text(
                        text = title.ifBlank { stringResource(R.string.room_rename_placeholder) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.15.sp,
                        ),
                        color = VanishXColors.OnSurface,
                        maxLines = 1,
                    )
                    if (isLive) {
                        val presenceLabel = when {
                            uiState.peerTyping -> stringResource(R.string.room_typing)
                            uiState.peerOnline == true -> stringResource(R.string.room_peer_online)
                            else -> stringResource(R.string.room_peer_away)
                        }
                        Text(
                            text = presenceLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                letterSpacing = 0.25.sp,
                            ),
                            color = when {
                                uiState.peerTyping -> VanishXColors.NeonAmber
                                uiState.peerOnline == true -> VanishXColors.NeonGreen
                                else -> VanishXColors.Muted
                            },
                            maxLines = 1,
                        )
                    }
                }
            }

            if (showMenu) {
                IconButton(onClick = { onAction(RoomAction.OpenRoomOptions) }) {
                    Icon(
                        imageVector = VanishXIcons.DotsVertical,
                        contentDescription = stringResource(R.string.room_menu_cd),
                        tint = VanishXColors.OnSurface,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
        HorizontalDivider(
            color = when {
                uiState.isLoading -> VanishXColors.Outline.copy(alpha = RoomUiDimens.dividerAlpha)
                isWaiting -> VanishXColors.NeonAmber.copy(alpha = RoomUiDimens.headerNeonDividerWaiting)
                isLive -> VanishXColors.NeonGreen.copy(alpha = RoomUiDimens.headerNeonDividerLive)
                else -> VanishXColors.Outline.copy(alpha = RoomUiDimens.dividerAlpha)
            },
            thickness = 1.dp,
        )
    }
}

@Composable
private fun RowScope.HeaderIdentitySkeleton() {
    Box(
        modifier = Modifier
            .size(RoomUiDimens.avatarSize)
            .clip(CircleShape)
            .background(VanishXColors.Surface2),
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VanishXColors.Surface2),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VanishXColors.Surface2.copy(alpha = 0.7f)),
        )
    }
}
