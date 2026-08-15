package com.vault.vanishx.presentation.mailbox.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.mailbox.RoomAction
import com.vault.vanishx.presentation.mailbox.RoomUiState
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
internal fun RoomSearchBar(
    uiState: RoomUiState,
    onAction: (RoomAction) -> Unit,
) {
    val matchCount = uiState.searchMatchIds.size
    val indexLabel = if (matchCount == 0) {
        "0/0"
    } else {
        "${uiState.searchMatchIndex + 1}/$matchCount"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VanishXColors.Surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicTextField(
            value = uiState.searchQuery,
            onValueChange = { onAction(RoomAction.SearchQueryChanged(it)) },
            singleLine = true,
            cursorBrush = SolidColor(VanishXColors.Primary),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = VanishXColors.OnSurface),
            modifier = Modifier
                .weight(1f)
                .background(VanishXColors.Surface2, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { inner ->
                if (uiState.searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.room_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VanishXColors.Muted,
                    )
                }
                inner()
            },
        )
        Text(
            text = indexLabel,
            style = MaterialTheme.typography.labelSmall,
            color = VanishXColors.Muted,
        )
        IconButton(
            onClick = { onAction(RoomAction.SearchPrevMatch) },
            enabled = matchCount > 0,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.room_search_prev),
                tint = VanishXColors.OnSurface,
            )
        }
        IconButton(
            onClick = { onAction(RoomAction.SearchNextMatch) },
            enabled = matchCount > 0,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.room_search_next),
                tint = VanishXColors.OnSurface,
            )
        }
        IconButton(onClick = { onAction(RoomAction.DismissRoomSearch) }) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_back),
                tint = VanishXColors.OnSurface,
            )
        }
    }
}
