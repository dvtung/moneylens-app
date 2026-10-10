package com.dvt.moneylens.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.MoneyRow
import com.dvt.moneylens.ui.theme.MoneySub
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun NetWorthBreakdownCard(
    uiState: MainUiState,
    onNavigateToInvestment: () -> Unit,
    onNavigateToDebts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    var isExpanded by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = "Tổng tài sản hiện có (Ròng)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.isMasked) "•••••••• đ" else Money(uiState.netWorthMinor).formatVnd(),
                            style = MoneyHero,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Thu gọn" else "Xem chi tiết phân bổ",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Two Quick Summary Pills (Tài sản có vs Tổng công nợ)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Total Assets Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = lensColors.income.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = lensColors.income,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Tài sản có",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.isMasked) "••••••" else Money(uiState.totalAllAssetsMinor).formatVnd(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = lensColors.income
                            )
                        }
                    }
                }

                // Total Liabilities Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = lensColors.expense.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = lensColors.expense,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Nợ phải trả",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.isMasked) "••••••" else Money(uiState.totalAllLiabilitiesMinor).formatVnd(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = lensColors.expense
                            )
                        }
                    }
                }
            }

            // Expanded Breakdown Details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = Spacing.md)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHI TIẾT CẤU THÀNH TÀI SẢN:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        )
                        IconButton(
                            onClick = { showExplanationDialog = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = "Giải thích công thức",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    // 1. Tiền mặt & Ngân hàng
                    AssetSourceRow(
                        icon = Icons.Rounded.AccountBalance,
                        iconColor = Color(0xFF1E88E5),
                        title = "Tiền mặt & Tài khoản ngân hàng",
                        amountMinor = uiState.totalCashBalanceMinor,
                        masked = uiState.isMasked,
                        isNegative = false
                    )

                    // 2. Đầu tư (BĐS, Cổ phiếu, Vàng, Crypto...)
                    AssetSourceRow(
                        icon = Icons.Rounded.TrendingUp,
                        iconColor = Color(0xFF0A6B5C),
                        title = "Danh mục đầu tư (BĐS, Cổ phiếu, Vàng...)",
                        subtitle = "Thực tế sau thuế, phí & lạm phát: ${if (uiState.isMasked) "••••••" else Money(uiState.totalNetRealLiquidationValueMinor).formatVnd()}",
                        amountMinor = uiState.totalPortfolioValueMinor,
                        masked = uiState.isMasked,
                        isNegative = false,
                        onClick = onNavigateToInvestment
                    )

                    // 3. Cho vay (Phải thu hồi)
                    AssetSourceRow(
                        icon = Icons.Rounded.Handshake,
                        iconColor = Color(0xFF8E24AA),
                        title = "Khoản cho vay (Phải thu hồi)",
                        amountMinor = uiState.totalLendingReceivableMinor,
                        masked = uiState.isMasked,
                        isNegative = false,
                        onClick = onNavigateToDebts
                    )

                    // 4. Nợ thẻ tín dụng
                    AssetSourceRow(
                        icon = Icons.Rounded.CreditCard,
                        iconColor = Color(0xFFE8A317),
                        title = "Dư nợ thẻ tín dụng",
                        amountMinor = uiState.totalCreditDebtMinor,
                        masked = uiState.isMasked,
                        isNegative = true,
                        onClick = onNavigateToDebts
                    )

                    // 5. Nợ vay & Trả góp
                    AssetSourceRow(
                        icon = Icons.Rounded.RequestQuote,
                        iconColor = Color(0xFFD32F2F),
                        title = "Khoản nợ vay & Mua trả góp",
                        amountMinor = uiState.totalBorrowingDebtMinor,
                        masked = uiState.isMasked,
                        isNegative = true,
                        onClick = onNavigateToDebts
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Dòng tiền tích lũy tháng
                    val monthlyCashflow = uiState.totalIncomeThisMonthMinor - uiState.totalExpenseThisMonthMinor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tích lũy ròng tháng 10:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.isMasked) "••••••" else "${if (monthlyCashflow >= 0) "+" else ""}${Money(monthlyCashflow).formatVnd()}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (monthlyCashflow >= 0) lensColors.income else lensColors.expense
                        )
                    }
                }
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Hiểu đúng về Tài sản của bạn",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = lensColors.income.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "1. Tổng tài sản có (Assets)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = lensColors.income
                            )
                            Text(
                                text = "Toàn bộ của cải bạn đang nắm giữ: Tiền mặt + Tài khoản ngân hàng + Danh mục đầu tư (Cổ phiếu, Vàng, BĐS) + Tiền cho vay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = lensColors.expense.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "2. Tổng nợ phải trả (Liabilities)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = lensColors.expense
                            )
                            Text(
                                text = "Tất cả các khoản nợ cần trả: Dư nợ thẻ tín dụng đã chi tiêu + Các khoản nợ vay ngân hàng & trả góp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "3. Tài sản ròng (Net Worth)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tài sản ròng = Tài sản có - Tổng nợ\nLà số tiền thực sự thuộc về bạn sau khi đã trả sạch mọi khoản nợ.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "💡 Sao Tài sản có lại lớn hơn Tài sản ròng?",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vì Tài sản có = Tài sản ròng + Tổng nợ. Khi bạn có bất kỳ khoản nợ nào (nợ thẻ tín dụng hoặc vay mua sắm), Tài sản có chắc chắn lớn hơn Tài sản ròng. Chúng chỉ bằng nhau khi bạn hoàn toàn không có nợ (Tổng nợ = 0 đ).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "💡 Khi thêm khoản đầu tư, Tài sản ròng cập nhật thế nào?",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• Nếu trích tiền từ tài khoản/ví: Tiền mặt giảm tương ứng và Danh mục đầu tư tăng -> Tài sản ròng được bảo toàn (chỉ trừ đi mức thuế phí thanh lý & lạm phát theo cấu hình).\n• Nếu chọn 'Không liên kết ví' (vốn ngoài bổ sung): Tài sản ròng tăng đúng bằng giá trị thực tế của khoản đầu tư.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExplanationDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đã hiểu")
                }
            }
        )
    }
}

@Composable
private fun AssetSourceRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    amountMinor: Long,
    masked: Boolean,
    isNegative: Boolean,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val lensColors = LocalMoneyLensColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = if (masked) "••••••" else "${if (isNegative && amountMinor > 0) "- " else "+ "}${Money(amountMinor).formatVnd()}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isNegative) lensColors.expense else MaterialTheme.colorScheme.onSurface
        )
    }
}
