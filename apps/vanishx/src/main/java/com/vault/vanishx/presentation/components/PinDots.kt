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

@Composable
fun PinDots(
    filled: Int,
    total: Int = 4,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(total) { index ->
            Surface(
                modifier = Modifier.size(14.dp),
                shape = RoundedCornerShape(50),
                color = if (index < filled) {
                    VanishXColors.Primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ) {}
        }
    }
}
