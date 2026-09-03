@file:Suppress("MagicNumber", "LongMethod", "ComplexMethod")

package com.vault.vanishx.presentation.mailbox.chat

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.data.media.VoicePlaybackBus
import com.vault.vanishx.data.media.VoiceRecorder
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxSheetInsets
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class VoiceSheetPhase {
    Idle,
    Recording,
    Review,
}

/**
 * Zalo-like voice sheet: tap/hold to record → review (delete · send · listen) → send.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceRecordSheet(
    voiceRecorder: VoiceRecorder,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSend: (uri: android.net.Uri, displayName: String) -> Unit,
    onRecordFailed: () -> Unit,
    onTooShort: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var phase by remember { mutableStateOf(VoiceSheetPhase.Idle) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var liveLevel by remember { mutableFloatStateOf(0f) }
    var reviewFile by remember { mutableStateOf<File?>(null) }
    val previewId = remember { "voice_preview_${System.currentTimeMillis()}" }
    val finishGate = remember { AtomicBoolean(false) }
    val phaseRef = rememberUpdatedState(phase)

    fun resetToIdle() {
        VoicePlaybackBus.stop()
        voiceRecorder.cancel()
        reviewFile?.delete()
        reviewFile = null
        elapsedMs = 0L
        liveLevel = 0f
        finishGate.set(false)
        phase = VoiceSheetPhase.Idle
    }

    fun completeRecording() {
        if (!finishGate.compareAndSet(false, true)) return
        val file = voiceRecorder.stop()
        if (file == null) {
            onTooShort()
            elapsedMs = 0L
            phase = VoiceSheetPhase.Idle
            finishGate.set(false)
        } else {
            reviewFile = file
            phase = VoiceSheetPhase.Review
        }
    }

    fun beginRecording() {
        if (!enabled || phase == VoiceSheetPhase.Recording) return
        VoicePlaybackBus.stop()
        reviewFile?.delete()
        reviewFile = null
        finishGate.set(false)
        runCatching {
            voiceRecorder.start(onMaxDuration = { completeRecording() })
            elapsedMs = 0L
            phase = VoiceSheetPhase.Recording
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }.onFailure {
            voiceRecorder.cancel()
            onRecordFailed()
            phase = VoiceSheetPhase.Idle
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            VoicePlaybackBus.stop()
            if (phaseRef.value == VoiceSheetPhase.Recording) {
                voiceRecorder.cancel()
            }
        }
    }

    LaunchedEffect(phase) {
        if (phase != VoiceSheetPhase.Recording) {
            liveLevel = 0f
            return@LaunchedEffect
        }
        val startedAt = SystemClock.elapsedRealtime()
        while (isActive && phase == VoiceSheetPhase.Recording) {
            elapsedMs = (SystemClock.elapsedRealtime() - startedAt)
                .coerceAtMost(MediaLimits.VOICE_MAX_DURATION_MS)
            liveLevel = voiceRecorder.amplitudeLevel()
            if (elapsedMs >= MediaLimits.VOICE_MAX_DURATION_MS) {
                completeRecording()
                break
            }
            delay(50L)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            resetToIdle()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .vanishxSheetInsets()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (phase) {
                VoiceSheetPhase.Idle, VoiceSheetPhase.Recording -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (phase == VoiceSheetPhase.Idle) {
                            Text(
                                text = stringResource(R.string.room_voice_tap_or_hold),
                                color = VanishXColors.Muted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                VoiceLiveMeter(
                                    elapsedMs = elapsedMs,
                                    liveLevel = liveLevel,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.room_voice_tap_to_stop),
                                    color = VanishXColors.Muted,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Stable mic slot so hold-release survives Idle → Recording recomposition.
                    VoiceBigMicButton(
                        enabled = enabled || phase == VoiceSheetPhase.Recording,
                        recording = phase == VoiceSheetPhase.Recording,
                        onStart = { beginRecording() },
                        onStop = { completeRecording() },
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
                VoiceSheetPhase.Review -> {
                    val file = reviewFile
                    VoiceLiveMeter(
                        elapsedMs = elapsedMs,
                        liveLevel = 0f,
                        animateIdle = false,
                        seed = file?.name ?: "review",
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VoiceReviewAction(
                            icon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = VanishXColors.OnSurface,
                                    modifier = Modifier.size(26.dp),
                                )
                            },
                            label = stringResource(R.string.room_voice_delete),
                            onClick = { resetToIdle() },
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(VanishXColors.Primary)
                                    .clickable(enabled = enabled && file != null) {
                                        val f = file ?: return@clickable
                                        onSend(voiceRecorder.uriFor(f), f.name)
                                        reviewFile = null
                                        phase = VoiceSheetPhase.Idle
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.room_voice_send),
                                    tint = VanishXColors.OnPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.room_voice_send),
                                color = VanishXColors.OnSurface,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        val playback by VoicePlaybackBus.state.collectAsStateWithLifecycle()
                        val previewPlaying =
                            playback.messageId == previewId && playback.playing
                        VoiceReviewAction(
                            icon = {
                                if (previewPlaying) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_voice_pause),
                                        contentDescription = null,
                                        tint = VanishXColors.OnSurface,
                                        modifier = Modifier.size(26.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = VanishXColors.OnSurface,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            },
                            label = stringResource(R.string.room_voice_listen),
                            onClick = {
                                val f = file ?: return@VoiceReviewAction
                                VoicePlaybackBus.toggle(context, previewId, f.absolutePath)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.action_back),
                        color = VanishXColors.Muted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable {
                                resetToIdle()
                                onDismiss()
                            }
                            .padding(8.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun VoiceLiveMeter(
    elapsedMs: Long,
    liveLevel: Float,
    animateIdle: Boolean = true,
    seed: String = "live",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(VanishXColors.Primary.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VoiceWaveformBars(
            seed = seed,
            active = animateIdle || liveLevel > 0.02f,
            activeColor = VanishXColors.Primary,
            inactiveColor = VanishXColors.Primary.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f),
            progress = 0f,
            barCount = 22,
            height = 22.dp,
            liveLevel = liveLevel,
        )
        Text(
            text = MediaPreviewLoader.formatDuration(elapsedMs),
            color = VanishXColors.OnSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun VoiceBigMicButton(
    enabled: Boolean,
    recording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val recordingRef = rememberUpdatedState(recording)
    val onStartRef = rememberUpdatedState(onStart)
    val onStopRef = rememberUpdatedState(onStop)
    val fill = if (recording) Color(0xFFE53935) else VanishXColors.Primary
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(fill.copy(alpha = 0.18f))
            .padding(8.dp)
            .clip(CircleShape)
            .background(fill)
            .then(
                if (enabled) {
                    // Unit key keeps the gesture alive across Idle → Recording.
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (recordingRef.value) {
                                    tryAwaitRelease()
                                    onStopRef.value()
                                    return@detectTapGestures
                                }
                                val startedAt = SystemClock.elapsedRealtime()
                                onStartRef.value()
                                tryAwaitRelease()
                                if (SystemClock.elapsedRealtime() - startedAt >= HOLD_TO_STOP_MS) {
                                    onStopRef.value()
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (recording) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.room_voice_tap_to_stop),
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_composer_mic),
                contentDescription = stringResource(R.string.room_voice_cd),
                tint = VanishXColors.OnPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

private const val HOLD_TO_STOP_MS = 350L

@Composable
private fun VoiceReviewAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = VanishXColors.OnSurface,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
