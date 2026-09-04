@file:Suppress("MagicNumber", "UnstableCollections", "LongMethod", "TooManyFunctions")

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets

private enum class MediaLibraryTab {
    Photos,
    Files,
    Voice,
}

@Composable
internal fun RoomMediaLibraryScreen(
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onOpenMessage: (String) -> Unit,
) {
    val buckets = remember(messages) { MediaLibraryBuckets.from(messages) }
    var tab by remember { mutableIntStateOf(0) }
    val selected = MediaLibraryTab.entries.getOrElse(tab) { MediaLibraryTab.Photos }
    val items = when (selected) {
        MediaLibraryTab.Photos -> buckets.photos
        MediaLibraryTab.Files -> buckets.files
        MediaLibraryTab.Voice -> buckets.voice
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        MediaLibraryTopBar(onBack = onBack)
        MediaLibraryTabs(
            tab = tab,
            photoCount = buckets.photos.size,
            fileCount = buckets.files.size,
            voiceCount = buckets.voice.size,
            onTabSelected = { tab = it },
        )
        MediaLibraryBody(
            selected = selected,
            items = items,
            onOpenMessage = onOpenMessage,
        )
    }
}

@Composable
private fun MediaLibraryTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RoomUiDimens.topBarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = VanishXIcons.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = VanishXColors.OnSurface,
            )
        }
        Text(
            text = stringResource(R.string.room_media_library_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = VanishXColors.OnSurface,
        )
    }
}

@Composable
private fun MediaLibraryTabs(
    tab: Int,
    photoCount: Int,
    fileCount: Int,
    voiceCount: Int,
    onTabSelected: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = tab,
        containerColor = VanishXColors.Surface,
        contentColor = VanishXColors.Primary,
        edgePadding = RoomUiDimens.spacingMedium,
    ) {
        Tab(
            selected = tab == 0,
            onClick = { onTabSelected(0) },
            text = {
                Text(text = stringResource(R.string.room_media_library_tab_media, photoCount))
            },
        )
        Tab(
            selected = tab == 1,
            onClick = { onTabSelected(1) },
            text = {
                Text(text = stringResource(R.string.room_media_library_tab_docs, fileCount))
            },
        )
        Tab(
            selected = tab == 2,
            onClick = { onTabSelected(2) },
            text = {
                Text(text = stringResource(R.string.room_media_library_tab_audio, voiceCount))
            },
        )
    }
}

@Composable
private fun MediaLibraryBody(
    selected: MediaLibraryTab,
    items: List<ChatMessage>,
    onOpenMessage: (String) -> Unit,
) {
    when {
        items.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(
                    when (selected) {
                        MediaLibraryTab.Photos -> R.string.room_media_library_empty_media
                        MediaLibraryTab.Files -> R.string.room_media_library_empty_docs
                        MediaLibraryTab.Voice -> R.string.room_media_library_empty_audio
                    },
                ),
                color = VanishXColors.Muted,
            )
        }
        selected == MediaLibraryTab.Voice -> LazyColumn(
            contentPadding = PaddingValues(RoomUiDimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { msg ->
                VoiceLibraryRow(message = msg, onClick = { onOpenMessage(msg.id) })
            }
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(RoomUiDimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItems(items, key = { it.id }) { msg ->
                MediaLibraryCell(message = msg, onClick = { onOpenMessage(msg.id) })
            }
        }
    }
}

@Composable
private fun MediaLibraryCell(message: ChatMessage, onClick: () -> Unit) {
    MediaThumbnail(
        message = message,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    )
}

@Composable
private fun VoiceLibraryRow(message: ChatMessage, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VanishXColors.Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(VanishXColors.Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = VanishXIcons.Play,
                contentDescription = null,
                tint = VanishXColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.mediaFileName
                    ?: stringResource(R.string.room_media_library_voice_item),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val bytes = message.mediaBytes
            if (bytes != null && bytes > 0L) {
                Text(
                    text = formatMediaBytes(bytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = VanishXColors.Muted,
                )
            }
        }
    }
}

private data class MediaLibraryBuckets(
    val photos: List<ChatMessage>,
    val files: List<ChatMessage>,
    val voice: List<ChatMessage>,
) {
    companion object {
        fun from(messages: List<ChatMessage>): MediaLibraryBuckets {
            val media = messages.filter {
                it.isMedia &&
                    !it.mediaLocalPath.isNullOrBlank() &&
                    it.mediaTransferStatus != ChatMessage.MEDIA_FAILED
            }
            return MediaLibraryBuckets(
                photos = media.filter { it.isPhotoOrVideo() },
                files = media.filter { it.isDocumentFile() },
                voice = media.filter { it.isVoiceOrAudio() },
            )
        }
    }
}

private fun ChatMessage.isPhotoOrVideo(): Boolean =
    mediaKind == AttachmentMeta.KIND_IMAGE || mediaKind == AttachmentMeta.KIND_VIDEO

private fun ChatMessage.isVoiceOrAudio(): Boolean {
    val mime = mediaMime?.lowercase()?.substringBefore(';')?.trim().orEmpty()
    val ext = mediaFileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return mediaKind == KIND_AUDIO ||
        mediaKind == KIND_VOICE ||
        mime.startsWith("audio/") ||
        ext in AudioExtensions
}

private fun ChatMessage.isDocumentFile(): Boolean =
    isMedia && !isPhotoOrVideo() && !isVoiceOrAudio()

private fun formatMediaBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

private const val KIND_AUDIO = "audio"
private const val KIND_VOICE = "voice"
private val AudioExtensions = setOf(
    "m4a", "aac", "mp3", "ogg", "opus", "wav", "amr", "3ga", "flac",
)
