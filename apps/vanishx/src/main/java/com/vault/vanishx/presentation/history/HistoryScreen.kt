@file:Suppress("TooManyFunctions")

package com.vault.vanishx.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.vault.vanishx.presentation.conversation.ConversationRow
import com.vault.vanishx.presentation.conversation.ConversationRowModel
import com.vault.vanishx.domain.model.ConversationPreview
import com.vault.vanishx.domain.model.ConversationPreviewKind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniapp.core.mvvm.BaseDestination
import com.miniapp.core.mvvm.BaseScreen
import com.vault.vanishx.R
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.theme.VanishXColors
import com.vault.vanishx.presentation.theme.VanishXTheme
import com.vault.vanishx.presentation.theme.vanishxScreenInsets

private val CardCorner = 16.dp
private const val CARD_BORDER_ALPHA = 0.06f

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    navigator: (BaseDestination) -> Unit,
) = BaseScreen {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.navigator.collectAsEffect { destination -> navigator(destination) }

    HistoryScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun HistoryScreenContent(
    uiState: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VanishXColors.Bg)
            .vanishxScreenInsets(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(HistoryAction.Back) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = VanishXColors.OnSurface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = VanishXColors.OnSurface,
                )
                Text(
                    text = stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = VanishXColors.Muted,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryFilterChip(
                    label = stringResource(R.string.history_filter_open),
                    selected = uiState.filter == HistoryRoomFilter.Open,
                    onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.Open)) },
                )
                HistoryFilterChip(
                    label = stringResource(R.string.history_filter_expired),
                    selected = uiState.filter == HistoryRoomFilter.Expired,
                    onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.Expired)) },
                )
                HistoryFilterChip(
                    label = stringResource(R.string.history_filter_all),
                    selected = uiState.filter == HistoryRoomFilter.All,
                    onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.All)) },
                )
                HistoryFilterChip(
                    label = stringResource(R.string.history_filter_favorite),
                    selected = uiState.filter == HistoryRoomFilter.Favorite,
                    onClick = { onAction(HistoryAction.SetFilter(HistoryRoomFilter.Favorite)) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onAction(HistoryAction.SearchQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.history_search_hint), color = VanishXColors.Muted) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = VanishXColors.Muted)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VanishXColors.Outline,
                    unfocusedBorderColor = VanishXColors.Outline,
                    focusedTextColor = VanishXColors.OnSurface,
                    unfocusedTextColor = VanishXColors.OnSurface,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.rooms.isEmpty()) {
                HistoryEmptyState(
                    hasAnyRooms = uiState.hasAnyRooms,
                    onCreate = { onAction(HistoryAction.CreateRoom) },
                    onJoin = { onAction(HistoryAction.JoinRoom) },
                )
            } else {
                uiState.rooms.forEach { room ->
                    HistoryRoomRow(
                        room = room,
                        isPro = uiState.isPro,
                        onOpen = { onAction(HistoryAction.OpenRoom(room.row.id)) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Text(
            text = stringResource(R.string.history_note),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = VanishXColors.Muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun HistoryEmptyState(
    hasAnyRooms: Boolean,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                if (hasAnyRooms) R.string.history_empty else R.string.history_empty_none,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = VanishXColors.Muted,
            textAlign = TextAlign.Center,
        )
        if (!hasAnyRooms) {
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanishXColors.Primary,
                    contentColor = VanishXColors.OnPrimary,
                ),
            ) {
                Text(stringResource(R.string.home_empty_create))
            }
            OutlinedButton(
                onClick = onJoin,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VanishXColors.Accent),
            ) {
                Text(stringResource(R.string.home_empty_paste))
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VanishXColors.Primary,
            selectedLabelColor = VanishXColors.OnPrimary,
            containerColor = VanishXColors.Surface,
            labelColor = VanishXColors.Muted,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = VanishXColors.Outline,
            selectedBorderColor = VanishXColors.Primary,
        ),
    )
}

@Composable
private fun HistoryRoomRow(
    room: HistoryRoomItem,
    isPro: Boolean,
    onOpen: () -> Unit,
) {
    val row = room.row
    Surface(
        shape = RoundedCornerShape(CardCorner),
        color = VanishXColors.Surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VanishXColors.Primary.copy(alpha = CARD_BORDER_ALPHA),
        ),
    ) {
        Column {
            ConversationRow(
                model = row,
                onClick = onOpen,
                showTtlRing = false,
            )
            Text(
                text = historyMetaLabel(room.meta),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = VanishXColors.Muted,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                when {
                    row.isLeft -> {
                        Text(
                            text = stringResource(R.string.history_action_none),
                            style = MaterialTheme.typography.labelLarge,
                            color = VanishXColors.Muted,
                        )
                    }
                    row.isExpired && !isPro -> {
                        OutlinedButton(
                            onClick = onOpen,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = VanishXColors.Accent,
                            ),
                        ) {
                            Text(text = stringResource(R.string.history_open_pro))
                        }
                    }
                    else -> {
                        Button(
                            onClick = onOpen,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VanishXColors.Primary,
                                contentColor = VanishXColors.OnPrimary,
                            ),
                        ) {
                            Text(text = stringResource(R.string.home_open))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun historyMetaLabel(meta: HistoryRoomMeta): String = when (meta) {
    HistoryRoomMeta.Creator -> stringResource(R.string.history_meta_creator)
    HistoryRoomMeta.Member -> stringResource(R.string.history_meta_member)
    HistoryRoomMeta.Archived -> stringResource(R.string.history_meta_archived)
    HistoryRoomMeta.Left -> stringResource(R.string.history_meta_left)
}

@Preview(showSystemUi = true)
@Composable
private fun HistoryScreenPreview() {
    VanishXTheme {
        HistoryScreenContent(
            uiState = HistoryUiState(
                rooms = listOf(
                    HistoryRoomItem(
                        row = ConversationRowModel(
                            id = "1",
                            displayName = "Kế hoạch cuối tuần",
                            initials = "K",
                            avatarLocalPath = null,
                            preview = ConversationPreview(ConversationPreviewKind.Text, "Hi"),
                            unreadCount = 0,
                            isFavorite = false,
                            isMuted = false,
                            isWaiting = false,
                            isExpired = false,
                            isLeft = false,
                            hasRoomClock = true,
                            ttlFraction = 0.5f,
                            remainingMs = 300_000,
                        ),
                        meta = HistoryRoomMeta.Creator,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
