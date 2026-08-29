@file:Suppress("TooManyFunctions", "LongMethod", "MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.MediaLimits
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.vanishxSheetInsets

/**
 * Zalo-like in-chat gallery sheet (ref screen recording 2026-08-23):
 * - First tile = Chụp ảnh
 * - Remaining = recent photos with multi-select circles (max [MediaLimits.PHOTO_MULTI_SELECT_MAX])
 * - Bottom send bar when selection non-empty
 * - Fallback “Thư viện” → system Photo Picker when no media permission / empty
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
internal fun GalleryAttachSheet(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpenSystemLibrary: () -> Unit,
    onSendSelected: (List<Uri>) -> Unit,
    onSelectionLimitReached: () -> Unit = {},
) {
    val context = LocalContext.current
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(mediaPermission)
    var recent by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selected by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(permissionState.status.isGranted) {
        recent = if (permissionState.status.isGranted) {
            loadRecentImageUris(context, limit = RECENT_LIMIT)
        } else {
            emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VanishXColors.Surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .vanishxSheetInsets()
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.room_gallery_sheet_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = VanishXColors.OnSurface,
                )
                TextButton(onClick = onOpenSystemLibrary, enabled = enabled) {
                    Text(
                        stringResource(R.string.room_gallery_browse_all),
                        color = VanishXColors.Primary,
                    )
                }
            }

            if (!permissionState.status.isGranted) {
                Text(
                    text = stringResource(R.string.room_gallery_permission_body),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                TextButton(
                    onClick = { permissionState.launchPermissionRequest() },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.room_gallery_permission_allow),
                        color = VanishXColors.Primary,
                    )
                }
            }

            if (permissionState.status.isGranted && recent.isEmpty()) {
                Text(
                    text = stringResource(R.string.room_gallery_empty_hint),
                    color = VanishXColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    TakePhotoTile(
                        enabled = enabled,
                        onClick = {
                            onDismiss()
                            onTakePhoto()
                        },
                    )
                }
                items(recent, key = { it.toString() }) { uri ->
                    val index = selected.indexOf(uri)
                    val isSelected = index >= 0
                    RecentPhotoTile(
                        uri = uri,
                        selected = isSelected,
                        selectionIndex = if (isSelected) index + 1 else null,
                        enabled = enabled,
                        onClick = {
                            when (val result = toggleSelection(selected, uri)) {
                                is SelectionToggle.Added,
                                is SelectionToggle.Removed,
                                -> selected = result.items
                                is SelectionToggle.MaxReached -> onSelectionLimitReached()
                            }
                        },
                    )
                }
            }

            if (selected.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.room_gallery_selected_count,
                            selected.size,
                            MediaLimits.PHOTO_MULTI_SELECT_MAX,
                        ),
                        color = VanishXColors.OnSurface,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Button(
                        onClick = {
                            val ordered = selected
                            onDismiss()
                            onSendSelected(ordered)
                        },
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VanishXColors.Primary,
                            contentColor = VanishXColors.OnPrimary,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(stringResource(R.string.room_gallery_send))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TakePhotoTile(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(VanishXColors.Surface2)
            .border(1.dp, VanishXColors.Outline, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = VanishXColors.OnSurface,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.room_gallery_take_photo),
                color = VanishXColors.OnSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecentPhotoTile(
    uri: Uri,
    selected: Boolean,
    selectionIndex: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (selected) VanishXColors.Primary else Color.Black.copy(alpha = 0.35f),
                )
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected && selectionIndex != null) {
                Text(
                    text = selectionIndex.toString(),
                    color = VanishXColors.OnPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                )
            } else if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = VanishXColors.OnPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private sealed interface SelectionToggle {
    val items: List<Uri>

    data class Added(override val items: List<Uri>) : SelectionToggle
    data class Removed(override val items: List<Uri>) : SelectionToggle
    data object MaxReached : SelectionToggle {
        override val items: List<Uri> get() = emptyList()
    }
}

private fun toggleSelection(current: List<Uri>, uri: Uri): SelectionToggle {
    val index = current.indexOf(uri)
    if (index >= 0) {
        return SelectionToggle.Removed(current.toMutableList().also { it.removeAt(index) })
    }
    if (current.size >= MediaLimits.PHOTO_MULTI_SELECT_MAX) {
        return SelectionToggle.MaxReached
    }
    return SelectionToggle.Added(current + uri)
}

private fun loadRecentImageUris(context: Context, limit: Int): List<Uri> {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    return runCatching {
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            buildList {
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    add(ContentUris.withAppendedId(collection, id))
                    count++
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())
}

private const val RECENT_LIMIT = 60
