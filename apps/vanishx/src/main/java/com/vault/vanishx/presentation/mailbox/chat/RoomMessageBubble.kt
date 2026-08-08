@file:Suppress("ComplexMethod", "MagicNumber", "UnstableCollections", "MatchingDeclarationName")

package com.vault.vanishx.presentation.mailbox.chat

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import kotlinx.coroutines.launch

internal data class ReplyQuoteUi(
    val parentExists: Boolean,
    val snippet: String,
)

@Composable
internal fun RoomMessageBubble(
    message: ChatMessage,
    showAura: Boolean = false,
    auraIntensity: Float = 0f,
    reactionCounts: Map<String, Int> = emptyMap(),
    replyQuote: ReplyQuoteUi? = null,
    readReceipt: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onReplyQuoteClick: (() -> Unit)? = null,
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
    val scope = rememberCoroutineScope()
    val burnFlash = remember(message.id) { Animatable(1f) }
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = burnFlash.value }
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
                .pointerInput(message.id, message.sensitive, message.recalled, onLongPress) {
                    if (message.sensitive && !message.recalled) {
                        // Hold-to-read only. Do not set onLongPress here — Compose cancels
                        // onPress after longPressTimeout when onLongPress is registered,
                        // so users could only peek ~500ms before the action sheet stole focus.
                        detectTapGestures(
                            onPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                revealed = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    revealed = false
                                }
                            },
                        )
                    } else {
                        detectTapGestures(
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLongPress?.invoke()
                            },
                            onDoubleTap = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                scope.launch {
                                    burnFlash.snapTo(0.35f)
                                    burnFlash.animateTo(1f, tween(450))
                                }
                            },
                        )
                    }
                }
                .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        ) {
            if (replyQuote != null && !message.recalled) {
                Text(
                    text = if (replyQuote.parentExists) {
                        replyQuote.snippet
                    } else {
                        stringResource(R.string.room_reply_missing)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontStyle = if (replyQuote.parentExists) FontStyle.Normal else FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = if (mine) {
                        VanishXColors.OnPrimary.copy(alpha = 0.75f)
                    } else {
                        VanishXColors.Muted
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (mine) {
                                VanishXColors.OnPrimary.copy(alpha = 0.12f)
                            } else {
                                VanishXColors.Outline.copy(alpha = 0.35f)
                            },
                        )
                        .then(
                            if (replyQuote.parentExists && onReplyQuoteClick != null) {
                                Modifier.clickable(onClick = onReplyQuoteClick)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .padding(bottom = 4.dp),
                )
            }
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
                message.sensitive -> {
                    // Stable layout: body slot + hold hint + meta — avoid branch swap height jump
                    SensitiveBody(
                        body = message.body,
                        revealed = showPlain,
                        mine = mine,
                        supportsBlur = supportsBlur,
                    )
                    Text(
                        text = stringResource(R.string.room_sensitive_hold),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (mine) {
                            VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.timeAlpha)
                        } else {
                            VanishXColors.NeonAmber
                        },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .graphicsLayer { alpha = if (showPlain) 0f else 1f },
                    )
                    BubbleMetaRow(
                        mine = mine,
                        sentAt = message.sentAt,
                        sensitive = true,
                        readReceipt = readReceipt,
                        // Actions: long-press time/badge (not the body — body is hold-to-read)
                        onLongPress = onLongPress,
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
                    BubbleMetaRow(
                        mine = mine,
                        sentAt = message.sentAt,
                        sensitive = false,
                        readReceipt = readReceipt,
                    )
                }
            }
            if (reactionCounts.isNotEmpty()) {
                ReactionCountsRow(
                    counts = reactionCounts,
                    mine = mine,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SensitiveBody(
    body: String,
    revealed: Boolean,
    mine: Boolean,
    supportsBlur: Boolean,
) {
    val bodyColor = if (mine) VanishXColors.OnPrimary else VanishXColors.OnSurface
    val bubbleFill = if (mine) VanishXColors.Primary else RoomUiDimens.bubbleInColor
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
    if (revealed) {
        Text(text = body, style = bodyStyle, color = bodyColor)
        return
    }
    if (supportsBlur) {
        // Offscreen layer + strong blur + scrim so large bubbles stay unreadable
        Box {
            Text(
                text = body,
                style = bodyStyle,
                color = bodyColor,
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .blur(sensitiveBlurRadius),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(bubbleFill.copy(alpha = 0.55f)),
            )
        }
    } else {
        // Pre-31: never paint real plaintext under a translucent scrim
        Text(
            text = stringResource(R.string.room_sensitive_masked),
            style = bodyStyle.copy(letterSpacing = 2.sp),
            color = if (mine) {
                VanishXColors.OnPrimary.copy(alpha = 0.75f)
            } else {
                VanishXColors.Muted
            },
        )
    }
}

@Composable
private fun ReactionCountsRow(
    counts: Map<String, Int>,
    mine: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        counts.entries
            .sortedByDescending { it.value }
            .forEach { (emoji, count) ->
                Text(
                    text = if (count > 1) "$emoji $count" else emoji,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = if (mine) {
                        VanishXColors.OnPrimary.copy(alpha = 0.9f)
                    } else {
                        VanishXColors.OnSurface
                    },
                )
            }
    }
}

private val sensitiveBlurRadius = 28.dp

@Composable
private fun BubbleMetaRow(
    mine: Boolean,
    sentAt: Long,
    sensitive: Boolean,
    readReceipt: Boolean = false,
    onLongPress: (() -> Unit)? = null,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .then(
                if (onLongPress != null) {
                    Modifier.pointerInput(onLongPress) {
                        detectTapGestures(
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLongPress()
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
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
                text = stringResource(
                    if (readReceipt) R.string.room_sent_checks else R.string.room_sent_check,
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = VanishXColors.OnPrimary.copy(alpha = RoomUiDimens.checkAlpha),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
