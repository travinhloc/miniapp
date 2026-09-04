package com.vault.vanishx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

/** Compact top strip when the device has no validated Internet. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VanishXColors.Warn)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.network_offline_title),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = OfflineBannerOnWarn,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.network_offline_body),
            style = MaterialTheme.typography.labelSmall,
            color = OfflineBannerOnWarn.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private val OfflineBannerOnWarn = Color(0xFF1A1200)
