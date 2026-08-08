@file:Suppress("ComplexMethod", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    isWaitingForPeer: Boolean,
    handshakeStatus: RoomHandshakeStatus,
) {
    val showMenu = uiState.room?.status == MailboxRoom.STATUS_ACTIVE && !uiState.isExpired
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }

            RoomAvatar(
                letter = resolveAvatarLetter(title),
                pulse = isWaiting,
                neonColor = if (isWaiting) VanishXColors.NeonAmber else VanishXColors.NeonGreen,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isWaitingForPeer) {
                        WaitingBadge(onClick = { onAction(RoomAction.OpenInviteSheet) })
                    }
                    if (isLive) {
                        LivePill()
                    }
                }
            }

            if (showMenu) {
                IconButton(onClick = { onAction(RoomAction.OpenBentoSheet) }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.room_menu_cd),
                        tint = VanishXColors.OnSurface,
                    )
                }
            }
        }
        HorizontalDivider(
            color = when {
                isWaiting -> VanishXColors.NeonAmber.copy(alpha = RoomUiDimens.headerNeonDividerWaiting)
                isLive -> VanishXColors.NeonGreen.copy(alpha = RoomUiDimens.headerNeonDividerLive)
                else -> VanishXColors.Outline.copy(alpha = RoomUiDimens.dividerAlpha)
            },
            thickness = 1.dp,
        )
    }
}

@Composable
private fun WaitingBadge(onClick: () -> Unit) {
    WaitingBadgeGlow { glow ->
        Text(
            text = stringResource(R.string.room_waiting_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = VanishXColors.NeonAmber,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    VanishXColors.NeonAmber.copy(
                        alpha = RoomUiDimens.waitingBadgeBgBase + glow * RoomUiDimens.waitingBadgeBgGlow,
                    ),
                )
                .border(
                    1.dp,
                    VanishXColors.NeonAmber.copy(
                        alpha = RoomUiDimens.waitingBadgeBorderBase +
                            glow * RoomUiDimens.waitingBadgeBorderGlow,
                    ),
                    RoundedCornerShape(6.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun LivePill() {
    val transition = rememberInfiniteTransition(label = "livePill")
    val glow by transition.animateFloat(
        initialValue = RoomUiDimens.liveGlowMin,
        targetValue = RoomUiDimens.liveGlowMax,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = RoomUiDimens.liveGlowDurationMs,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveGlow",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                VanishXColors.NeonGreen.copy(
                    alpha = RoomUiDimens.livePillBgBase + glow * RoomUiDimens.livePillBgGlow,
                ),
            )
            .border(
                1.dp,
                VanishXColors.NeonGreen.copy(
                    alpha = RoomUiDimens.livePillBorderBase + glow * RoomUiDimens.livePillBorderGlow,
                ),
                RoundedCornerShape(20.dp),
            )
            .drawBehind {
                drawCircle(
                    color = VanishXColors.NeonGreen.copy(
                        alpha = RoomUiDimens.livePillHaloAlpha * glow,
                    ),
                    radius = size.maxDimension * RoomUiDimens.livePillHaloScale,
                    center = center.copy(x = 10.dp.toPx()),
                )
            }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(VanishXColors.NeonGreen),
        )
        Text(
            text = stringResource(R.string.room_live_pill),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = VanishXColors.NeonGreen,
            maxLines = 1,
        )
    }
}
