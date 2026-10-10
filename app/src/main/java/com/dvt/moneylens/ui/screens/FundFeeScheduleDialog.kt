package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.FundExitFeeSchedule
import com.dvt.moneylens.domain.model.FundExitFeeTier
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundFeeScheduleDialog(
    initialSchedule: FundExitFeeSchedule,
    onDismiss: () -> Unit,
    onSave: (FundExitFeeSchedule) -> Unit,
    onResetToDefault: () -> Unit
) {
    var symbol by remember { mutableStateOf(initialSchedule.symbol) }
    var fundName by remember { mutableStateOf(initialSchedule.fundName) }
    var tiersList by remember { mutableStateOf(initialSchedule.tiers.toMutableList()) }

    var showTemplateMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Biểu phí chuộc lại: ${symbol.uppercase()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Quy định riêng theo bản cáo bạch của từng quỹ mở",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Body
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Info & Quick Templates
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nạp nhanh mẫu quỹ chuẩn:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Box {
                                TextButton(onClick = { showTemplateMenu = true }) {
                                    Icon(imageVector = Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chọn mẫu quỹ")
                                }
                                DropdownMenu(
                                    expanded = showTemplateMenu,
                                    onDismissRequest = { showTemplateMenu = false }
                                ) {
                                    FundExitFeeSchedule.getPredefinedFundTemplates().forEach { tpl ->
                                        DropdownMenuItem(
                                            text = { Text("${tpl.symbol} · ${tpl.fundName}") },
                                            onClick = {
                                                symbol = tpl.symbol
                                                fundName = tpl.fundName
                                                tiersList = tpl.tiers.toMutableList()
                                                showTemplateMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Tên quỹ: $fundName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tiers List Manager
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Các mốc thời gian nắm giữ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            TextButton(
                                onClick = {
                                    // Add a new tier
                                    val lastMax = tiersList.maxOfOrNull { if (it.maxMonths >= 1200) 0 else it.maxMonths } ?: 12
                                    val newMax = if (lastMax == 0) 12 else lastMax + 6
                                    tiersList = (tiersList + FundExitFeeTier(maxMonths = newMax, feePercent = 0.5))
                                        .sortedBy { it.maxMonths }
                                        .toMutableList()
                                }
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thêm mốc")
                            }
                        }

                        tiersList.forEachIndexed { index, tier ->
                            TierEditRow(
                                tier = tier,
                                isLastTier = index == tiersList.lastIndex,
                                onUpdate = { updatedTier ->
                                    val updated = tiersList.toMutableList()
                                    updated[index] = updatedTier
                                    tiersList = updated
                                },
                                onDelete = if (tiersList.size > 1) {
                                    {
                                        val updated = tiersList.toMutableList()
                                        updated.removeAt(index)
                                        tiersList = updated
                                    }
                                } else null
                            )
                        }
                    }
                }

                // Help Note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hệ thống sẽ đối chiếu số tháng nắm giữ thực tế của từng lô mua với các mốc trên để áp mức phí bán lại chính xác nhất.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        val def = FundExitFeeSchedule.getDefaultSchedule(symbol, fundName)
                        tiersList = def.tiers.toMutableList()
                        onResetToDefault()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mặc định")
                }

                Button(
                    onClick = {
                        val finalSchedule = FundExitFeeSchedule(
                            symbol = symbol.uppercase(),
                            fundName = fundName,
                            tiers = tiersList.sortedBy { it.maxMonths }
                        )
                        onSave(finalSchedule)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lưu biểu phí")
                }
            }
        }
    }
}

private fun Double.format4Decimals(): String {
    val rounded = Math.round(this * 10000.0) / 10000.0
    val formatted = String.format(Locale.US, "%.4f", rounded)
    return formatted.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
}

private fun String.parse4Decimals(defaultVal: Double): Double {
    val clean = this.replace(',', '.').trim()
    val parsed = clean.toDoubleOrNull() ?: defaultVal
    return Math.round(parsed * 10000.0) / 10000.0
}

@Composable
private fun TierEditRow(
    tier: FundExitFeeTier,
    isLastTier: Boolean,
    onUpdate: (FundExitFeeTier) -> Unit,
    onDelete: (() -> Unit)?
) {
    var monthsStr by remember(tier.maxMonths) {
        mutableStateOf(if (tier.maxMonths >= 1200) "99" else tier.maxMonths.toString())
    }
    var feeStr by remember(tier.feePercent) {
        mutableStateOf(tier.feePercent.format4Decimals())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dưới",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = monthsStr,
                onValueChange = { newVal ->
                    monthsStr = newVal
                    val parsedMonths = newVal.toIntOrNull() ?: tier.maxMonths
                    val actualMonths = if (parsedMonths >= 99) Int.MAX_VALUE else parsedMonths
                    onUpdate(tier.copy(maxMonths = actualMonths))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(70.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "tháng:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = feeStr,
                onValueChange = { newVal ->
                    feeStr = newVal
                    val parsedFee = newVal.parse4Decimals(tier.feePercent)
                    onUpdate(tier.copy(feePercent = parsedFee))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.width(90.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                suffix = { Text("%", style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(8.dp)
            )

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Xóa mốc",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}
