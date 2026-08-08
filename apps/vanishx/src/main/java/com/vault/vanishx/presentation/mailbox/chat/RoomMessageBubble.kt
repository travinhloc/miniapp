@file:Suppress("ComplexMethod", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
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
        RoomUiDimens.auraThresholdFraction).coerceIn(0f, 1f) * RoomUiDimens.auraMaxAlpha
    val view = LocalView.current
    var revealed by remember(message.id) { mutableStateOf(false) }
    val showPlain = !message.sensitive || revealed || message.recalled

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
                .then(
                    if (message.sensitive && !message.recalled) {
                        Modifier.pointerInput(message.id) {
                            detectTapGestures(
                                onPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    revealed = true
                                    tryAwaitRelease()
                                    revealed = false
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
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
                showPlain -> {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        ),
                        color = if (mine) VanishXColors.OnPrimary else VanishXColors.OnSurface,
                    )
                    BubbleMetaRow(mine = mine, sentAt = message.sentAt, sensitive = message.sensitive)
                }
                else -> {
                    Text(
                        text = stringResource(R.string.room_sensitive_masked),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                        ),
                        color = if (mine) {
                            VanishXColors.OnPrimary.copy(alpha = 0.75f)
                        } else {
                            VanishXColors.Muted
                        },
                    )
                    Text(
                        text = stringResource(R.string.room_sensitive_hold),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (mine) {
                            VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.timeAlpha)
                        } else {
                            VanishXColors.NeonAmber
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleMetaRow(
    mine: Boolean,
    sentAt: Long,
    sensitive: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sensitive) {
            Text(
                text = stringResource(R.string.room_sensitive_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (mine) {
                    VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.timeAlpha)
                } else {
                    VanishXColors.NeonAmber
                },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(
            text = formatMessageTime(sentAt),
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
