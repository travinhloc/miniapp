package com.vault.vanishx.presentation.mailbox.chat

import com.vault.vanishx.presentation.icons.VanishXIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomScreenshotBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VanishXColors.NeonAmber.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = VanishXIcons.Alert,
            contentDescription = null,
            tint = VanishXColors.NeonAmber,
        )
        Text(
            text = stringResource(R.string.room_screenshot_banner),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = VanishXColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = VanishXIcons.Close,
                contentDescription = stringResource(R.string.room_screenshot_dismiss_cd),
                tint = VanishXColors.Muted,
            )
        }
    }
}
