@file:Suppress("ComplexMethod", "MatchingDeclarationName", "MagicNumber", "ComplexCondition")

package com.vault.vanishx.presentation.conversation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.ConversationPreview
import com.vault.vanishx.domain.model.ConversationPreviewKind
import com.vault.vanishx.presentation.mailbox.chat.RoomAvatar
import com.vault.vanishx.presentation.theme.VanishXColors
import java.text.DateFormat
import java.util.Date

data class ConversationRowModel(
    val id: String,
    val displayName: String,
    val initials: String,
    val avatarLocalPath: String?,
    val preview: ConversationPreview,
    val unreadCount: Int,
    val isFavorite: Boolean,
    val isMuted: Boolean,
    val isWaiting: Boolean,
    val isExpired: Boolean,
    val isLeft: Boolean,
    val hasRoomClock: Boolean,
    val ttlFraction: Float,
    val remainingMs: Long,
)

private val RowHeight = 72.dp
private val AvatarSize = 48.dp
private val RingSize = 56.dp
private val UnreadBadgeSize = 20.dp
private const val TTL_WARN_MS = 3_600_000L
private const val TTL_DANGER_MS = 900_000L

@Composable
fun ConversationRow(
    model: ConversationRowModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTtlRing: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onQrClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConversationLeading(
            model = model,
            showTtlRing = showTtlRing,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = VanishXColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (model.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.room_options_favorite),
                        tint = VanishXColors.NeonAmber,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = previewLabel(model.preview),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = if (model.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                ),
                color = when {
                    model.isWaiting -> VanishXColors.Warn
                    model.unreadCount > 0 -> VanishXColors.OnSurface
                    else -> VanishXColors.Muted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val timeLabel = formatListTime(model.preview.lastActivityAt)
            if (timeLabel.isNotEmpty()) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = if (model.unreadCount > 0) VanishXColors.Primary else VanishXColors.Muted,
                )
            }
            when {
                trailing != null -> trailing()
                model.isWaiting && onQrClick != null -> {
                    IconButton(
                        onClick = onQrClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.home_invite_qr),
                            tint = VanishXColors.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                model.unreadCount > 0 -> {
                    Box(
                        modifier = Modifier
                            .size(UnreadBadgeSize)
                            .clip(CircleShape)
                            .background(VanishXColors.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (model.unreadCount > 9) "9+" else model.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationLeading(
    model: ConversationRowModel,
    showTtlRing: Boolean,
) {
    val ringColor = ttlRingColor(model.remainingMs, model.isExpired, model.hasRoomClock)
    Box(
        modifier = Modifier.size(if (showTtlRing && model.hasRoomClock) RingSize else AvatarSize),
        contentAlignment = Alignment.Center,
    ) {
        if (showTtlRing && model.hasRoomClock) {
            TtlRing(
                fraction = model.ttlFraction,
                color = ringColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        RoomAvatar(
            letter = model.initials,
            size = AvatarSize,
            imagePath = model.avatarLocalPath,
        )
    }
}

@Composable
private fun TtlRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
            Canvas(modifier = modifier) {
                val strokeWidth = 2.75.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = VanishXColors.Outline.copy(alpha = 0.9f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
}

@Composable
private fun previewLabel(preview: ConversationPreview): String {
    val base = when (preview.kind) {
        ConversationPreviewKind.Waiting -> stringResource(R.string.conv_preview_waiting)
        ConversationPreviewKind.Expired -> stringResource(R.string.conv_preview_expired)
        ConversationPreviewKind.Recalled -> stringResource(R.string.conv_preview_recalled)
        ConversationPreviewKind.Sensitive -> stringResource(R.string.conv_preview_sensitive)
        ConversationPreviewKind.Image -> stringResource(R.string.conv_preview_image)
        ConversationPreviewKind.File -> stringResource(R.string.conv_preview_file)
        ConversationPreviewKind.Video -> stringResource(R.string.conv_preview_video)
        ConversationPreviewKind.Left -> stringResource(R.string.conv_preview_left)
        ConversationPreviewKind.Empty -> stringResource(R.string.conv_preview_empty)
        ConversationPreviewKind.Text -> preview.snippet.orEmpty()
    }
    return if (preview.kind == ConversationPreviewKind.Text && preview.outbound && base.isNotBlank()) {
        stringResource(R.string.conv_preview_you, base)
    } else {
        base
    }
}

private fun formatListTime(atMs: Long): String {
    if (atMs <= 0L) return ""
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(atMs))
}

private fun ttlRingColor(remainingMs: Long, isExpired: Boolean, hasClock: Boolean): Color = when {
    !hasClock -> VanishXColors.Primary
    isExpired || remainingMs <= TTL_DANGER_MS -> VanishXColors.Error
    remainingMs < TTL_WARN_MS -> VanishXColors.Warn
    else -> VanishXColors.Primary
}
