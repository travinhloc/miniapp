@file:Suppress("MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.data.media.MediaPreviewLoader
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors

/**
 * Full-screen recording HUD (E16-5): progress ring · slide-up cancel.
 * Gesture stays on the composer mic; this is visual feedback only.
 */
@Composable
internal fun VoiceRecordHud(
    elapsedMs: Long,
    cancelArmed: Boolean,
) {
    val progress = (elapsedMs.toFloat() / MediaLimits.VOICE_MAX_DURATION_MS.toFloat())
        .coerceIn(0f, 1f)
    val accent = if (cancelArmed) Color(0xFFE53935) else VanishXColors.Primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = stringResource(
                    if (cancelArmed) {
                        R.string.room_voice_release_cancel
                    } else {
                        R.string.room_voice_slide_cancel
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cancelArmed) accent else Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeWidth = 5.dp,
                )
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.22f),
                    modifier = Modifier.size(84.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_composer_mic),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = MediaPreviewLoader.formatDuration(elapsedMs),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.room_voice_recording_hint),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
