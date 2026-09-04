@file:Suppress("MagicNumber", "LongMethod", "ComplexMethod")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.data.media.VoicePlaybackBus
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val DocFooterColor = Color(0xFFE8F1FF)
private val DocFooterOnColor = Color(0xFF1A1A1A)
private val PdfBadgeColor = Color(0xFFE53935)
private val OnDeviceGreen = Color(0xFF2E7D32)
private val BadgeScrim = Color(0x99000000)

@Composable
internal fun ImageMediaPreview(message: ChatMessage) {
    val mediaCorner = RoundedCornerShape(RoomUiDimens.mediaCorner)
    Image(
        painter = rememberAsyncImagePainter(message.mediaLocalPath),
        contentDescription = stringResource(R.string.room_media_image_cd),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .widthIn(max = RoomUiDimens.bubbleMaxWidth)
            .heightIn(min = 140.dp, max = 260.dp)
            .fillMaxWidth()
            .clip(mediaCorner),
    )
}

@Composable
internal fun VideoMediaPreview(message: ChatMessage) {
    val path = message.mediaLocalPath
    val loader = rememberMediaPreviewLoader()
    var frame by remember(path) { mutableStateOf<Bitmap?>(null) }
    var durationMs by remember(path) { mutableStateOf(0L) }
    LaunchedEffect(path) {
        if (path.isNullOrBlank()) {
            frame = null
            durationMs = 0L
            return@LaunchedEffect
        }
        val preview = withContext(Dispatchers.IO) { loader.videoFrame(path) }
        frame = preview?.bitmap
        durationMs = preview?.durationMs ?: 0L
    }
    val mediaCorner = RoundedCornerShape(RoomUiDimens.mediaCorner)
    Box(
        modifier = Modifier
            .widthIn(max = RoomUiDimens.bubbleMaxWidth)
            .heightIn(min = 140.dp, max = 240.dp)
            .fillMaxWidth()
            .clip(mediaCorner)
            .background(Color.Black),
    ) {
        val bitmap = frame
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.room_media_video_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp),
        ) {
            Icon(
                imageVector = VanishXIcons.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(10.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = BadgeScrim,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Text(
                text = MediaPreviewLoader.formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
internal fun VoiceMediaPreview(
    message: ChatMessage,
    mine: Boolean,
    onLongPress: (() -> Unit)? = null,
) {
    val path = message.mediaLocalPath
    val context = LocalContext.current
    val view = LocalView.current
    val loader = rememberMediaPreviewLoader()
    var durationMs by remember(path) { mutableStateOf(0L) }
    val playback by VoicePlaybackBus.state.collectAsStateWithLifecycle()
    val isThis = playback.messageId == message.id
    val playing = isThis && playback.playing
    val progress = when {
        !isThis || playback.durationMs <= 0L -> 0f
        else -> (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    }
    val displayMs = when {
        isThis && playback.durationMs > 0L && playing ->
            (playback.durationMs - playback.positionMs).coerceAtLeast(0L)
        isThis && playback.durationMs > 0L -> playback.durationMs
        else -> durationMs
    }
    LaunchedEffect(path) {
        if (path.isNullOrBlank()) {
            durationMs = 0L
            return@LaunchedEffect
        }
        durationMs = withContext(Dispatchers.IO) { loader.mediaDurationMs(context, path) }
    }
    LaunchedEffect(playing, isThis) {
        if (!playing || !isThis) return@LaunchedEffect
        while (isActive) {
            VoicePlaybackBus.pollPosition()
            delay(48L)
        }
    }
    val mediaCorner = RoundedCornerShape(RoomUiDimens.mediaCorner)
    val activeColor = if (mine) VanishXColors.OnPrimary else VanishXColors.Primary
    val inactiveColor = activeColor.copy(alpha = 0.35f)
    val pending = message.mediaTransferStatus == ChatMessage.MEDIA_PENDING
    val failed = message.mediaTransferStatus == ChatMessage.MEDIA_FAILED
    Row(
        modifier = Modifier
            .width(RoomUiDimens.voiceBubbleWidth)
            .clip(mediaCorner)
            .background(
                if (mine) VanishXColors.Primary else VanishXColors.Surface.copy(alpha = 0.95f),
            )
            .combinedClickable(
                enabled = (!pending && !path.isNullOrBlank()) || onLongPress != null,
                onClick = {
                    if (pending || failed) return@combinedClickable
                    val p = path ?: return@combinedClickable
                    VoicePlaybackBus.toggle(context, message.id, p)
                },
                onLongClick = onLongPress?.let { action ->
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        action()
                    }
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (mine) {
                VanishXColors.OnPrimary.copy(alpha = 0.22f)
            } else {
                VanishXColors.Primary.copy(alpha = 0.16f)
            },
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                when {
                    pending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = activeColor,
                            strokeWidth = 2.dp,
                        )
                    }
                    failed -> {
                        Icon(
                            imageVector = VanishXIcons.Close,
                            contentDescription = stringResource(R.string.room_media_failed),
                            tint = activeColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    playing -> {
                        Icon(
                            imageVector = VanishXIcons.Pause,
                            contentDescription = stringResource(R.string.room_voice_play_cd),
                            tint = activeColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = VanishXIcons.Play,
                            contentDescription = stringResource(R.string.room_voice_play_cd),
                            tint = activeColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
        VoiceWaveformBars(
            seed = message.id,
            active = playing && !pending,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            modifier = Modifier.weight(1f),
            progress = progress,
            barCount = 26,
            height = 26.dp,
        )
        Text(
            text = MediaPreviewLoader.formatDuration(displayMs),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            color = activeColor,
        )
    }
}

@Composable
internal fun DocumentMediaPreview(message: ChatMessage, mine: Boolean) {
    val path = message.mediaLocalPath
    val mime = message.mediaMime
    val fileName = message.mediaFileName ?: stringResource(R.string.room_media_file)
    val typeLabel = MediaPreviewLoader.fileTypeLabel(mime, message.mediaFileName)
    val loader = rememberMediaPreviewLoader()
    val isPdf = typeLabel == "PDF"
    val isTextual = typeLabel == "TXT" || typeLabel == "MD"
    var pdfBitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var textSnippet by remember(path) { mutableStateOf<String?>(null) }
    LaunchedEffect(path, typeLabel) {
        if (path.isNullOrBlank()) {
            pdfBitmap = null
            textSnippet = null
            return@LaunchedEffect
        }
        when {
            isPdf -> {
                pdfBitmap = withContext(Dispatchers.IO) { loader.pdfFirstPage(path) }
                textSnippet = null
            }
            isTextual -> {
                textSnippet = withContext(Dispatchers.IO) { loader.textSnippet(path) }
                pdfBitmap = null
            }
            else -> {
                pdfBitmap = null
                textSnippet = null
            }
        }
    }
    val mediaCorner = RoundedCornerShape(RoomUiDimens.mediaCorner)
    val ready = message.mediaTransferStatus == ChatMessage.MEDIA_READY ||
        (!message.mediaLocalPath.isNullOrBlank() && message.mediaTransferStatus != ChatMessage.MEDIA_FAILED)
    Column(
        modifier = Modifier
            .widthIn(min = 200.dp, max = RoomUiDimens.bubbleMaxWidth)
            .clip(mediaCorner)
            .background(if (mine) VanishXColors.OnPrimary.copy(alpha = 0.08f) else Color.White),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .background(Color(0xFFF7F8FA)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                pdfBitmap != null -> Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.room_media_file),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
                !textSnippet.isNullOrBlank() -> Text(
                    text = textSnippet.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                    color = DocFooterOnColor.copy(alpha = 0.85f),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
                else -> DocTypeBadge(typeLabel = typeLabel, large = true)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DocFooterColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DocTypeBadge(typeLabel = typeLabel, large = false)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DocFooterOnColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val size = formatMediaBytesLabel(message.mediaBytes)
                    Text(
                        text = if (size != null) "$typeLabel · $size" else typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = DocFooterOnColor.copy(alpha = 0.65f),
                        maxLines = 1,
                    )
                }
            }
            if (ready) {
                Text(
                    text = "✓  ${stringResource(R.string.room_media_on_device)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = OnDeviceGreen,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DocTypeBadge(typeLabel: String, large: Boolean) {
    val color = when (typeLabel) {
        "PDF" -> PdfBadgeColor
        "DOC" -> Color(0xFF1565C0)
        "XLS" -> Color(0xFF2E7D32)
        "MD" -> Color(0xFF6A1B9A)
        "TXT" -> Color(0xFF546E7A)
        else -> VanishXColors.Primary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color,
        modifier = if (large) Modifier.size(56.dp) else Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (large) 14.sp else 10.sp,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
internal fun rememberMediaPreviewLoader(): MediaPreviewLoader {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MediaPreviewEntryPoint::class.java,
        ).mediaPreviewLoader()
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface MediaPreviewEntryPoint {
    fun mediaPreviewLoader(): MediaPreviewLoader
}

internal fun formatMediaBytesLabel(bytes: Long?): String? {
    if (bytes == null || bytes <= 0L) return null
    val kb = bytes / 1024.0
    return if (kb < 1024) {
        "${kb.toInt()} KB"
    } else {
        val mb = kb / 1024.0
        "${(mb * 10).toInt() / 10.0} MB"
    }
}
