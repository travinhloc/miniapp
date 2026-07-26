package com.vault.vanishx.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vault.vanishx.presentation.theme.VanishXColors

enum class VanishXAlertTone {
    Primary,
    Accent,
    Danger,
    Warn,
}

@Composable
fun VanishXAlertDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    tone: VanishXAlertTone = VanishXAlertTone.Primary,
    dismissLabel: String? = null,
) {
    val confirmColors = when (tone) {
        VanishXAlertTone.Primary -> ButtonDefaults.buttonColors(
            containerColor = VanishXColors.Primary,
            contentColor = VanishXColors.OnPrimary,
        )
        VanishXAlertTone.Accent -> ButtonDefaults.buttonColors(
            containerColor = VanishXColors.Accent,
            contentColor = VanishXColors.OnSurface,
        )
        VanishXAlertTone.Danger -> ButtonDefaults.buttonColors(
            containerColor = VanishXColors.Error,
            contentColor = VanishXColors.OnSurface,
        )
        VanishXAlertTone.Warn -> ButtonDefaults.buttonColors(
            containerColor = VanishXColors.Warn,
            contentColor = VanishXColors.OnPrimary,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = VanishXColors.Surface,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = confirmColors,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.45f),
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = dismissLabel?.let { label ->
            {
                TextButton(onClick = onDismiss) {
                    Text(text = label)
                }
            }
        },
    )
}
