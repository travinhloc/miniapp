package com.vault.vanishx.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .background(VanishXColors.Surface2, OfflineBannerShape)
            .border(BorderStroke(1.dp, VanishXColors.Warn), OfflineBannerShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.network_offline_title),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = VanishXColors.Warn,
        )
        Text(
            text = stringResource(R.string.network_offline_body),
            style = MaterialTheme.typography.bodySmall,
            color = VanishXColors.OnSurface,
        )
    }
}

private val OfflineBannerShape = RoundedCornerShape(12.dp)
