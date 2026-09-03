@file:Suppress("TooManyFunctions", "LongMethod", "MagicNumber", "ComplexMethod", "ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.chrisbanes.photoview.PhotoView
import com.vault.vanishx.R
import com.vault.vanishx.data.media.DocumentOpenHelper
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.domain.model.ChatMessage
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            isPro = isPro,
            onBack = onBack,
            onSave = onSave,
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
    isPro: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
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
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onSave) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = stringResource(
                    if (isPro) R.string.room_media_save else R.string.room_media_save_pro,
                ),
                tint = VanishXColors.Primary,
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
    var text by remember(path) { mutableStateOf<String?>(null) }
    var loadFailed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        loadFailed = false
        text = null
        val loaded = withContext(Dispatchers.IO) {
            runCatching { readTextCapped(File(path)) }.getOrNull()
        }
        if (loaded == null) {
            loadFailed = true
        } else {
            text = loaded
        }
    }
    val content = text
    when {
        loadFailed -> MissingDocumentState()
        content == null -> LoadingDocumentState()
        content.isBlank() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.room_doc_empty),
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
        else -> {
            // fillMaxSize + verticalScroll on the same Column collapses content height.
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = VanishXColors.OnSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun PdfDocumentPages(path: String) {
    val density = LocalDensity.current
    val session = remember(path) { PdfSession(path) }
    var pageIndex by remember(path) { mutableIntStateOf(0) }
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember(path) { mutableIntStateOf(0) }
    var loadFailed by remember(path) { mutableStateOf(false) }
    var renderWidthPx by remember(path) { mutableIntStateOf(0) }

    DisposableEffect(path) {
        loadFailed = !session.open()
        pageCount = session.pageCount
        onDispose {
            bitmap?.recycle()
            bitmap = null
            session.close()
        }
    }

    LaunchedEffect(pageIndex, pageCount, loadFailed, renderWidthPx) {
        if (loadFailed || pageCount <= 0 || renderWidthPx <= 0) return@LaunchedEffect
        val next = session.render(
            index = pageIndex.coerceIn(0, pageCount - 1),
            targetWidthPx = renderWidthPx,
        ) ?: return@LaunchedEffect
        val previous = bitmap
        bitmap = next
        if (previous !== next) previous?.recycle()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.White),
        ) {
            val widthPx = with(density) { maxWidth.roundToPx() }
            LaunchedEffect(widthPx) {
                // 2× viewport width so pinch-zoom stays sharp; clamp for memory.
                renderWidthPx = (widthPx * PDF_RENDER_SCALE)
                    .roundToInt()
                    .coerceIn(PDF_RENDER_MIN_PX, PDF_RENDER_MAX_PX)
            }
            val pageBitmap = bitmap
            when {
                loadFailed -> MissingDocumentState()
                pageBitmap == null -> LoadingDocumentState()
                else -> PdfPhotoViewPage(
                    bitmap = pageBitmap,
                    pageKey = pageIndex,
                    contentDescription = stringResource(R.string.room_doc_pdf_page_cd),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                    enabled = pageIndex > 0,
                ) {
                    Text(stringResource(R.string.room_doc_prev_page))
                }
                Text(
                    text = stringResource(R.string.room_doc_page_of, pageIndex + 1, pageCount),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(
                    onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                    enabled = pageIndex < pageCount - 1,
                ) {
                    Text(stringResource(R.string.room_doc_next_page))
                }
            }
        }
    }
}

@Composable
private fun PdfPhotoViewPage(
    bitmap: Bitmap,
    pageKey: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PhotoView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                maximumScale = PDF_MAX_ZOOM
                mediumScale = PDF_MEDIUM_ZOOM
                minimumScale = 1f
                setBackgroundColor(Color.WHITE)
            }
        },
        update = { photoView ->
            photoView.contentDescription = contentDescription
            val bound = photoView.tag as? PdfPageBind
            if (bound?.pageKey != pageKey || bound.bitmap !== bitmap) {
                photoView.setImageBitmap(bitmap)
                photoView.setScale(1f, false)
                photoView.tag = PdfPageBind(pageKey, bitmap)
            }
        },
    )
}

private data class PdfPageBind(
    val pageKey: Int,
    val bitmap: Bitmap,
)

@Composable
private fun LoadingDocumentState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.room_doc_loading), color = VanishXColors.Muted)
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

    fun render(index: Int, targetWidthPx: Int): Bitmap? {
        val pdf = renderer ?: return null
        if (index !in 0 until pdf.pageCount) return null
        val page = pdf.openPage(index)
        return try {
            val pageW = page.width.coerceAtLeast(1)
            val pageH = page.height.coerceAtLeast(1)
            var scale = targetWidthPx.toFloat() / pageW
            var outW = (pageW * scale).roundToInt().coerceAtLeast(1)
            var outH = (pageH * scale).roundToInt().coerceAtLeast(1)
            val pixels = outW.toLong() * outH.toLong()
            if (pixels > PDF_MAX_PIXELS) {
                val shrink = sqrt(PDF_MAX_PIXELS.toDouble() / pixels.toDouble())
                scale = (scale * shrink).toFloat()
                outW = (pageW * scale).roundToInt().coerceAtLeast(1)
                outH = (pageH * scale).roundToInt().coerceAtLeast(1)
            }
            val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            // Transparent bitmap + dark theme hides black PDF text.
            Canvas(out).drawColor(Color.WHITE)
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

private const val PDF_RENDER_SCALE = 2f
private const val PDF_RENDER_MIN_PX = 720
private const val PDF_RENDER_MAX_PX = 2_560
private const val PDF_MAX_PIXELS = 8_000_000L
private const val PDF_MAX_ZOOM = 5f
private const val PDF_MEDIUM_ZOOM = 2.5f

private fun readTextCapped(file: File): String {
    if (!file.exists()) return ""
    val bytes = file.readBytes()
    val raw = decodeTextBytes(bytes)
    return if (raw.length <= MediaLimits.TEXT_VIEWER_MAX_CHARS) {
        raw
    } else {
        raw.take(MediaLimits.TEXT_VIEWER_MAX_CHARS) + "\n…"
    }
}

private fun decodeTextBytes(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val bomCharset = when {
        bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte() -> StandardCharsets.UTF_8
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
            Charset.forName("UTF-16LE")
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
            Charset.forName("UTF-16BE")
        else -> null
    }
    if (bomCharset != null) {
        return String(bytes, bomCharset)
    }
    val utf8 = decodeStrict(bytes, StandardCharsets.UTF_8)
    if (utf8 != null) return utf8
    return runCatching { String(bytes, Charset.forName("windows-1252")) }
        .getOrElse { String(bytes, StandardCharsets.UTF_8) }
}

private fun decodeStrict(bytes: ByteArray, charset: Charset): String? {
    return runCatching {
        val decoder: CharsetDecoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }.getOrNull()
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
