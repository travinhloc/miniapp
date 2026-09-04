@file:Suppress("MagicNumber", "UnstableCollections")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.presentation.theme.VanishXColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val GridGutter = 2.dp
private val TileCorner = RoundedCornerShape(2.dp)

@Composable
internal fun GallerySheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(CircleShape)
            .background(VanishXColors.Muted.copy(alpha = 0.35f)),
    )
}

@Composable
internal fun GalleryAccentLink(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = if (enabled) VanishXColors.Primary else VanishXColors.Muted,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
internal fun GalleryMediaGrid(
    recent: List<GalleryMediaItem>,
    selected: List<Uri>,
    enabled: Boolean,
    useCameraIcon: Boolean,
    onTakePhoto: () -> Unit,
    onToggleItem: (GalleryMediaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(GridGutter),
        verticalArrangement = Arrangement.spacedBy(GridGutter),
    ) {
        item(key = "take_photo") {
            GalleryTakePhotoTile(
                enabled = enabled,
                useCameraIcon = useCameraIcon,
                onClick = onTakePhoto,
            )
        }
        items(recent, key = { it.uri.toString() }) { item ->
            val index = selected.indexOf(item.uri)
            GalleryMediaTile(
                item = item,
                selected = index >= 0,
                selectionIndex = if (index >= 0) index + 1 else null,
                enabled = enabled,
                onClick = { onToggleItem(item) },
            )
        }
    }
}

@Composable
internal fun GalleryTakePhotoTile(
    enabled: Boolean,
    useCameraIcon: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(TileCorner)
            .background(VanishXColors.Surface2)
            .border(1.dp, VanishXColors.Outline.copy(alpha = 0.55f), TileCorner)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (useCameraIcon) {
                Icon(
                    imageVector = VanishXIcons.Camera,
                    contentDescription = null,
                    tint = VanishXColors.OnSurface,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Icon(
                    imageVector = VanishXIcons.Plus,
                    contentDescription = null,
                    tint = VanishXColors.OnSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.room_gallery_take_photo),
                color = VanishXColors.OnSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
internal fun GalleryMediaTile(
    item: GalleryMediaItem,
    selected: Boolean,
    selectionIndex: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(TileCorner)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (item.isVideo) {
            GalleryVideoThumbnail(
                item = item,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (item.isVideo && item.durationMs > 0L) {
            Text(
                text = MediaPreviewLoader.formatDuration(item.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(TileCorner)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(VanishXColors.Primary)
                    } else {
                        Modifier
                            .background(Color.Transparent)
                            .border(1.5.dp, Color.White, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected && selectionIndex != null) {
                Text(
                    text = selectionIndex.toString(),
                    color = VanishXColors.OnPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun GalleryVideoThumbnail(
    item: GalleryMediaItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var frame by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.uri) {
        frame = withContext(Dispatchers.IO) {
            loadGalleryVideoThumbnail(context, item)
        }
    }
    val bitmap = frame
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(VanishXColors.Surface2),
        )
    }
}

@Composable
internal fun GallerySendFab(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (enabled) VanishXColors.Primary else VanishXColors.Primary.copy(alpha = 0.45f),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = VanishXIcons.Send,
            contentDescription = stringResource(R.string.room_gallery_send),
            tint = VanishXColors.OnPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}
