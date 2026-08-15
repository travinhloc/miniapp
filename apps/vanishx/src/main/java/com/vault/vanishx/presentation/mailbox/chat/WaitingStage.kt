@file:Suppress("ComplexCondition", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.qr.QrBitmapEncoder
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun WaitingStage(
    room: MailboxRoom?,
    inviteUri: String?,
    onAction: (RoomAction) -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCreator = room?.role == MailboxRoom.ROLE_CREATOR
    val titleLetter = resolveAvatarLetter(resolveRoomTitle(room))
    val titlePulse = rememberInfiniteTransition(label = "waitingTitle")
    val titleGlow by titlePulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "titleGlow",
    )
    val qrEncoder = remember { QrBitmapEncoder() }
    val qrBitmap = remember(inviteUri) {
        inviteUri?.let { qrEncoder.encode(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(RoomUiDimens.spacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(RoomUiDimens.cardCorner),
            color = VanishXColors.Surface2,
            border = BorderStroke(1.dp, VanishXColors.NeonAmber.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                RoomAvatar(
                    letter = titleLetter,
                    pulse = true,
                    size = RoomUiDimens.waitingHeroAvatar,
                    neonColor = VanishXColors.NeonAmber,
                    imagePath = room?.avatarLocalPath,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.room_handshake_waiting_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    textAlign = TextAlign.Center,
                    color = VanishXColors.NeonAmber.copy(alpha = 0.65f + titleGlow * 0.35f),
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            color = VanishXColors.NeonAmber.copy(alpha = 0.12f * titleGlow),
                            radius = size.maxDimension * 0.9f,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        if (isCreator) {
                            R.string.room_handshake_waiting_body_creator
                        } else {
                            R.string.room_handshake_waiting_body_member
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    textAlign = TextAlign.Center,
                    color = VanishXColors.Muted,
                    modifier = Modifier.widthIn(max = RoomUiDimens.handshakeBannerTextWidth),
                )

                if (isCreator && inviteUri != null && qrBitmap != null) {
                    Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VanishXColors.NeonAmber.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, VanishXColors.NeonAmber.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.room_handshake_nudge),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            ),
                            color = VanishXColors.OnSurface.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.room_handshake_qr_cd),
                        modifier = Modifier
                            .size(RoomUiDimens.waitingQrSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable { onAction(RoomAction.OpenInviteSheet) }
                            .padding(10.dp),
                    )
                    Spacer(modifier = Modifier.height(RoomUiDimens.spacingMedium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RoomUiDimens.spacingSmall),
                    ) {
                        Button(
                            onClick = { onCopyInvite(inviteUri) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VanishXColors.Primary,
                                contentColor = VanishXColors.OnPrimary,
                            ),
                        ) {
                            Text(text = stringResource(R.string.room_handshake_copy_link))
                        }
                        OutlinedButton(
                            onClick = { onShareInvite(inviteUri) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, VanishXColors.Outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = VanishXColors.OnSurface,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(text = stringResource(R.string.create_share))
                        }
                    }
                    TextButton(onClick = { onAction(RoomAction.OpenInviteSheet) }) {
                        Text(text = stringResource(R.string.room_handshake_show_qr))
                    }
                }

                Spacer(modifier = Modifier.height(RoomUiDimens.spacingSmall))
                OutlinedButton(
                    onClick = { onAction(RoomAction.PingPeer) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, VanishXColors.Outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VanishXColors.Primary),
                ) {
                    Text(text = stringResource(R.string.room_handshake_ping))
                }
            }
        }
    }
}
