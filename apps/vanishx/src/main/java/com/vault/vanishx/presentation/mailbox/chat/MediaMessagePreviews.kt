@file:Suppress("MagicNumber", "LongMethod", "ComplexMethod")

package com.vault.vanishx.presentation.mailbox.chat

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
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
                imageVector = Icons.Filled.PlayArrow,
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
private fun rememberMediaPreviewLoader(): MediaPreviewLoader {
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
