@file:Suppress("MagicNumber", "LongMethod")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.mailbox.RoomUiState
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets

@Composable
internal fun RoomOptionsScreen(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
    onPickAvatar: () -> Unit,
) {
    val room = uiState.room
    val title = resolveRoomTitle(room)
    val mediaItems = uiState.messages.filter { msg ->
        msg.isMedia &&
            !msg.mediaLocalPath.isNullOrBlank() &&
            msg.mediaTransferStatus != ChatMessage.MEDIA_FAILED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RoomUiDimens.topBarHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(RoomAction.DismissRoomOptions) }) {
                Icon(
                    imageVector = VanishXIcons.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }
            Text(
                text = stringResource(R.string.room_options_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VanishXColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = VanishXColors.Outline.copy(alpha = 0.35f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RoomUiDimens.spacingMedium)
                .padding(bottom = RoomUiDimens.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPickAvatar),
                contentAlignment = Alignment.Center,
            ) {
                RoomAvatar(
                    letter = resolveAvatarLetter(title),
                    imagePath = room?.avatarLocalPath,
                    size = 88.dp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.room_options_change_avatar),
                style = MaterialTheme.typography.labelMedium,
                color = VanishXColors.Primary,
                modifier = Modifier.clickable(onClick = onPickAvatar),
            )
            if (!room?.avatarLocalPath.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.room_options_reset_avatar),
                    style = MaterialTheme.typography.labelSmall,
                    color = VanishXColors.Muted,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onAction(RoomAction.ResetRoomAvatar) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title.ifBlank { stringResource(R.string.room_rename_placeholder) },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = VanishXColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.join_preview_room_id,
                    uiState.roomId.takeLast(ROOM_ID_DISPLAY_SUFFIX).uppercase(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = VanishXColors.Muted,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickAction(
                    icon = VanishXIcons.Search,
                    label = stringResource(R.string.room_options_search),
                    onClick = {
                        onAction(RoomAction.DismissRoomOptions)
                        onAction(RoomAction.OpenRoomSearch)
                    },
                )
                QuickAction(
                    icon = VanishXIcons.Calendar,
                    label = stringResource(R.string.room_options_wallpaper),
                    onClick = { onAction(RoomAction.OpenWallpaperSheet) },
                )
                QuickAction(
                    icon = VanishXIcons.Alert,
                    label = stringResource(
                        if (room?.muted == true) {
                            R.string.room_options_unmute
                        } else {
                            R.string.room_options_mute
                        },
                    ),
                    onClick = { onAction(RoomAction.ToggleRoomMuted) },
                )
                QuickAction(
                    icon = VanishXIcons.Star,
                    label = stringResource(R.string.room_options_favorite),
                    onClick = { onAction(RoomAction.ToggleRoomFavorite) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            OptionsSectionLabel(stringResource(R.string.room_options_media_section))
            if (mediaItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.room_options_media_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    mediaItems.take(MEDIA_STRIP_MAX).forEach { msg ->
                        MediaStripItem(
                            message = msg,
                            onClick = { onAction(RoomAction.OpenMediaViewer(msg.id)) },
                        )
                    }
                    MediaSeeAllButton(
                        onClick = { onAction(RoomAction.OpenMediaLibrary) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OptionsRow(
                title = stringResource(R.string.room_options_nickname),
                subtitle = title.ifBlank { stringResource(R.string.room_rename_placeholder) },
                onClick = { onAction(RoomAction.OpenRenameDialog) },
            )
            OptionsRow(
                title = stringResource(R.string.room_options_favorite),
                trailing = {
                    Switch(
                        checked = room?.favorite == true,
                        onCheckedChange = { onAction(RoomAction.ToggleRoomFavorite) },
                    )
                },
            )
            OptionsRow(
                title = stringResource(R.string.room_options_mute),
                trailing = {
                    Switch(
                        checked = room?.muted == true,
                        onCheckedChange = { onAction(RoomAction.ToggleRoomMuted) },
                    )
                },
            )

            Spacer(modifier = Modifier.height(12.dp))
            OptionsSectionLabel(stringResource(R.string.room_options_safety_section))
            OptionsRow(
                title = stringResource(R.string.room_bento_fingerprint),
                subtitle = stringResource(R.string.room_bento_fingerprint_sub),
                onClick = {
                    onAction(RoomAction.DismissRoomOptions)
                    onAction(RoomAction.OpenSafetySheet)
                },
            )
            OptionsRow(
                title = stringResource(R.string.room_bento_block),
                subtitle = stringResource(R.string.room_block_body),
                danger = true,
                onClick = {
                    onAction(RoomAction.DismissRoomOptions)
                    onAction(RoomAction.OpenBlockConfirm)
                },
            )
            OptionsRow(
                title = stringResource(R.string.room_bento_report),
                subtitle = stringResource(R.string.room_report_body),
                onClick = {
                    onAction(RoomAction.DismissRoomOptions)
                    onAction(RoomAction.OpenReport)
                },
            )
            OptionsRow(
                title = stringResource(R.string.room_bento_burn),
                subtitle = stringResource(R.string.room_bento_burn_sub),
                danger = true,
                onClick = {
                    onAction(RoomAction.DismissRoomOptions)
                    onAction(RoomAction.OpenBurnConfirm)
                },
            )
        }
    }
}

@Composable
private fun MediaStripItem(message: ChatMessage, onClick: () -> Unit) {
    MediaThumbnail(
        message = message,
        modifier = Modifier
            .size(MediaStripTile)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    )
}

@Composable
private fun MediaSeeAllButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(MediaStripTile)
            .clip(RoundedCornerShape(8.dp))
            .background(VanishXColors.Primary.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = VanishXIcons.ArrowForward,
            contentDescription = stringResource(R.string.room_options_media_see_all),
            tint = VanishXColors.Primary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(VanishXColors.Surface2)
                .border(1.dp, VanishXColors.Outline.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = VanishXColors.OnSurface)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = VanishXColors.Muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun OptionsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = VanishXColors.Muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun OptionsRow(
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (danger) VanishXColors.Error else VanishXColors.OnSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VanishXColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                imageVector = VanishXIcons.ArrowForward,
                contentDescription = null,
                tint = VanishXColors.Muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(color = VanishXColors.Outline.copy(alpha = 0.25f))
}

private val MediaStripTile = 64.dp
private const val MEDIA_STRIP_MAX = 11
