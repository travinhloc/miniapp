package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.presentation.theme.VanishXColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MediaThumbnail(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(VanishXColors.Surface2),
        contentAlignment = Alignment.Center,
    ) {
        when (message.mediaKind) {
            AttachmentMeta.KIND_IMAGE -> ImageThumbnail(message)
            AttachmentMeta.KIND_VIDEO -> VideoThumbnail(message)
            AttachmentMeta.KIND_VOICE -> AudioPlaceholder(message)
            else -> DocumentPlaceholder(message)
        }
    }
}

@Composable
private fun ImageThumbnail(message: ChatMessage) {
    Image(
        painter = rememberAsyncImagePainter(message.mediaLocalPath),
        contentDescription = message.mediaFileName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun VideoThumbnail(message: ChatMessage) {
    val path = message.mediaLocalPath
    val loader = rememberMediaPreviewLoader()
    var frame by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        frame = if (path.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) { loader.videoFrame(path)?.bitmap }
        }
    }
    val bitmap = frame
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = message.mediaFileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF25364D), Color(0xFF111827)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = VanishXIcons.Camcorder,
                contentDescription = null,
                tint = VanishXColors.Muted.copy(alpha = 0.65f),
                modifier = Modifier.size(36.dp),
            )
        }
    }
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.68f),
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape),
    ) {
        Icon(
            imageVector = VanishXIcons.Play,
            contentDescription = message.mediaFileName,
            tint = Color.White,
            modifier = Modifier.padding(7.dp),
        )
    }
}

@Composable
private fun DocumentPlaceholder(message: ChatMessage) {
    val typeLabel = MediaPreviewLoader.fileTypeLabel(message.mediaMime, message.mediaFileName)
    val accent = documentAccent(typeLabel)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = VanishXIcons.File,
            contentDescription = message.mediaFileName ?: typeLabel,
            tint = accent,
            modifier = Modifier.size(34.dp),
        )
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AudioPlaceholder(message: ChatMessage) {
    val typeLabel = MediaPreviewLoader.fileTypeLabel(message.mediaMime, message.mediaFileName)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = VanishXColors.Primary.copy(alpha = 0.2f),
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = VanishXIcons.Play,
                contentDescription = message.mediaFileName ?: typeLabel,
                tint = VanishXColors.Primary,
                modifier = Modifier.padding(7.dp),
            )
        }
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = VanishXColors.OnSurface,
            maxLines = 1,
        )
    }
}

private fun documentAccent(typeLabel: String): Color = when (typeLabel) {
    "PDF" -> Color(0xFFE85D5D)
    "DOC" -> Color(0xFF5B8DEF)
    "XLS" -> Color(0xFF43A66B)
    "TXT", "MD" -> Color(0xFFB0B8C5)
    else -> Color(0xFFFFB74D)
}
