package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.InvestmentTaxConfig
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentTaxConfigSheet(
    initialConfig: InvestmentTaxConfig,
    fundFeeSchedules: Map<String, com.dvt.moneylens.domain.model.FundExitFeeSchedule> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (InvestmentTaxConfig) -> Unit,
    onResetToDefault: () -> Unit,
    onSaveFundSchedule: (com.dvt.moneylens.domain.model.FundExitFeeSchedule) -> Unit = {},
    onResetFundSchedule: (String) -> Unit = {}
) {
    var stockBrokerageStr by remember { mutableStateOf(initialConfig.stockBrokerageFeePercent.format4Decimals()) }
    var stockSellingTaxStr by remember { mutableStateOf(initialConfig.stockSellingTaxPercent.format4Decimals()) }
    var cashDividendTaxStr by remember { mutableStateOf(initialConfig.cashDividendTaxPercent.format4Decimals()) }

    var fundBuyFeeStr by remember { mutableStateOf(initialConfig.fundBuyingFeePercent.format4Decimals()) }
    var fundSellTaxStr by remember { mutableStateOf(initialConfig.fundSellingTaxPercent.format4Decimals()) }
    var fundExitTier1Str by remember { mutableStateOf(initialConfig.fundExitFeeTier1Percent.format4Decimals()) }
    var fundExitTier2Str by remember { mutableStateOf(initialConfig.fundExitFeeTier2Percent.format4Decimals()) }
    var fundExitTier3Str by remember { mutableStateOf(initialConfig.fundExitFeeTier3Percent.format4Decimals()) }

    var reTaxStr by remember { mutableStateOf(initialConfig.realEstateTransferTaxPercent.format4Decimals()) }
    var reBrokerageStr by remember { mutableStateOf(initialConfig.realEstateBrokerageFeePercent.format4Decimals()) }

    // Chỉ số lạm phát & trượt giá (%)
    var generalInflationStr by remember { mutableStateOf(initialConfig.generalInflationRatePercent.format4Decimals()) }
    var stockInflationStr by remember { mutableStateOf(initialConfig.stockInflationRatePercent.format4Decimals()) }
    var reInflationStr by remember { mutableStateOf(initialConfig.realEstateInflationRatePercent.format4Decimals()) }
    var goldInflationStr by remember { mutableStateOf(initialConfig.goldInflationRatePercent.format4Decimals()) }
    var cryptoInflationStr by remember { mutableStateOf(initialConfig.cryptoInflationRatePercent.format4Decimals()) }
    var fundInflationStr by remember { mutableStateOf(initialConfig.fundInflationRatePercent.format4Decimals()) }

    // Quản lý biểu phí riêng theo từng mã quỹ CCQ
    var selectedFundSymbol by remember { mutableStateOf("DCDS") }
    var editingFundSchedule by remember { mutableStateOf<com.dvt.moneylens.domain.model.FundExitFeeSchedule?>(null) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cài đặt Thuế & Phí Đầu tư",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Giá trị làm tròn 4 số thập phân, tự động áp dụng vào tính toán ròng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Scrollable Form
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Section 1: Chứng khoán (Cổ phiếu)
                TaxCategoryCard(
                    title = "1. Cổ phiếu & Chứng khoán",
                    icon = Icons.Rounded.TrendingUp,
                    color = Color(0xFF1E88E5)
                ) {
                    TaxInputField(
                        label = "Phí môi giới GD bán (%)",
                        hint = "Mặc định 0.2%",
                        value = stockBrokerageStr,
                        onValueChange = { stockBrokerageStr = it }
                    )
                    TaxInputField(
                        label = "Thuế TNCN bán CK (%)",
                        hint = "Mặc định 0.1%",
                        value = stockSellingTaxStr,
                        onValueChange = { stockSellingTaxStr = it }
                    )
                    TaxInputField(
                        label = "Thuế cổ tức tiền mặt (%)",
                        hint = "Mặc định 5.0%",
                        value = cashDividendTaxStr,
                        onValueChange = { cashDividendTaxStr = it }
                    )
                }

                // Section 2: Chứng chỉ quỹ mở (CCQ)
                TaxCategoryCard(
                    title = "2. Chứng chỉ quỹ mở (CCQ)",
                    icon = Icons.Rounded.PieChart,
                    color = Color(0xFF0A6B5C)
                ) {
                    TaxInputField(
                        label = "Phí mua CCQ (%)",
                        hint = "Mặc định 0.5%",
                        value = fundBuyFeeStr,
                        onValueChange = { fundBuyFeeStr = it }
                    )
                    TaxInputField(
                        label = "Thuế TNCN bán CCQ (%)",
                        hint = "Mặc định 0.1%",
                        value = fundSellTaxStr,
                        onValueChange = { fundSellTaxStr = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Biểu phí bán lại / chuộc lại CCQ theo thời gian nắm giữ:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TaxInputField(
                        label = "Nắm giữ dưới 12 tháng (< 1 năm) (%)",
                        hint = "Mặc định 1.0%",
                        value = fundExitTier1Str,
                        onValueChange = { fundExitTier1Str = it }
                    )
                    TaxInputField(
                        label = "Nắm giữ từ 12 - 24 tháng (1 - 2 năm) (%)",
                        hint = "Mặc định 0.5%",
                        value = fundExitTier2Str,
                        onValueChange = { fundExitTier2Str = it }
                    )
                    TaxInputField(
                        label = "Nắm giữ từ 24 tháng trở lên (≥ 2 năm) (%)",
                        hint = "Mặc định 0.0% (Miễn phí)",
                        value = fundExitTier3Str,
                        onValueChange = { fundExitTier3Str = it }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Biểu phí chuộc lại riêng theo từng mã quỹ (CCQ):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tùy chỉnh số tháng và % phí chuộc lại theo bản cáo bạch từng quỹ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Fund Symbol Chips Row
                    val fundSymbols = listOf("DCDS", "VESAF", "VEOF", "SSISCA", "DCBC")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fundSymbols.forEach { sym ->
                            FilterChip(
                                selected = selectedFundSymbol == sym,
                                onClick = { selectedFundSymbol = sym },
                                label = { Text(sym, fontSize = 11.sp) }
                            )
                        }
                    }

                    val activeSchedule = fundFeeSchedules[selectedFundSymbol]
                        ?: com.dvt.moneylens.domain.model.FundExitFeeSchedule.getDefaultSchedule(selectedFundSymbol)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "${activeSchedule.symbol} · ${activeSchedule.fundName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mốc phí: ${activeSchedule.formatSummary()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { editingFundSchedule = activeSchedule },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tùy chỉnh mốc phí quỹ ${activeSchedule.symbol}", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section 3: Bất động sản
                TaxCategoryCard(
                    title = "3. Bất động sản (BĐS)",
                    icon = Icons.Rounded.Home,
                    color = Color(0xFFFF7043)
                ) {
                    TaxInputField(
                        label = "Thuế TNCN chuyển nhượng BĐS (%)",
                        hint = "Mặc định 2.0%",
                        value = reTaxStr,
                        onValueChange = { reTaxStr = it }
                    )
                    TaxInputField(
                        label = "Phí hoa hồng môi giới khi bán (%)",
                        hint = "Mặc định 1.0%",
                        value = reBrokerageStr,
                        onValueChange = { reBrokerageStr = it }
                    )
                }

                // Section 4: Vàng & Kim loại
                TaxCategoryCard(
                    title = "4. Vàng & Kim loại quý",
                    icon = Icons.Rounded.Stars,
                    color = Color(0xFFE8A317)
                ) {
                    Text(
                        text = "• Định giá danh mục: Tự động dùng GIÁ MUA VÀO CỦA TIỆM (Bid price) để phản ánh đúng số tiền thực thu về, tránh ảo tưởng lợi nhuận do spread chênh lệch.\n• Thuế TNCN: 0% theo quy định cho vàng vật chất cá nhân.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Section 5: Chỉ số Lạm phát & Sức mua thực tế
                TaxCategoryCard(
                    title = "5. Chỉ số Lạm phát & Sức mua thực tế",
                    icon = Icons.Rounded.PriceChange,
                    color = Color(0xFF9C27B0)
                ) {
                    Text(
                        text = "Lạm phát được dùng để tính toán Giá trị thực sau lạm phát và Tỷ suất sinh lời thực tế (Real ROI) theo thời gian nắm giữ, phản ánh chính xác sức mua của tài sản.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    TaxInputField(
                        label = "Tỷ lệ lạm phát mục tiêu chung (%/năm)",
                        hint = "Mặc định 4.0%/năm",
                        value = generalInflationStr,
                        onValueChange = {
                            generalInflationStr = it
                            stockInflationStr = it
                            reInflationStr = it
                            goldInflationStr = it
                            cryptoInflationStr = it
                            fundInflationStr = it
                        }
                    )

                    Text(
                        text = "Tùy chỉnh tỷ lệ lạm phát riêng theo từng loại tài sản:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = stockInflationStr,
                            onValueChange = { stockInflationStr = it },
                            label = { Text("Cổ phiếu (%/năm)", maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                            suffix = { Text("%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = reInflationStr,
                            onValueChange = { reInflationStr = it },
                            label = { Text("Bất động sản (%/năm)", maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                            suffix = { Text("%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = goldInflationStr,
                            onValueChange = { goldInflationStr = it },
                            label = { Text("Vàng (%/năm)", maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                            suffix = { Text("%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = cryptoInflationStr,
                            onValueChange = { cryptoInflationStr = it },
                            label = { Text("Crypto (%/năm)", maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                            suffix = { Text("%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    TaxInputField(
                        label = "Quỹ mở CCQ (%/năm)",
                        hint = "Mặc định 4.0%",
                        value = fundInflationStr,
                        onValueChange = { fundInflationStr = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        val def = InvestmentTaxConfig()
                        stockBrokerageStr = def.stockBrokerageFeePercent.format4Decimals()
                        stockSellingTaxStr = def.stockSellingTaxPercent.format4Decimals()
                        cashDividendTaxStr = def.cashDividendTaxPercent.format4Decimals()
                        fundBuyFeeStr = def.fundBuyingFeePercent.format4Decimals()
                        fundSellTaxStr = def.fundSellingTaxPercent.format4Decimals()
                        fundExitTier1Str = def.fundExitFeeTier1Percent.format4Decimals()
                        fundExitTier2Str = def.fundExitFeeTier2Percent.format4Decimals()
                        fundExitTier3Str = def.fundExitFeeTier3Percent.format4Decimals()
                        reTaxStr = def.realEstateTransferTaxPercent.format4Decimals()
                        reBrokerageStr = def.realEstateBrokerageFeePercent.format4Decimals()
                        generalInflationStr = def.generalInflationRatePercent.format4Decimals()
                        stockInflationStr = def.stockInflationRatePercent.format4Decimals()
                        reInflationStr = def.realEstateInflationRatePercent.format4Decimals()
                        goldInflationStr = def.goldInflationRatePercent.format4Decimals()
                        cryptoInflationStr = def.cryptoInflationRatePercent.format4Decimals()
                        fundInflationStr = def.fundInflationRatePercent.format4Decimals()
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
                        val updated = InvestmentTaxConfig(
                            stockBrokerageFeePercent = stockBrokerageStr.parse4Decimals(0.2),
                            stockSellingTaxPercent = stockSellingTaxStr.parse4Decimals(0.1),
                            cashDividendTaxPercent = cashDividendTaxStr.parse4Decimals(5.0),
                            fundBuyingFeePercent = fundBuyFeeStr.parse4Decimals(0.5),
                            fundSellingTaxPercent = fundSellTaxStr.parse4Decimals(0.1),
                            fundExitFeeTier1Percent = fundExitTier1Str.parse4Decimals(1.0),
                            fundExitFeeTier2Percent = fundExitTier2Str.parse4Decimals(0.5),
                            fundExitFeeTier3Percent = fundExitTier3Str.parse4Decimals(0.0),
                            realEstateTransferTaxPercent = reTaxStr.parse4Decimals(2.0),
                            realEstateBrokerageFeePercent = reBrokerageStr.parse4Decimals(1.0),
                            generalInflationRatePercent = generalInflationStr.parse4Decimals(4.0),
                            stockInflationRatePercent = stockInflationStr.parse4Decimals(4.0),
                            realEstateInflationRatePercent = reInflationStr.parse4Decimals(4.0),
                            goldInflationRatePercent = goldInflationStr.parse4Decimals(4.0),
                            cryptoInflationRatePercent = cryptoInflationStr.parse4Decimals(4.0),
                            fundInflationRatePercent = fundInflationStr.parse4Decimals(4.0)
                        )
                        onSave(updated)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lưu thiết lập")
                }
            }
        }
    }

    editingFundSchedule?.let { schedule ->
        FundFeeScheduleDialog(
            initialSchedule = schedule,
            onDismiss = { editingFundSchedule = null },
            onSave = { updatedSchedule ->
                onSaveFundSchedule(updatedSchedule)
                editingFundSchedule = null
            },
            onResetToDefault = {
                onResetFundSchedule(schedule.symbol)
            }
        )
    }
}

@Composable
private fun TaxCategoryCard(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun TaxInputField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hint.isNotBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.width(96.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            shape = RoundedCornerShape(8.dp),
            suffix = { Text("%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) }
        )
    }
}
