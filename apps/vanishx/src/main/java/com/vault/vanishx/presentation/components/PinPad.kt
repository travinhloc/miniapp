package com.vault.vanishx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vault.vanishx.presentation.theme.VanishXColors

private val KeySize = 56.dp
private val KeyGap = 10.dp
private val PadMaxWidth = 280.dp

@Composable
fun PinPad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    submitEnabled: Boolean = false,
    submitLabel: String = "OK",
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("OK", "0", "⌫"),
    )
    Column(
        modifier = modifier
            .widthIn(max = PadMaxWidth)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(KeyGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { key ->
                    when (key) {
                        "OK" -> if (onSubmit != null) {
                            DigitKey(
                                label = submitLabel,
                                enabled = enabled && submitEnabled,
                                onClick = onSubmit,
                                tint = VanishXColors.Primary,
                                elevated = false,
                            )
                        } else {
                            Box(modifier = Modifier.size(KeySize))
                        }
                        "⌫" -> DigitKey(
                            label = "⌫",
                            enabled = enabled,
                            onClick = onBackspace,
                            tint = VanishXColors.Primary,
                            elevated = false,
                        )
                        else -> DigitKey(
                            label = key,
                            enabled = enabled,
                            onClick = { onDigit(key.first()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = VanishXColors.OnSurface,
    elevated: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(KeySize)
            .then(if (elevated) Modifier.shadow(2.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(if (elevated) VanishXColors.Surface else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (label.length > 1) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.titleLarge
            },
            color = if (enabled) tint else tint.copy(alpha = 0.35f),
            textAlign = TextAlign.Center,
        )
    }
}
