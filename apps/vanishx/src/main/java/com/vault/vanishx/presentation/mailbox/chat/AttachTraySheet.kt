package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.theme.VanishXColors

private val DocumentCircleColor = Color(0xFF1B4F9C)

/**
 * Extensible “more” tray from composer `…`.
 * MVP: Document. Later: transfer, bank account, card, GIF, …
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachTraySheet(
    enabled: Boolean,
    onPickDocument: () -> Unit,
    onAction: (RoomAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(RoomAction.DismissAttachTray) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VanishXColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.room_attach_tray_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.Start),
            ) {
                AttachTrayItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = stringResource(R.string.room_attach_document),
                    circleColor = DocumentCircleColor,
                    enabled = enabled,
                    onClick = {
                        onAction(RoomAction.DismissAttachTray)
                        onPickDocument()
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.room_attach_tray_soon),
                style = MaterialTheme.typography.labelSmall,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AttachTrayItem(
    icon: ImageVector,
    label: String,
    circleColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) circleColor else circleColor.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier
                    .padding(14.dp)
                    .size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = VanishXColors.OnSurface,
            textAlign = TextAlign.Center,
        )
    }
}
