package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.calc.CreditCardCycleCalculator
import com.dvt.moneylens.domain.model.CreditCard
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing

import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import com.dvt.moneylens.domain.model.CardType

@Composable
fun CreditCardTile(
    card: CreditCard,
    onCardClick: () -> Unit,
    onPayClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    masked: Boolean = false,
    accountBalanceMinor: Long? = null
) {
    val lensColors = LocalMoneyLensColors.current
    val cycleInfo = CreditCardCycleCalculator.calculateCycle(card)
    val cardColor = runCatching { Color(android.graphics.Color.parseColor(card.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    val utilizationPercent = (card.utilizationRate * 100).toInt()
    val isCredit = card.cardType == CardType.CREDIT

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .focusRing(active = isCredit && (cycleInfo.isDueSoon || cycleInfo.isOverdue), cornerRadius = 16.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (card.cardType) {
                                CardType.WALLET -> Icons.Rounded.AccountBalanceWallet
                                CardType.BANK_ACCOUNT -> Icons.Rounded.AccountBalance
                                else -> Icons.Rounded.CreditCard
                            },
                            contentDescription = null,
                            tint = cardColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = card.nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = when (card.cardType) {
                                CardType.CREDIT -> "${card.issuerBank} · ${card.network.name} ••••${card.last4}"
                                CardType.DEBIT -> "${card.issuerBank} · ${card.network.name} ••••${card.last4}"
                                CardType.WALLET -> "${card.issuerBank} ${if (card.last4.isNotBlank() && card.last4 != "0000") "••••${card.last4}" else ""}"
                                CardType.BANK_ACCOUNT -> "${card.issuerBank} · STK: ${card.last4}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Due status or Type pill
                if (isCredit) {
                    DuePill(
                        text = cycleInfo.dueStatusText,
                        isAlert = cycleInfo.isDueSoon || cycleInfo.isOverdue
                    )
                } else {
                    val (pillBg, pillTextColor, pillLabel) = when (card.cardType) {
                        CardType.DEBIT -> Triple(Color(0xFF0D8253).copy(alpha = 0.15f), Color(0xFF0D8253), "Thẻ Debit")
                        CardType.WALLET -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary, "Ví điện tử")
                        CardType.BANK_ACCOUNT -> Triple(Color(0xFF1565C0).copy(alpha = 0.15f), Color(0xFF1565C0), "Tài khoản")
                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "")
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = pillBg
                    ) {
                        Text(
                            text = pillLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = pillTextColor,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Balance Owed & Credit Limit OR Available Balance
            if (isCredit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Dư nợ hiện tại",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        MoneyText(
                            amountMinor = card.currentBalanceOwedMinor,
                            type = TransactionType.EXPENSE,
                            masked = masked,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Hạn mức: ${if (masked) "••••••" else card.creditLimit.formatVnd()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Đã dùng $utilizationPercent%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (utilizationPercent > 70) lensColors.expense else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Utilization Progress Bar
                LinearProgressIndicator(
                    progress = { (card.utilizationRate).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        utilizationPercent > 80 -> lensColors.expense
                        utilizationPercent > 50 -> lensColors.focus
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = when (card.cardType) {
                                CardType.DEBIT -> "Số dư tài khoản thẻ"
                                CardType.BANK_ACCOUNT -> "Số dư tài khoản"
                                else -> "Số dư khả dụng"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        MoneyText(
                            amountMinor = accountBalanceMinor ?: 0L,
                            type = TransactionType.INCOME,
                            masked = masked,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = when (card.cardType) {
                                CardType.DEBIT -> "Nhận & chi tiền"
                                CardType.BANK_ACCOUNT -> "Tài khoản thanh toán"
                                else -> "Ví thanh toán"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Footer Details & Quick Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCredit) {
                    Column {
                        Text(
                            text = "Sao kê: Ngày ${card.statementDay} hàng tháng",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tối thiểu: ${if (masked) "••••••" else cycleInfo.minPaymentRequired.formatVnd()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = when (card.cardType) {
                            CardType.DEBIT -> "Dùng để thanh toán & nhận tiền từ khoản thu"
                            CardType.BANK_ACCOUNT -> "Tài khoản thanh toán & nhận tiền từ khoản thu"
                            else -> "Dùng để chi tiêu & nhận chuyển khoản"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEditClick != null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Sửa thẻ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Xóa thẻ",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isCredit) {
                        Button(
                            onClick = onPayClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Thanh toán", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuePill(
    text: String,
    isAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    val bg = if (isAlert) lensColors.focusContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isAlert) lensColors.onFocusContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAlert) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = textColor
        )
    }
}
