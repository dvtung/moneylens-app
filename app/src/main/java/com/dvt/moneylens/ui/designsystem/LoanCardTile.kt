package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.ConversionFeeSchedule
import com.dvt.moneylens.domain.model.InterestType
import com.dvt.moneylens.domain.model.Loan
import com.dvt.moneylens.domain.model.LoanStatus
import com.dvt.moneylens.domain.model.LoanType
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

@Composable
fun LoanCardTile(
    loan: Loan,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    masked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    val isBorrowing = loan.type == LoanType.BORROWING
    val primaryThemeColor = runCatching { Color(android.graphics.Color.parseColor(loan.colorHex)) }
        .getOrDefault(if (isBorrowing) MaterialTheme.colorScheme.primary else Color(0xFF8E24AA))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 4.dp)
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Header: Title, Counterparty & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryThemeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBorrowing) Icons.Rounded.RequestQuote else Icons.Rounded.Handshake,
                            contentDescription = null,
                            tint = primaryThemeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = loan.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = loan.counterparty,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Interest / Mode Badge
                val isFlexible = loan.termMonths == 0 || loan.monthlyPaymentMinor == 0L
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = when {
                        isFlexible -> Color(0xFF0A6B5C).copy(alpha = 0.15f)
                        loan.interestType == InterestType.ZERO_PERCENT -> Color(0xFFE8A317).copy(alpha = 0.15f)
                        loan.interestType == InterestType.NO_INTEREST -> MaterialTheme.colorScheme.surfaceVariant
                        else -> primaryThemeColor.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            isFlexible -> "Trả tự do"
                            loan.interestType == InterestType.PERCENT_YEAR -> "${loan.interestRate}%/năm"
                            loan.interestType == InterestType.ZERO_PERCENT -> "0% Trả góp"
                            loan.interestType == InterestType.FIXED_INTEREST -> "Lãi cố định"
                            loan.interestType == InterestType.NO_INTEREST -> "Không tính lãi"
                            else -> "Định kỳ"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            isFlexible -> Color(0xFF0A6B5C)
                            loan.interestType == InterestType.ZERO_PERCENT -> Color(0xFFE8A317)
                            loan.interestType == InterestType.NO_INTEREST -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> primaryThemeColor
                        },
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Debt Progress & Remaining Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isBorrowing) "Dư nợ gốc còn lại" else "Số tiền còn phải thu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (masked) "•••••••• đ" else Money(loan.remainingPrincipalMinor).formatVnd(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (loan.isPaidOff) lensColors.income else if (isBorrowing) lensColors.expense else primaryThemeColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Gốc ban đầu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (masked) "••••••" else Money(loan.originalAmountMinor).formatVnd(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (loan.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (loan.isPaidOff) lensColors.income else primaryThemeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            val startMonthFormatted = java.text.SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(java.util.Date(loan.startDate))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Đã thanh toán: %.1f%% · Bắt đầu: Thg %s", loan.progressPercent, startMonthFormatted),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loan.monthlyPaymentMinor > 0 && !loan.isPaidOff) {
                    Text(
                        text = "Kỳ hạn: ngày ${loan.dueDayOfMonth} hàng tháng",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!loan.isPaidOff) {
                    Text(
                        text = "Trả tự do",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Conversion Fee Info Badge (if configured)
            if (loan.conversionFeeMinor > 0) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (loan.conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) {
                                "Phí chuyển đổi: ${Money(loan.conversionFeeMinor).formatVnd()} · Thu kỳ 1 (${Money(loan.firstMonthPaymentMinor).formatVnd()})"
                            } else {
                                "Phí chuyển đổi: ${Money(loan.conversionFeeMinor).formatVnd()} · Chia đều ${loan.termMonths} tháng (+${Money(loan.conversionFeeMinor / loan.termMonths.coerceAtLeast(1)).formatVnd()}/th)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loan.monthlyPaymentMinor > 0 && !loan.isPaidOff) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        val paymentLabel = when {
                            loan.conversionFeeMinor > 0 && loan.conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH ->
                                "Kỳ 1: ${if (masked) "•••" else Money(loan.firstMonthPaymentMinor).formatVnd()} · Sau: ${if (masked) "•••" else Money(loan.regularMonthlyPaymentMinor).formatVnd()}"
                            loan.conversionFeeMinor > 0 && loan.conversionFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY ->
                                "Định kỳ: ${if (masked) "•••" else Money(loan.regularMonthlyPaymentMinor).formatVnd()}/tháng"
                            else ->
                                "Định kỳ: ${if (masked) "•••" else Money(loan.monthlyPaymentMinor).formatVnd()}/tháng"
                        }
                        Text(
                            text = paymentLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        )
                    }
                } else if (!loan.isPaidOff) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Trả linh hoạt",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Cập nhật thông tin khoản vay",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xóa khoản nợ",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!loan.isPaidOff) {
                        Button(
                            onClick = onPayClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isBorrowing) Icons.Rounded.Payment else Icons.Rounded.Savings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBorrowing) "Trả nợ" else "Thu nợ",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
