@file:Suppress("MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomWallpaperSheet(
    currentToken: String?,
    onPickPreset: (String) -> Unit,
    onPickGallery: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = VanishXColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoomUiDimens.spacingMedium)
                .padding(bottom = RoomUiDimens.spacingMedium),
        ) {
            Text(
                text = stringResource(R.string.room_options_wallpaper),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VanishXColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.room_wallpaper_presets),
                style = MaterialTheme.typography.labelLarge,
                color = VanishXColors.Muted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.height(168.dp),
            ) {
                items(RoomWallpaper.presets) { (label, argb) ->
                    val token = RoomWallpaper.colorToken(argb)
                    val selected = currentToken == token
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        VanishXColors.Primary
                                    } else {
                                        VanishXColors.Outline.copy(alpha = 0.45f)
                                    },
                                    shape = CircleShape,
                                )
                                .clickable { onPickPreset(token) },
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = VanishXColors.Muted,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onPickGallery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.room_wallpaper_from_gallery))
            }
            if (!currentToken.isNullOrBlank()) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.room_options_reset_wallpaper))
                }
            }
        }
    }
}
