@file:Suppress("MagicNumber", "LongMethod")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MediaAlbumItem
import com.vault.vanishx.domain.model.MediaAlbumState
import com.vault.vanishx.presentation.theme.VanishXColors

private val AlbumGutter = 2.dp
private val AlbumCorner = RoundedCornerShape(RoomUiDimens.mediaCorner)
private val TileCorner = RoundedCornerShape(2.dp)
private val BadgeScrim = Color(0x99000000)

/**
 * Zalo-like collage for multi visual media:
 * 1 → full · 2–3 → equal row · 4+ → first full-width, then rows of 3.
 * Per-tile pending spinner; tiles stay clickable while uploading.
 */
@Composable
internal fun AlbumMediaCollage(
    album: MediaAlbumState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = album.items
    Column(
        modifier = modifier
            .widthIn(max = RoomUiDimens.bubbleMaxWidth)
            .fillMaxWidth()
            .clip(AlbumCorner)
            .background(VanishXColors.Surface2),
        verticalArrangement = Arrangement.spacedBy(AlbumGutter),
    ) {
        when (items.size) {
            0 -> Unit
            1 -> AlbumTile(
                item = items[0],
                onClick = { onItemClick(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
            2, 3 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AlbumGutter),
                ) {
                    items.forEachIndexed { index, item ->
                        AlbumTile(
                            item = item,
                            onClick = { onItemClick(index) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.75f),
                        )
                    }
                }
            }
            else -> {
                AlbumTile(
                    item = items[0],
                    onClick = { onItemClick(0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                )
                albumGridIndices(items.size).forEach { rowIndices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AlbumGutter),
                    ) {
                        rowIndices.forEach { itemIndex ->
                            AlbumTile(
                                item = items[itemIndex],
                                onClick = { onItemClick(itemIndex) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Balances tiles after the full-width hero without creating blank placeholders. */
internal fun albumGridRowSizes(itemCount: Int): List<Int> {
    val remaining = (itemCount - 1).coerceAtLeast(0)
    if (remaining == 0) return emptyList()
    val fullRows = remaining / 3
    val remainder = remaining % 3
    return when {
        remainder == 0 -> List(fullRows) { 3 }
        remainder == 2 -> List(fullRows) { 3 } + 2
        fullRows == 0 -> listOf(1)
        // Avoid a lone final tile: turn the final 3 + 1 into 2 + 2.
        else -> List(fullRows - 1) { 3 } + listOf(2, 2)
    }
}

internal fun albumGridIndices(itemCount: Int): List<List<Int>> {
    var nextIndex = 1
    return albumGridRowSizes(itemCount).map { rowSize ->
        List(rowSize) { offset -> nextIndex + offset }.also { nextIndex += rowSize }
    }
}

@Composable
private fun AlbumTile(
    item: MediaAlbumItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(TileCorner)
            .background(VanishXColors.Surface)
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.previewPath),
            contentDescription = stringResource(R.string.room_media_image_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.kind == AttachmentMeta.KIND_VIDEO) {
                val duration = item.durationMs.takeIf { it > 0L }
                if (duration != null) {
                    Text(
                        text = MediaPreviewLoader.formatDuration(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BadgeScrim)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
        when (item.status) {
            ChatMessage.MEDIA_PENDING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                }
            }
            ChatMessage.MEDIA_FAILED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.room_media_failed),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
