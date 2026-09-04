@file:Suppress("TooManyFunctions")

package com.vault.vanishx.presentation.components

import com.vault.vanishx.presentation.icons.VanishXIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vault.vanishx.R
import com.vault.vanishx.presentation.theme.VanishXColors

private val CardCorner = 16.dp
private val LeadingCorner = 12.dp
private val LeadingSize = 40.dp
private const val CARD_BORDER_ALPHA = 0.06f
private const val ID_CHIP_BORDER_ALPHA = 0.2f
private const val ID_CHIP_BG_ALPHA = 0.08f
private const val LEADING_PRIMARY_ALPHA = 0.1f
private const val LEADING_WARN_ALPHA = 0.12f
private const val LEADING_ACCENT_ALPHA = 0.15f

enum class SettingsLeadingTone {
    Primary,
    Warn,
    Accent,
}

@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = VanishXIcons.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = VanishXColors.OnSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.15.sp,
            ),
            color = VanishXColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SettingsGroupLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        ),
        color = VanishXColors.Muted,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 0.dp),
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        color = VanishXColors.Surface,
        border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = CARD_BORDER_ALPHA)),
        content = {
            Column(content = content)
        },
    )
}

@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = VanishXIcons.Settings,
    leadingTone: SettingsLeadingTone = SettingsLeadingTone.Primary,
    trailingStatus: String? = null,
    trailingStatusAccent: Boolean = false,
    showChevron: Boolean = onClick != null,
) {
    val clickable = onClick != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (clickable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsLeadingIcon(icon = leadingIcon, tone = leadingTone)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = VanishXColors.Muted,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            trailingStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = when {
                        trailingStatusAccent -> VanishXColors.Accent
                        status == stringResource(R.string.settings_status_on) -> VanishXColors.Primary
                        else -> VanishXColors.Muted
                    },
                )
            }
            if (showChevron) {
                Text(
                    text = stringResource(R.string.settings_chevron),
                    style = MaterialTheme.typography.titleMedium,
                    color = VanishXColors.Muted,
                )
            }
        }
    }
}

@Composable
fun SettingsRowDivider() {
    HorizontalDivider(color = VanishXColors.Outline, thickness = 1.dp)
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = VanishXIcons.Lock,
    leadingTone: SettingsLeadingTone = SettingsLeadingTone.Primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsLeadingIcon(icon = leadingIcon, tone = leadingTone)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = VanishXColors.Muted,
                )
            }
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun SettingsIdentityChip(
    anonymousId: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = VanishXColors.Primary.copy(alpha = ID_CHIP_BG_ALPHA),
        border = BorderStroke(1.dp, VanishXColors.Primary.copy(alpha = ID_CHIP_BORDER_ALPHA)),
    ) {
        Text(
            text = truncateId(anonymousId),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
            ),
            color = VanishXColors.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SettingsDangerNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = VanishXColors.Muted,
        modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
fun SettingsIdentityRow(
    title: String,
    subtitle: String,
    anonymousId: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsLeadingIcon(icon = VanishXIcons.Lock, tone = SettingsLeadingTone.Primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = VanishXColors.OnSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = VanishXColors.Muted,
            )
        }
        anonymousId?.let { SettingsIdentityChip(anonymousId = it) }
    }
}

@Composable
private fun SettingsLeadingIcon(
    icon: ImageVector,
    tone: SettingsLeadingTone,
) {
    val colors = when (tone) {
        SettingsLeadingTone.Primary -> {
            VanishXColors.Primary.copy(alpha = LEADING_PRIMARY_ALPHA) to VanishXColors.Primary
        }
        SettingsLeadingTone.Warn -> {
            VanishXColors.Error.copy(alpha = LEADING_WARN_ALPHA) to VanishXColors.Error
        }
        SettingsLeadingTone.Accent -> {
            VanishXColors.Accent.copy(alpha = LEADING_ACCENT_ALPHA) to VanishXColors.Accent
        }
    }
    val (bg, tint) = colors
    Box(
        modifier = Modifier
            .size(LeadingSize)
            .clip(RoundedCornerShape(LeadingCorner))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun truncateId(id: String): String {
    if (id.length <= ID_VISIBLE_CHARS) return id
    return id.take(ID_PREFIX_CHARS) + "…" + id.takeLast(ID_SUFFIX_CHARS)
}

private const val ID_VISIBLE_CHARS = 14
private const val ID_PREFIX_CHARS = 6
private const val ID_SUFFIX_CHARS = 2
