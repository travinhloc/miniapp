@file:Suppress("LongMethod", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxSheetInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Zalo-like in-chat gallery sheet:
 * - Header: Ảnh · Thư viện
 * - Permission copy + link when media access missing
 * - 3-column grid (Chụp ảnh + recent photos/videos)
 * - Circular send FAB when selection non-empty
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
internal fun GalleryAttachSheet(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSendSelected: (List<Uri>) -> Unit,
    onSelectionLimitReached: () -> Unit = {},
) {
    val context = LocalContext.current
    val mediaPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permissionState = rememberMultiplePermissionsState(mediaPermissions)
    var recent by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }
    var selected by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        recent = if (permissionState.allPermissionsGranted) {
            withContext(Dispatchers.IO) {
                loadRecentGalleryMedia(context, GALLERY_RECENT_LIMIT)
            }
        } else {
            emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .vanishxSheetInsets(),
        ) {
            GallerySheetDragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.room_gallery_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = VanishXColors.OnSurface,
                )
                GalleryAccentLink(
                    text = stringResource(R.string.room_gallery_browse_all),
                    enabled = enabled,
                    onClick = {
                        onDismiss()
                        onOpenLibrary()
                    },
                )
            }

            if (!permissionState.allPermissionsGranted) {
                Text(
                    text = stringResource(R.string.room_gallery_permission_body),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                GalleryAccentLink(
                    text = stringResource(R.string.room_gallery_permission_allow),
                    enabled = true,
                    onClick = { permissionState.launchMultiplePermissionRequest() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            } else if (recent.isEmpty()) {
                Text(
                    text = stringResource(R.string.room_gallery_empty_hint),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                GalleryMediaGrid(
                    recent = recent,
                    selected = selected,
                    enabled = enabled,
                    useCameraIcon = false,
                    onTakePhoto = {
                        onDismiss()
                        onTakePhoto()
                    },
                    onToggleItem = { item ->
                        when (
                            val result = toggleGallerySelection(
                                selected,
                                item.uri,
                                MediaLimits.PHOTO_MULTI_SELECT_MAX,
                            )
                        ) {
                            is GallerySelectionToggle.Changed -> selected = result.items
                            is GallerySelectionToggle.MaxReached -> onSelectionLimitReached()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        end = 2.dp,
                        bottom = if (selected.isNotEmpty()) 72.dp else 8.dp,
                    ),
                )
                if (selected.isNotEmpty()) {
                    GallerySendFab(
                        enabled = enabled,
                        onClick = {
                            val ordered = selected
                            onDismiss()
                            onSendSelected(ordered)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}
