@file:Suppress("MagicNumber", "ComplexMethod")

package com.vault.vanishx.presentation.mailbox.chat

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomComposer(
    draft: String,
    isSending: Boolean,
    onAction: (RoomAction) -> Unit,
    locked: Boolean = false,
    replySnippet: String? = null,
) {
    val inputEnabled = !isSending && !locked
    val canSend = inputEnabled && draft.isNotBlank()
    val view = LocalView.current
    val sendCd = stringResource(R.string.room_send_cd)
    val sendSensitiveCd = stringResource(R.string.room_send_sensitive_cd)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VanishXColors.Glass)
            .border(BorderStroke(1.dp, VanishXColors.GlassBorder.copy(alpha = 0.1f))),
    ) {
        if (!locked && !replySnippet.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.room_reply_banner),
                        style = MaterialTheme.typography.labelSmall,
                        color = VanishXColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = replySnippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = VanishXColors.Muted,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = { onAction(RoomAction.ClearReply) }) {
                    Text(
                        text = "✕",
                        color = VanishXColors.Muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 12.dp)
                    .alpha(if (locked) RoomUiDimens.composerLockedAlpha else 1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(RoomUiDimens.composerGap),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RoomUiDimens.composerPillRadius),
                    color = VanishXColors.Surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, VanishXColors.Outline.copy(alpha = 0.8f)),
                ) {
                    Row(
                        modifier = Modifier
                            .height(RoomUiDimens.composerFieldHeight)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { onAction(RoomAction.DraftChanged(it)) },
                            enabled = inputEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            textStyle = TextStyle(
                                color = VanishXColors.OnSurface,
                                fontSize = 15.sp,
                                letterSpacing = 0.15.sp,
                            ),
                            cursorBrush = SolidColor(VanishXColors.Primary),
                            singleLine = false,
                            maxLines = COMPOSER_MAX_LINES,
                            decorationBox = { inner ->
                                Box {
                                    if (draft.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.room_composer_hint),
                                            style = TextStyle(
                                                color = VanishXColors.Muted,
                                                fontSize = 15.sp,
                                            ),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(RoomUiDimens.sendButtonSize)
                        .clip(CircleShape)
                        .background(
                            if (canSend) {
                                VanishXColors.Primary
                            } else {
                                VanishXColors.Primary.copy(alpha = RoomUiDimens.sendDisabledAlpha)
                            },
                        )
                        .semantics {
                            contentDescription = "$sendCd. $sendSensitiveCd"
                        }
                        .then(
                            if (canSend) {
                                Modifier.pointerInput(draft) {
                                    detectTapGestures(
                                        onTap = { onAction(RoomAction.Send) },
                                        onLongPress = {
                                            view.performHapticFeedback(
                                                HapticFeedbackConstants.LONG_PRESS,
                                            )
                                            onAction(RoomAction.RequestSensitiveSend)
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = VanishXColors.OnPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (locked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            VanishXColors.Bg.copy(alpha = RoomUiDimens.composerLockedOverlayAlpha),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.room_composer_locked_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = VanishXColors.Muted,
                    )
                }
            }
        }
    }
}
