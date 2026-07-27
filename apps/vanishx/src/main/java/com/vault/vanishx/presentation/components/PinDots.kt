package com.vault.vanishx.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vault.vanishx.presentation.theme.VanishXColors

private val DotSize = 14.dp
private val DotGap = 12.dp
private const val DEFAULT_PIN_LENGTH = 4

@Composable
fun PinDots(
    filled: Int,
    modifier: Modifier = Modifier,
    total: Int = DEFAULT_PIN_LENGTH,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        repeat(total) { index ->
            Surface(
                modifier = Modifier.size(DotSize),
                shape = RoundedCornerShape(percent = 50),
                color = if (index < filled) {
                    VanishXColors.Primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ) {}
        }
    }
}
