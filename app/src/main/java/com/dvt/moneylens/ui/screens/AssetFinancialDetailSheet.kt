package com.dvt.moneylens.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.MoneyText
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetFinancialDetailSheet(
    asset: Asset,
    transactions: List<InvestmentTransaction> = emptyList(),
    config: InvestmentTaxConfig,
    fundFeeSchedules: Map<String, FundExitFeeSchedule> = emptyMap(),
    masked: Boolean = false,
    onDismiss: () -> Unit,
    onExecuteAction: (Asset, InvestmentAction) -> Unit = { _, _ -> },
    onEditAsset: (Asset) -> Unit = {},
    onDeleteAsset: (Asset) -> Unit = {},
    onSaveFundSchedule: (FundExitFeeSchedule) -> Unit = {},
    onResetFundSchedule: (String) -> Unit = {}
) {
    val lensColors = LocalMoneyLensColors.current
    var editingFundSchedule by remember { mutableStateOf<FundExitFeeSchedule?>(null) }
    var showFormulasDetail by remember { mutableStateOf(false) }
    val metrics = remember(asset, config, fundFeeSchedules) {
        InvestmentCalculations.calculateAssetMetrics(asset, config, fundFeeSchedules)
    }
    val isNetGain = metrics.netUnrealizedPnlMinor >= 0
    val netPnlColor = if (isNetGain) lensColors.income else lensColors.expense
    val pnlPrefix = if (isNetGain) "▲ +" else "▼ "

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
            // Header with symbol, Edit, Delete and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = asset.symbol.take(3),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = asset.symbol,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEditAsset(asset) }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Chỉnh sửa khoản đầu tư",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onDeleteAsset(asset) }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xóa khoản đầu tư",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Đóng")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Body
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Primary Net P&L Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Text(
                            text = "Lãi / Lỗ ròng dự tính (sau thuế & phí)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (masked) "••••••" else "$pnlPrefix${Money(Math.abs(metrics.netUnrealizedPnlMinor)).formatVnd()}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = netPnlColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (masked) "" else "(${String.format(Locale.US, "%.1f", metrics.netRoiPercent)}% ROI)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = netPnlColor
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Lãi/lỗ trước thuế phí", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val isGrossGain = metrics.unrealizedPnlGrossMinor >= 0
                                Text(
                                    text = if (masked) "••••••" else "${if (isGrossGain) "+" else ""}${Money(metrics.unrealizedPnlGrossMinor).formatVnd()} (${String.format(Locale.US, "%.1f", metrics.unrealizedPnlGrossPercent)}%)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isGrossGain) lensColors.income else lensColors.expense
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Thuế & phí ước tính", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (masked) "••••••" else "-${Money(metrics.estimatedLiquidationFeeAndTaxMinor).formatVnd()}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = lensColors.expense
                                )
                            }
                        }
                    }
                }

                // Financial Breakdown Metrics
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                        Text(
                            text = "Chi tiết định giá & thanh lý",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        MetricRow(label = "Số lượng nắm giữ", value = "${asset.quantity}")
                        MetricRow(label = "Giá vốn bình quân", value = Money(asset.costBasisMinor).formatVnd() + " / đv")
                        MetricRow(label = "Tổng vốn đầu tư", value = if (masked) "••••••" else Money(metrics.totalCostBasisMinor).formatVnd())
                        MetricRow(label = "Giá thị trường hiện tại", value = Money(asset.currentPriceMinor).formatVnd() + " / đv")
                        MetricRow(label = "Tổng giá trị thị trường", value = if (masked) "••••••" else Money(metrics.grossMarketValueMinor).formatVnd())

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        MetricRow(
                            label = "Giá trị thanh lý ròng (nếu bán)",
                            value = if (masked) "••••••" else Money(metrics.netLiquidationValueMinor).formatVnd(),
                            isEmphasized = true
                        )

                        MetricRow(
                            label = "Giá trị thực (sau lạm phát ${metrics.inflationRatePercent}%/năm)",
                            value = if (masked) "••••••" else Money(metrics.realLiquidationValueMinor).formatVnd(),
                            isEmphasized = true
                        )

                        MetricRow(
                            label = "Lãi/lỗ thực tế sau trượt giá",
                            value = if (masked) "••••••" else "${if (metrics.realUnrealizedPnlMinor >= 0) "+" else ""}${Money(metrics.realUnrealizedPnlMinor).formatVnd()} (${String.format(Locale.US, "%.1f", metrics.realRoiPercent)}%)"
                        )

                        MetricRow(
                            label = "Mức giá hòa vốn (Break-even)",
                            value = Money(metrics.breakEvenPricePerUnitMinor).formatVnd() + " / đv"
                        )
                    }
                }

                // Ghi chú tài sản (Hiển thị nổi bật nếu có)
                if (!asset.notes.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ghi chú tài sản",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = asset.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Asset Specific Regulations, Formulas & Tax Info Card (Mặc định thu gọn)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clickable Header to Expand / Collapse
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFormulasDetail = !showFormulasDetail },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Quy tắc tính toán áp dụng",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!showFormulasDetail) {
                                        Text(
                                            text = "Thuế & phí: ${metrics.feeAndTaxRatePercent}% • Chạm để xem công thức chi tiết",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (showFormulasDetail) "Thu gọn" else "Xem chi tiết",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (showFormulasDetail) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (showFormulasDetail) "Thu gọn" else "Xem chi tiết",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Chi tiết chỉ hiển thị khi người dùng chọn mở rộng (Mặc định thu gọn)
                        AnimatedVisibility(visible = showFormulasDetail) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                // Tóm tắt quy định thuế & phí áp dụng
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Quy định thuế & chi phí giao dịch áp dụng:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Text(
                                            text = "• ${metrics.feeBreakdownDescription}",
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp, fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        metrics.specialNotes?.let { notes ->
                                            Text(
                                                text = "• $notes",
                                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Công thức & cách tính chi tiết các giá trị tài chính:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // 1. Giá thị trường hiện tại
                                FormulaItem(
                                    title = "1. Giá thị trường hiện tại (Gross Market Value)",
                                    formula = "Giá thị trường = Số lượng × Đơn giá hiện tại",
                                    example = "${asset.quantity} × ${Money(asset.currentPriceMinor).formatVnd()} = ${Money(metrics.grossMarketValueMinor).formatVnd()}",
                                    explanation = "Tổng giá trị danh nghĩa của khoản đầu tư theo thị giá hiện tại, chưa trừ bất kỳ khoản thuế phí hay chi phí thanh lý nào."
                                )

                                // 2. Tổng vốn đầu tư
                                FormulaItem(
                                    title = "2. Tổng vốn đầu tư (Total Cost Basis)",
                                    formula = "Tổng vốn = Số lượng × Giá vốn bình quân (Cost Basis)",
                                    example = "${asset.quantity} × ${Money(asset.costBasisMinor).formatVnd()} = ${Money(metrics.totalCostBasisMinor).formatVnd()}",
                                    explanation = "Tổng số tiền vốn gốc ban đầu và các lần mua thêm đã bỏ ra để sở hữu số lượng tài sản hiện có."
                                )

                                // 3. Thuế & Phí thanh lý ước tính
                                FormulaItem(
                                    title = "3. Thuế & Phí thanh lý ước tính (Fees & Taxes)",
                                    formula = "Thuế & Phí = Giá thị trường × Tỷ lệ thuế & phí (${metrics.feeAndTaxRatePercent}%)",
                                    example = "${Money(metrics.grossMarketValueMinor).formatVnd()} × ${metrics.feeAndTaxRatePercent}% = ${Money(metrics.estimatedLiquidationFeeAndTaxMinor).formatVnd()}",
                                    explanation = "Bao gồm thuế TNCN chuyển nhượng, phí môi giới sàn giao dịch, hoặc phí chuộc lại CCQ theo thời gian nắm giữ khi bán ra."
                                )

                                // 4. Giá trị thanh lý ròng
                                FormulaItem(
                                    title = "4. Giá trị thanh lý ròng (Net Liquidation Value)",
                                    formula = "Giá thanh lý = Giá thị trường - Thuế & Phí ước tính",
                                    example = "${Money(metrics.grossMarketValueMinor).formatVnd()} - ${Money(metrics.estimatedLiquidationFeeAndTaxMinor).formatVnd()} = ${Money(metrics.netLiquidationValueMinor).formatVnd()}",
                                    explanation = "Số tiền thực nhận sẽ được cộng vào ví/tài khoản tiền mặt nếu bạn thanh lý toàn bộ khoản đầu tư ở thời điểm hiện tại."
                                )

                                // 5. Giá trị thực tế sau lạm phát
                                FormulaItem(
                                    title = "5. Giá trị thực tế sau lạm phát (Real Value)",
                                    formula = "Giá thực = Giá thanh lý ròng ÷ (1 + Lạm phát % / năm)^Số năm nắm giữ",
                                    example = "${Money(metrics.netLiquidationValueMinor).formatVnd()} ÷ (1 + ${metrics.inflationRatePercent}%/năm) = ${Money(metrics.realLiquidationValueMinor).formatVnd()}",
                                    explanation = "Sức mua thực tế của số tiền thanh lý sau khi trừ trượt giá do lạm phát (${metrics.inflationRatePercent}%/năm) theo thời gian nắm giữ."
                                )

                                // 6. Lợi nhuận ròng thực nhận
                                FormulaItem(
                                    title = "6. Lãi/Lỗ ròng thực nhận (Net Unrealized PnL)",
                                    formula = "Lợi nhuận ròng = Giá thanh lý ròng - Tổng vốn đầu tư",
                                    example = "${Money(metrics.netLiquidationValueMinor).formatVnd()} - ${Money(metrics.totalCostBasisMinor).formatVnd()} = ${if (metrics.netUnrealizedPnlMinor >= 0) "+" else ""}${Money(metrics.netUnrealizedPnlMinor).formatVnd()}",
                                    explanation = "Số tiền lãi hoặc lỗ thực tế bỏ túi sau khi đã khấu trừ đầy đủ mọi nghĩa vụ thuế và chi phí chuyển nhượng."
                                )

                                // 7. Tỷ suất sinh lời ròng (Net ROI %)
                                FormulaItem(
                                    title = "7. Tỷ suất sinh lời ròng (Net ROI %)",
                                    formula = "ROI (%) = [(Giá thanh lý ròng - Tổng vốn đầu tư) ÷ Tổng vốn đầu tư] × 100%",
                                    example = "[(${Money(metrics.netLiquidationValueMinor).formatVnd()} - ${Money(metrics.totalCostBasisMinor).formatVnd()}) ÷ ${Money(metrics.totalCostBasisMinor).formatVnd()}] × 100% = ${String.format(Locale.US, "%.1f", metrics.netRoiPercent)}%",
                                    explanation = "Tỷ lệ hoàn vốn đầu tư ròng (Return on Investment) thực tế, phản ánh chính xác hiệu suất sinh lời trên mỗi đồng vốn đã giải ngân."
                                )

                                // 8. Lợi nhuận & ROI thực tế sau lạm phát
                                FormulaItem(
                                    title = "8. Lợi nhuận & ROI thực tế sau lạm phát (Real ROI %)",
                                    formula = "Real PnL = Giá thực - Tổng vốn | Real ROI (%) = (Real PnL ÷ Tổng vốn) × 100%",
                                    example = "${Money(metrics.realLiquidationValueMinor).formatVnd()} - ${Money(metrics.totalCostBasisMinor).formatVnd()} = ${if (metrics.realUnrealizedPnlMinor >= 0) "+" else ""}${Money(metrics.realUnrealizedPnlMinor).formatVnd()} (${String.format(Locale.US, "%.1f", metrics.realRoiPercent)}%)",
                                    explanation = "Lợi nhuận thực sự sau khi bù đắp cả chi phí thanh lý lẫn tỷ lệ trượt giá lạm phát bào mòn sức mua trong suốt thời gian đầu tư."
                                )

                                // 9. Mức giá hòa vốn (Break-even Price)
                                FormulaItem(
                                    title = "9. Mức giá hòa vốn (Break-even Price)",
                                    formula = "Giá hòa vốn = Giá vốn bình quân ÷ (1 - Tỷ lệ thuế & phí %)",
                                    example = "${Money(asset.costBasisMinor).formatVnd()} ÷ (1 - ${metrics.feeAndTaxRatePercent}%) = ${Money(metrics.breakEvenPricePerUnitMinor).formatVnd()} / đv",
                                    explanation = "Mức thị giá tối thiểu cần đạt để khi bán thanh lý toàn bộ, số tiền thu về vừa đủ bù đắp vốn gốc và chi phí giao dịch, không bị lỗ."
                                )
                            }
                        }
                    }
                }


                // Thẻ Biểu Phí Chuộc Lại Riêng cho Quỹ CCQ
                if (asset.type == AssetType.FUND) {
                    val fundSchedule = fundFeeSchedules[asset.symbol.uppercase()]
                        ?: FundExitFeeSchedule.getDefaultSchedule(asset.symbol, asset.name)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Biểu phí chuộc lại riêng: ${fundSchedule.symbol}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text(
                                text = "Mốc thời gian áp dụng: ${fundSchedule.formatSummary()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedButton(
                                onClick = { editingFundSchedule = fundSchedule },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tùy chỉnh biểu phí quỹ này", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Lịch sử giao dịch Mua thêm / Bán bớt chi tiết
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lịch sử mua thêm & bán bớt",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${transactions.size} lần GD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (transactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có lịch sử giao dịch phát sinh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val dateFormat = remember { SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()) }
                            transactions.forEach { tx ->
                                val isBuy = tx.action == InvestmentAction.BUY
                                val isSell = tx.action == InvestmentAction.SELL
                                val isDividend = tx.action == InvestmentAction.DIVIDEND

                                val actionColor = when {
                                    isBuy -> lensColors.income
                                    isSell -> lensColors.expense
                                    isDividend -> Color(0xFFF57F17)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                val actionLabel = when {
                                    isBuy && tx.note.contains("ban đầu", ignoreCase = true) -> "MUA BAN ĐẦU"
                                    isBuy -> "MUA THÊM"
                                    isSell -> "BÁN BỚT"
                                    isDividend -> "CỔ TỨC"
                                    else -> tx.action.name
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = actionColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = actionLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = actionColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = dateFormat.format(Date(tx.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = if (masked) "••••••" else Money(tx.totalAmountMinor).formatVnd(),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = actionColor
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isDividend) "Nhận cổ tức tiền mặt" else "SL: ${if (isBuy) "+" else "-"}${tx.quantity} đv",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            if (!isDividend) {
                                                Text(
                                                    text = "Đơn giá: ${Money(tx.priceMinor).formatVnd()} / đv",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (!tx.accountName.isNullOrBlank() || tx.note.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (!tx.accountName.isNullOrBlank()) {
                                                    Text(
                                                        text = "Ví: ${tx.accountName}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (tx.note.isNotBlank()) {
                                                    Text(
                                                        text = tx.note,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Action Buttons: Mua thêm / Bán bớt & Xóa tài sản
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onExecuteAction(asset, InvestmentAction.BUY)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = lensColors.income)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mua thêm", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onExecuteAction(asset, InvestmentAction.SELL)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = lensColors.expense)
                ) {
                    Icon(imageVector = Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bán bớt", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = {
                    onDeleteAsset(asset)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xóa khoản đầu tư này", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    editingFundSchedule?.let { schedule ->
        FundFeeScheduleDialog(
            initialSchedule = schedule,
            onDismiss = { editingFundSchedule = null },
            onSave = { updated ->
                onSaveFundSchedule(updated)
                editingFundSchedule = null
            },
            onResetToDefault = {
                onResetFundSchedule(schedule.symbol)
            }
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isEmphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isEmphasized) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodySmall,
            color = if (isEmphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isEmphasized) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isEmphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormulaItem(
    title: String,
    formula: String,
    example: String,
    explanation: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formula,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "→ Áp dụng: $example",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = explanation,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
