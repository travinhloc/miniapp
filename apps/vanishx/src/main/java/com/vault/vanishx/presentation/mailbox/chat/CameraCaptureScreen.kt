@file:Suppress(
    "TooManyFunctions",
    "LongMethod",
    "MagicNumber",
    "ComplexMethod",
    "MatchingDeclarationName",
)

package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import dagger.hilt.android.EntryPointAccessors
import com.vault.vanishx.presentation.util.appDetailsSettingsIntent
import com.vault.vanishx.presentation.mailbox.AppLockEntryPoint
import com.vault.vanishx.presentation.components.VanishXAlertTone
import com.vault.vanishx.presentation.components.VanishXAlertDialog
import androidx.compose.runtime.saveable.rememberSaveable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.pm.PackageManager
import android.Manifest
import android.os.SystemClock
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.vault.vanishx.R
import com.vault.vanishx.data.media.ImagePrepareHelper
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.data.media.VideoPrepareHelper
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class CameraCaptureTab {
    Photo,
    Video,
}

/**
 * Full-screen camera sheet (E16-4 / E16-6 / E16-9): Photo | Video.
 * Photo = CameraX still + pre-send rotate/crop. Video = CameraX record · 60s ring · size gate.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun CameraCaptureScreen(
    imagePrepareHelper: ImagePrepareHelper,
    videoPrepareHelper: VideoPrepareHelper,
    onDismiss: () -> Unit,
    onPhotoReady: (uri: android.net.Uri, displayName: String?) -> Unit,
    onVideoReady: (uri: android.net.Uri, displayName: String?) -> Unit,
    initialTab: CameraCaptureTab = CameraCaptureTab.Photo,
) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var cameraPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var showCameraSettingsDialog by remember { mutableStateOf(false) }
    val appLockSession = remember(context) {
        EntryPointAccessors.fromActivity(
            context as Activity,
            AppLockEntryPoint::class.java,
        ).appLockSession()
    }
    val appSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        appLockSession.endExternalUi()
    }
    var tab by remember { mutableStateOf(initialTab) }
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var videoBusy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .vanishxScreenInsets(),
    ) {
        CameraCaptureTopBar(onBack = onDismiss)
        if (!cameraPermission.status.isGranted) {
            val permanentlyDenied = !cameraPermission.status.shouldShowRationale &&
                cameraPermissionRequested
            LaunchedEffect(permanentlyDenied) {
                if (permanentlyDenied) showCameraSettingsDialog = true
            }
            CameraPermissionPane(
                showRationale = cameraPermission.status.shouldShowRationale,
                permanentlyDenied = permanentlyDenied,
                onRequest = {
                    if (permanentlyDenied) {
                        showCameraSettingsDialog = true
                    } else {
                        cameraPermissionRequested = true
                        cameraPermission.launchPermissionRequest()
                    }
                },
                onDismiss = onDismiss,
            )
            if (showCameraSettingsDialog) {
                VanishXAlertDialog(
                    title = stringResource(R.string.room_camera_permission_title),
                    body = stringResource(R.string.room_camera_permission_denied_body),
                    confirmLabel = stringResource(R.string.room_permission_open_settings),
                    dismissLabel = stringResource(R.string.action_back),
                    tone = VanishXAlertTone.Accent,
                    onConfirm = {
                        showCameraSettingsDialog = false
                        appLockSession.beginExternalUi()
                        appSettingsLauncher.launch(appDetailsSettingsIntent(context))
                    },
                    onDismiss = { showCameraSettingsDialog = false },
                )
            }
            return@Column
        }

        val captured = capturedFile
        if (captured != null) {
            ImagePrepareEditor(
                source = captured,
                imagePrepareHelper = imagePrepareHelper,
                onRetake = { capturedFile = null },
                onCancel = onDismiss,
                onReady = { file ->
                    onPhotoReady(imagePrepareHelper.uriForFile(file), file.name)
                },
            )
            return@Column
        }

        val recorded = recordedFile
        if (recorded != null) {
            VideoReviewPane(
                source = recorded,
                videoPrepareHelper = videoPrepareHelper,
                busy = videoBusy,
                onBusyChange = { videoBusy = it },
                onRetake = {
                    recorded.delete()
                    recordedFile = null
                },
                onCancel = onDismiss,
                onReady = { file ->
                    onVideoReady(videoPrepareHelper.uriForFile(file), file.name)
                },
            )
            return@Column
        }

        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = Color.Transparent,
            contentColor = VanishXColors.OnSurface,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[tab.ordinal]),
                    color = VanishXColors.Primary,
                )
            },
        ) {
            Tab(
                selected = tab == CameraCaptureTab.Photo,
                onClick = { tab = CameraCaptureTab.Photo },
                text = {
                    Text(
                        stringResource(R.string.room_camera_tab_photo),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
            Tab(
                selected = tab == CameraCaptureTab.Video,
                onClick = { tab = CameraCaptureTab.Video },
                text = {
                    Text(
                        stringResource(R.string.room_camera_tab_video),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }

        when (tab) {
            CameraCaptureTab.Photo -> PhotoCapturePane(
                imagePrepareHelper = imagePrepareHelper,
                onCaptured = { capturedFile = it },
            )
            CameraCaptureTab.Video -> VideoCapturePane(
                videoPrepareHelper = videoPrepareHelper,
                onRecorded = { recordedFile = it },
            )
        }
    }
}

@Composable
private fun CameraCaptureTopBar(onBack: () -> Unit) {
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
                tint = Color.White,
            )
        }
        Text(
            text = stringResource(R.string.room_camera_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

@Composable
private fun CameraPermissionPane(
    showRationale: Boolean,
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                when {
                    permanentlyDenied -> R.string.room_camera_permission_denied_body
                    showRationale -> R.string.room_camera_permission_rationale
                    else -> R.string.room_camera_permission_body
                },
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = VanishXColors.Primary,
                contentColor = VanishXColors.OnPrimary,
            ),
        ) {
            Text(
                stringResource(
                    if (permanentlyDenied) {
                        R.string.room_permission_open_settings
                    } else {
                        R.string.room_camera_permission_allow
                    },
                ),
            )
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_back), color = VanishXColors.Muted)
        }
    }
}

@Composable
private fun PhotoCapturePane(
    imagePrepareHelper: ImagePrepareHelper,
    onCaptured: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var capturing by remember { mutableStateOf(false) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            }
        }
        future.addListener(listener, mainExecutor)
        onDispose {
            runCatching { future.get().unbindAll() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
            ) {
                Icon(
                    imageVector = VanishXIcons.Refresh,
                    contentDescription = stringResource(R.string.room_camera_flip_cd),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(VanishXColors.Primary)
                    .clickable(enabled = !capturing) {
                        capturing = true
                        val out = imagePrepareHelper.createCaptureFile()
                        val options = ImageCapture.OutputFileOptions.Builder(out).build()
                        imageCapture.takePicture(
                            options,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(
                                    outputFileResults: ImageCapture.OutputFileResults,
                                ) {
                                    mainExecutor.execute {
                                        capturing = false
                                        onCaptured(out)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    mainExecutor.execute {
                                        capturing = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.room_camera_capture_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (capturing) {
                    CircularProgressIndicator(
                        color = VanishXColors.OnPrimary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoCapturePane(
    videoPrepareHelper: VideoPrepareHelper,
    onRecorded: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var audioPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var showMicSettingsDialog by remember { mutableStateOf(false) }
    val videoContext = LocalContext.current
    val videoAppLockSession = remember(videoContext) {
        EntryPointAccessors.fromActivity(
            videoContext as Activity,
            AppLockEntryPoint::class.java,
        ).appLockSession()
    }
    val videoSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        videoAppLockSession.endExternalUi()
    }

    val qualitySelector = remember {
        QualitySelector.fromOrderedList(
            listOf(Quality.SD, Quality.LOWEST),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
        )
    }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun startRecording(withAudio: Boolean) {
        if (isRecording) return
        errorRes = null
        val out = videoPrepareHelper.createCaptureFile()
        val options = FileOutputOptions.Builder(out).build()
        val started = runCatching {
            val base = recorder.prepareRecording(context, options)
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            val pending = if (withAudio && hasMic) {
                base.withAudioEnabled()
            } else {
                base
            }
            isRecording = true
            elapsedMs = 0L
            pending.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        activeRecording = null
                        if (event.hasError()) {
                            out.delete()
                            errorRes = R.string.room_camera_video_record_failed
                        } else if (out.exists() && out.length() > 0L) {
                            onRecorded(out)
                        } else {
                            out.delete()
                            errorRes = R.string.room_camera_video_record_failed
                        }
                    }
                }
            }
        }.getOrElse {
            out.delete()
            isRecording = false
            errorRes = R.string.room_camera_video_record_failed
            null
        }
        activeRecording = started
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            elapsedMs = 0L
            return@LaunchedEffect
        }
        val startedAt = SystemClock.elapsedRealtime()
        while (isActive && isRecording) {
            elapsedMs = (SystemClock.elapsedRealtime() - startedAt)
                .coerceAtMost(MediaLimits.VIDEO_MAX_DURATION_MS)
            if (elapsedMs >= MediaLimits.VIDEO_MAX_DURATION_MS) {
                stopRecording()
                break
            }
            delay(50L)
        }
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
            }
        }
        future.addListener(listener, mainExecutor)
        onDispose {
            activeRecording?.stop()
            activeRecording = null
            runCatching { future.get().unbindAll() }
        }
    }

    val progress = (elapsedMs.toFloat() / MediaLimits.VIDEO_MAX_DURATION_MS.toFloat())
        .coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        if (!audioPermission.status.isGranted && !isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.room_camera_video_mic_muted),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    val permanentlyDenied = !audioPermission.status.shouldShowRationale &&
                        audioPermissionRequested
                    if (permanentlyDenied) {
                        showMicSettingsDialog = true
                    } else {
                        audioPermissionRequested = true
                        audioPermission.launchPermissionRequest()
                    }
                }) {
                    Text(
                        stringResource(R.string.room_camera_video_mic_allow),
                        color = VanishXColors.Primary,
                    )
                }
            }
        }



        if (showMicSettingsDialog) {
            VanishXAlertDialog(
                title = stringResource(R.string.room_voice_permission_title),
                body = stringResource(R.string.room_voice_permission_denied_body),
                confirmLabel = stringResource(R.string.room_permission_open_settings),
                dismissLabel = stringResource(R.string.action_back),
                tone = VanishXAlertTone.Accent,
                onConfirm = {
                    showMicSettingsDialog = false
                    videoAppLockSession.beginExternalUi()
                    videoSettingsLauncher.launch(appDetailsSettingsIntent(videoContext))
                },
                onDismiss = { showMicSettingsDialog = false },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isRecording) {
                    MediaPreviewLoader.formatDuration(elapsedMs)
                } else {
                    stringResource(R.string.room_camera_video_idle_hint)
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (isRecording) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.room_camera_video_recording_hint),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            errorRes?.let { res ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(res),
                    color = Color(0xFFE53935),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    enabled = !isRecording,
                ) {
                    Icon(
                        imageVector = VanishXIcons.Refresh,
                        contentDescription = stringResource(R.string.room_camera_flip_cd),
                        tint = if (isRecording) Color.White.copy(alpha = 0.35f) else Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.22f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        if (isRecording && progress > 0f) {
                            drawArc(
                                color = Color(0xFFE53935),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 36.dp else 64.dp)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(if (isRecording) Color(0xFFE53935) else Color.White)
                            .clickable {
                                if (isRecording) {
                                    stopRecording()
                                } else {
                                    startRecording(withAudio = audioPermission.status.isGranted)
                                }
                            },
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun VideoReviewPane(
    source: File,
    videoPrepareHelper: VideoPrepareHelper,
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onReady: (File) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var errorRes by remember { mutableStateOf<Int?>(null) }
    val durationLabel = remember(source) {
        MediaPreviewLoader.formatDuration(videoPrepareHelper.readDurationMs(source))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            MediaPathPlayer(
                path = source.absolutePath,
                aspectRatio = 16f / 9f,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                autoPlay = true,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = durationLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            errorRes?.let { res ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC1A1A1A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(res),
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (busy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VanishXColors.Primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.room_camera_video_preparing),
                            color = Color.White,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel, enabled = !busy) {
                Icon(
                    VanishXIcons.Close,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Color.White,
                )
            }
            TextButton(onClick = onRetake, enabled = !busy) {
                Text(stringResource(R.string.room_camera_retake), color = VanishXColors.Muted)
            }
            IconButton(
                onClick = {
                    onBusyChange(true)
                    errorRes = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { videoPrepareHelper.prepareForSend(source) }
                        }
                        onBusyChange(false)
                        result.onSuccess { onReady(it) }
                            .onFailure { err ->
                                val msg = err.message.orEmpty()
                                errorRes = if (msg.contains("exceeds", ignoreCase = true)) {
                                    R.string.room_camera_video_too_large
                                } else {
                                    R.string.room_camera_video_record_failed
                                }
                            }
                    }
                },
                enabled = !busy,
            ) {
                Icon(
                    VanishXIcons.Check,
                    contentDescription = stringResource(R.string.room_camera_video_send_cd),
                    tint = VanishXColors.Primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ImagePrepareEditor(
    source: File,
    imagePrepareHelper: ImagePrepareHelper,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onReady: (File) -> Unit,
) {
    var rotation by remember { mutableIntStateOf(0) }
    var squareCrop by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var previewFile by remember(source) { mutableStateOf(source) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = rememberAsyncImagePainter(previewFile),
                contentDescription = stringResource(R.string.room_camera_preview_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
            )
            if (busy) {
                CircularProgressIndicator(color = VanishXColors.Primary)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(
                onClick = {
                    rotation = (rotation + 90) % 360
                    busy = true
                    runCatching {
                        imagePrepareHelper.prepare(source, rotation, squareCrop)
                    }.onSuccess {
                        previewFile = it
                        busy = false
                    }.onFailure { busy = false }
                },
                enabled = !busy,
            ) {
                Text(stringResource(R.string.room_camera_rotate), color = Color.White)
            }
            TextButton(
                onClick = {
                    val next = !squareCrop
                    squareCrop = next
                    busy = true
                    runCatching {
                        imagePrepareHelper.prepare(source, rotation, next)
                    }.onSuccess {
                        previewFile = it
                        busy = false
                    }.onFailure { busy = false }
                },
                enabled = !busy,
            ) {
                Text(
                    stringResource(
                        if (squareCrop) R.string.room_camera_crop_on else R.string.room_camera_crop,
                    ),
                    color = if (squareCrop) VanishXColors.Primary else Color.White,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel, enabled = !busy) {
                Icon(
                    VanishXIcons.Close,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Color.White,
                )
            }
            TextButton(onClick = onRetake, enabled = !busy) {
                Text(stringResource(R.string.room_camera_retake), color = VanishXColors.Muted)
            }
            IconButton(
                onClick = {
                    busy = true
                    runCatching {
                        imagePrepareHelper.prepare(source, rotation, squareCrop)
                    }.onSuccess { prepared ->
                        busy = false
                        onReady(prepared)
                    }.onFailure { busy = false }
                },
                enabled = !busy,
            ) {
                Icon(
                    VanishXIcons.Check,
                    contentDescription = stringResource(R.string.room_camera_send_cd),
                    tint = VanishXColors.Primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
