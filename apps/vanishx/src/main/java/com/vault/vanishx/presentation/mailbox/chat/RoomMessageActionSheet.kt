@file:Suppress("MagicNumber", "ComplexMethod")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.RecallPolicy
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.mailbox.RoomUiState
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxSheetInsets

private val reactionEmojis = listOf("❤️", "👍", "😂", "😮", "😢", "😡")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomMessageActionSheet(
    uiState: RoomUiState,
    message: ChatMessage,
    onAction: (RoomAction) -> Unit,
) {
    val mine = message.direction == ChatMessage.DIRECTION_OUT
    val canRecall = mine &&
        !message.recalled &&
        !uiState.isExpired &&
        RecallPolicy.canRecallOutbound(message.sentAt, uiState.isPro)
    val showRecallPaywall = mine &&
        !message.recalled &&
        !uiState.isExpired &&
        !RecallPolicy.canRecallOutbound(message.sentAt, uiState.isPro)
    val canDelete = mine && !message.recalled
    val canCopy = !message.recalled && message.body.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { onAction(RoomAction.DismissMessageActions) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .vanishxSheetInsets()
                .padding(horizontal = RoomUiDimens.spacingMedium)
                .padding(bottom = RoomUiDimens.spacingMedium),
        ) {
            ReactionBar(
                onPick = { emoji ->
                    onAction(RoomAction.ReactToMessage(message.id, emoji))
                    onAction(RoomAction.DismissMessageActions)
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionCell(
                    icon = VanishXIcons.ArrowBack,
                    label = stringResource(R.string.room_action_reply),
                    onClick = {
                        onAction(RoomAction.ReplyToMessage(message.id))
                        onAction(RoomAction.DismissMessageActions)
                    },
                )
                if (canRecall || showRecallPaywall) {
                    ActionCell(
                        icon = VanishXIcons.Refresh,
                        label = stringResource(R.string.room_action_recall),
                        onClick = {
                            if (canRecall) {
                                onAction(RoomAction.RecallMessage(message.id))
                            } else {
                                onAction(RoomAction.OpenPaywall)
                            }
                            onAction(RoomAction.DismissMessageActions)
                        },
                    )
                }
                ActionCell(
                    icon = VanishXIcons.Share,
                    label = stringResource(R.string.room_action_copy),
                    enabled = canCopy,
                    onClick = {
                        onAction(RoomAction.CopyMessage(message.id))
                        onAction(RoomAction.DismissMessageActions)
                    },
                )
                ActionCell(
                    icon = VanishXIcons.Information,
                    label = stringResource(R.string.room_action_details),
                    onClick = {
                        onAction(RoomAction.OpenMessageDetails(message.id))
                        onAction(RoomAction.DismissMessageActions)
                    },
                )
            }
            if (canDelete) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ActionCell(
                        icon = VanishXIcons.Delete,
                        label = stringResource(R.string.room_action_delete),
                        danger = true,
                        onClick = {
                            onAction(RoomAction.OpenDeleteForMe(message.id))
                            onAction(RoomAction.DismissMessageActions)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReactionPickerBar(
    onPick: (String) -> Unit,
    selected: String? = null,
) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(24.dp),
        color = VanishXColors.Surface2,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            reactionEmojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .clickable { onPick(emoji) }
                        .padding(6.dp)
                        .then(
                            if (emoji == selected) {
                                Modifier.background(
                                    VanishXColors.Primary.copy(alpha = 0.18f),
                                    RoundedCornerShape(12.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun ReactionBar(onPick: (String) -> Unit) {
    ReactionPickerBar(onPick = onPick)
}

@Composable
private fun ActionCell(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> VanishXColors.Muted.copy(alpha = 0.4f)
                danger -> VanishXColors.Error
                else -> VanishXColors.OnSurface
            },
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = when {
                !enabled -> VanishXColors.Muted.copy(alpha = 0.4f)
                danger -> VanishXColors.Error
                else -> VanishXColors.OnSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
