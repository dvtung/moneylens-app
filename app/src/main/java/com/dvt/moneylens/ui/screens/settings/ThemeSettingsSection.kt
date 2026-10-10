package com.dvt.moneylens.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.theme.Spacing
import com.dvt.moneylens.ui.theme.ThemeAccent
import com.dvt.moneylens.ui.theme.ThemeMode

@Composable
fun ThemeSettingsSection(
    uiState: MainUiState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectThemeAccent: (ThemeAccent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Giao diện & Chủ đề",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Chế độ hiển thị và màu chủ đạo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Chế độ Sáng/Tối/Hệ thống
            Text(
                text = "Chế độ hiển thị",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> "Hệ thống"
                        ThemeMode.LIGHT -> "Sáng"
                        ThemeMode.DARK -> "Tối"
                    }
                    FilterChip(
                        selected = uiState.themeMode == mode,
                        onClick = { onSelectThemeMode(mode) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            val icon = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Rounded.SettingsSuggest
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                            }
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Bộ màu Accent
            Text(
                text = "Màu sắc chủ đạo (Accent)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                ThemeAccent.entries.forEach { accent ->
                    Surface(
                        shape = CircleShape,
                        color = accent.previewColor,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onSelectThemeAccent(accent) }
                    ) {
                        if (uiState.themeAccent == accent) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
