package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

/** Voice note tray stub — full record/send pipeline later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceRecordTray(
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VanishXColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.room_voice_tray_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.room_voice_tray_body),
                style = MaterialTheme.typography.bodyMedium,
                color = VanishXColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Surface(
                shape = CircleShape,
                color = VanishXColors.Primary.copy(alpha = 0.18f),
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_composer_mic),
                    contentDescription = null,
                    tint = VanishXColors.Primary,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.room_voice_tray_hint),
                style = MaterialTheme.typography.labelMedium,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.room_safety_close))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
