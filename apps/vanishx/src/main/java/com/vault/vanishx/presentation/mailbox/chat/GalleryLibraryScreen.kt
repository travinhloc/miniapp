@file:Suppress("LongMethod")

package com.vault.vanishx.presentation.mailbox.chat

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxScreenInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Zalo-like full library picker (opened via “Thư viện”):
 * - Top bar: back · Tất cả · Gửi cho {peer}
 * - 3-column grid with camera tile, photos, videos (duration badge)
 * - Send FAB bottom-right
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
internal fun GalleryLibraryScreen(
    recipientName: String,
    enabled: Boolean,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = VanishXColors.OnSurface,
                    )
                }
            },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.room_gallery_library_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        ),
                        color = VanishXColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.room_gallery_send_to, recipientName),
                        style = MaterialTheme.typography.bodySmall,
                        color = VanishXColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VanishXColors.Bg,
                titleContentColor = VanishXColors.OnSurface,
            ),
        )

        if (!permissionState.allPermissionsGranted) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.room_gallery_permission_body),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                GalleryAccentLink(
                    text = stringResource(R.string.room_gallery_permission_allow),
                    enabled = true,
                    onClick = { permissionState.launchMultiplePermissionRequest() },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            GalleryMediaGrid(
                recent = recent,
                selected = selected,
                enabled = enabled,
                useCameraIcon = true,
                onTakePhoto = onTakePhoto,
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
                        onSendSelected(selected)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}
