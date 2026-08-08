@file:Suppress("MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.stringResource
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
    draftSensitive: Boolean = false,
) {
    val inputEnabled = !isSending && !locked
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VanishXColors.Glass)
            .border(BorderStroke(1.dp, VanishXColors.GlassBorder.copy(alpha = 0.1f))),
    ) {
        if (!locked) {
            FilterChip(
                selected = draftSensitive,
                onClick = { onAction(RoomAction.ToggleDraftSensitive) },
                enabled = inputEnabled,
                label = {
                    Text(
                        text = stringResource(R.string.room_composer_sensitive),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VanishXColors.NeonAmber.copy(alpha = 0.2f),
                    selectedLabelColor = VanishXColors.NeonAmber,
                    selectedLeadingIconColor = VanishXColors.NeonAmber,
                ),
                modifier = Modifier.padding(start = 10.dp, top = 6.dp),
            )
        }
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 12.dp)
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
                IconButton(
                    onClick = { onAction(RoomAction.Send) },
                    enabled = inputEnabled && draft.isNotBlank(),
                    modifier = Modifier
                        .size(RoomUiDimens.sendButtonSize)
                        .clip(CircleShape)
                        .background(
                            if (inputEnabled && draft.isNotBlank()) {
                                VanishXColors.Primary
                            } else {
                                VanishXColors.Primary.copy(alpha = RoomUiDimens.sendDisabledAlpha)
                            },
                        ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.room_send_cd),
                        tint = VanishXColors.OnPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (locked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(VanishXColors.Bg.copy(alpha = RoomUiDimens.composerLockedOverlayAlpha)),
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
