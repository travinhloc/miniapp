@file:Suppress("TooManyFunctions", "LongMethod", "MagicNumber", "ComplexMethod", "ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.data.media.DocumentOpenHelper
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@Composable
internal fun DocumentViewerScreen(
    message: ChatMessage,
    isPro: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    documentOpenHelper: DocumentOpenHelper = remember { DocumentOpenHelper() },
) {
    val context = LocalContext.current
    val fileName = message.mediaFileName ?: stringResource(R.string.room_media_file)
    val path = message.mediaLocalPath
    val mime = message.mediaMime
    val inApp = MediaLimits.isInAppDocumentViewer(mime, message.mediaFileName)
    val isPdf = MediaLimits.isPdf(mime, message.mediaFileName)
    val isText = MediaLimits.isPlainTextDocument(mime, message.mediaFileName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        DocumentViewerTopBar(
            title = fileName,
            onBack = onBack,
            onOpenExternal = path?.let { filePath ->
                { documentOpenHelper.openWithSystemApp(context, filePath, mime) }
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            DocumentViewerBody(
                path = path,
                isPdf = isPdf,
                isText = isText,
                message = message,
                fileName = fileName,
                onOpenExternal = {
                    path?.let { documentOpenHelper.openWithSystemApp(context, it, mime) }
                },
            )
        }
        DocumentViewerActions(
            isPro = isPro,
            showOpenExternal = inApp && path != null,
            onOpenExternal = {
                path?.let { documentOpenHelper.openWithSystemApp(context, it, mime) }
            },
            onSave = onSave,
            onBack = onBack,
        )
    }
}

@Composable
private fun DocumentViewerBody(
    path: String?,
    isPdf: Boolean,
    isText: Boolean,
    message: ChatMessage,
    fileName: String,
    onOpenExternal: () -> Unit,
) {
    if (path == null || !File(path).exists()) {
        MissingDocumentState()
        return
    }
    if (isPdf) {
        PdfDocumentPages(path = path)
        return
    }
    if (isText) {
        TextDocumentBody(path = path)
        return
    }
    OfficeDocumentMeta(
        message = message,
        fileName = fileName,
        onOpenExternal = onOpenExternal,
    )
}

@Composable
private fun DocumentViewerTopBar(
    title: String,
    onBack: () -> Unit,
    onOpenExternal: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RoomUiDimens.topBarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = VanishXColors.OnSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = VanishXColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onOpenExternal != null) {
            IconButton(onClick = onOpenExternal) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.room_doc_open_system_cd),
                    tint = VanishXColors.Primary,
                )
            }
        }
    }
}

@Composable
private fun DocumentViewerActions(
    isPro: Boolean,
    showOpenExternal: Boolean,
    onOpenExternal: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.action_back), color = VanishXColors.Muted)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showOpenExternal) {
            TextButton(onClick = onOpenExternal) {
                Text(stringResource(R.string.room_doc_open_system), color = VanishXColors.Primary)
            }
        }
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                stringResource(
                    if (isPro) R.string.room_media_save else R.string.room_media_save_pro,
                ),
            )
        }
    }
}

@Composable
private fun MissingDocumentState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.room_media_missing),
            color = VanishXColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OfficeDocumentMeta(
    message: ChatMessage,
    fileName: String,
    onOpenExternal: () -> Unit,
) {
    val typeLabel = MediaPreviewLoader.fileTypeLabel(message.mediaMime, message.mediaFileName)
    val sizeLabel = message.mediaBytes?.let { formatBytes(it) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .background(VanishXColors.Surface2, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = VanishXColors.Primary,
            )
        }
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleMedium,
            color = VanishXColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        if (sizeLabel != null) {
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
            )
        }
        Text(
            text = stringResource(R.string.room_doc_office_hint),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = VanishXColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onOpenExternal,
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.room_doc_open_system))
        }
    }
}

@Composable
private fun TextDocumentBody(path: String) {
    val text = remember(path) { readTextCapped(File(path)) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = VanishXColors.OnSurface,
        )
    }
}

@Composable
private fun PdfDocumentPages(path: String) {
    val session = remember(path) { PdfSession(path) }
    var pageIndex by remember(path) { mutableIntStateOf(0) }
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember(path) { mutableIntStateOf(0) }
    var loadFailed by remember(path) { mutableStateOf(false) }

    DisposableEffect(path) {
        loadFailed = !session.open()
        pageCount = session.pageCount
        onDispose {
            bitmap?.recycle()
            bitmap = null
            session.close()
        }
    }

    LaunchedEffect(pageIndex, pageCount, loadFailed) {
        if (loadFailed || pageCount <= 0) return@LaunchedEffect
        val next = session.render(pageIndex.coerceIn(0, pageCount - 1)) ?: return@LaunchedEffect
        val previous = bitmap
        bitmap = next
        if (previous !== next) previous?.recycle()
    }

    val current = bitmap
    when {
        loadFailed -> MissingDocumentState()
        current == null -> LoadingDocumentState()
        else -> PdfPagesBody(
            bitmap = current,
            pageIndex = pageIndex,
            pageCount = pageCount,
            onPrev = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
            onNext = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
        )
    }
}

@Composable
private fun LoadingDocumentState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.room_doc_loading), color = VanishXColors.Muted)
    }
}

@Composable
private fun PdfPagesBody(
    bitmap: Bitmap,
    pageIndex: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.room_doc_pdf_page_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPrev, enabled = pageIndex > 0) {
                    Text(stringResource(R.string.room_doc_prev_page))
                }
                Text(
                    text = stringResource(R.string.room_doc_page_of, pageIndex + 1, pageCount),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = onNext, enabled = pageIndex < pageCount - 1) {
                    Text(stringResource(R.string.room_doc_next_page))
                }
            }
        }
    }
}

private class PdfSession(private val path: String) {
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    val pageCount: Int get() = renderer?.pageCount ?: 0

    fun open(): Boolean = runCatching {
        descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(checkNotNull(descriptor))
        true
    }.getOrDefault(false)

    fun render(index: Int): Bitmap? {
        val pdf = renderer ?: return null
        if (index !in 0 until pdf.pageCount) return null
        val page = pdf.openPage(index)
        return try {
            val out = Bitmap.createBitmap(
                page.width.coerceAtLeast(1),
                page.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            page.render(out, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            out
        } finally {
            page.close()
        }
    }

    fun close() {
        runCatching { renderer?.close() }
        runCatching { descriptor?.close() }
        renderer = null
        descriptor = null
    }
}

private fun readTextCapped(file: File): String {
    if (!file.exists()) return ""
    val bytes = file.readBytes()
    val charset: Charset = StandardCharsets.UTF_8
    val raw = String(bytes, charset)
    return if (raw.length <= MediaLimits.TEXT_VIEWER_MAX_CHARS) {
        raw
    } else {
        raw.take(MediaLimits.TEXT_VIEWER_MAX_CHARS) + "\n…"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    return if (kb < 1024) {
        "%.1f KB".format(kb)
    } else {
        "%.1f MB".format(kb / 1024.0)
    }
}
