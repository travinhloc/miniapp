@file:Suppress("ComplexMethod", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomMessageBubble(
    message: ChatMessage,
    showAura: Boolean = false,
    auraIntensity: Float = 0f,
) {
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
    val auraAlpha = ((auraIntensity - (1f - RoomUiDimens.auraThresholdFraction)) /
        RoomUiDimens.auraThresholdFraction).coerceIn(0f, 1f) * 0.55f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showAura) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VanishXColors.NeonAmber.copy(alpha = auraAlpha),
                                    Color.Transparent,
                                ),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                RoomUiDimens.bubbleCorner.toPx(),
                            ),
                        )
                    }
                } else {
                    Modifier
                },
            ),
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
