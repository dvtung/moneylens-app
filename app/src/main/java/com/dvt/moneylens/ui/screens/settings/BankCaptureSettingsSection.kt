package com.dvt.moneylens.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.calc.BankPackageRegistry
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun BankCaptureSettingsSection(
    uiState: MainUiState,
    onToggleBank: (String) -> Unit,
    onEnableAllBanks: ((List<String>) -> Unit)? = null,
    onDisableAllBanks: (() -> Unit)? = null,
    onToggleNotificationCapture: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

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
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tự động đọc thông báo Ngân hàng",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Nhận diện biến động số dư từ App Ngân hàng & SMS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isNotificationCaptureEnabled,
                    onCheckedChange = { onToggleNotificationCapture(it) }
                )
            }

            if (uiState.isNotificationCaptureEnabled) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isExpanded) "Thu gọn danh sách ngân hàng" else "Cấu hình ngân hàng cho phép (${uiState.enabledBankCodes.size})")
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    val supportedBanks = com.dvt.moneylens.domain.calc.BankParserEngine.allSupportedBanks
                    val allCodes = supportedBanks.map { it.code }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { onEnableAllBanks?.invoke(allCodes) }
                        ) {
                            Text("Bật tất cả")
                        }
                        TextButton(
                            onClick = { onDisableAllBanks?.invoke() }
                        ) {
                            Text("Tắt tất cả")
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    supportedBanks.chunked(2).forEach { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pair.forEach { bank ->
                                val isEnabled = uiState.enabledBankCodes.contains(bank.code)
                                FilterChip(
                                    selected = isEnabled,
                                    onClick = { onToggleBank(bank.code) },
                                    label = { Text(bank.shortName, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
