package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Category
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun ReviewTray(
    pendingTransactions: List<Transaction>,
    allCategories: List<Category>,
    onQuickConfirmCategory: (transaction: Transaction, categoryId: String) -> Unit,
    onOpenConfirmSheet: (transaction: Transaction) -> Unit,
    onViewAllPending: () -> Unit,
    modifier: Modifier = Modifier,
    masked: Boolean = false
) {
    if (pendingTransactions.isEmpty()) return

    val lensColors = LocalMoneyLensColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusRing(active = true, cornerRadius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(lensColors.focusContainer.copy(alpha = 0.35f))
            .padding(Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = lensColors.focus,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "${pendingTransactions.size} giao dịch cần xem lại",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = lensColors.onFocusContainer
                )
            }

            TextButton(
                onClick = onViewAllPending,
                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp)
            ) {
                Text(
                    text = "Xem tất cả",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Display up to 2 items directly in the tray for quick resolution
        pendingTransactions.take(2).forEach { tx ->
            ReviewItemCard(
                transaction = tx,
                allCategories = allCategories,
                onQuickConfirm = { catId -> onQuickConfirmCategory(tx, catId) },
                onOpenMore = { onOpenConfirmSheet(tx) },
                masked = masked
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun ReviewItemCard(
    transaction: Transaction,
    allCategories: List<Category>,
    onQuickConfirm: (String) -> Unit,
    onOpenMore: () -> Unit,
    masked: Boolean
) {
    val isTransfer = remember(transaction) {
        val t = "${transaction.note} ${transaction.counterparty.orEmpty()}".uppercase()
        t.contains("CHUYEN TIEN") || t.contains("CHUYEN KHOAN") || t.contains("IBFT") || t.contains("CHUYEN DI") || t.contains("NHAN TIEN")
    }
    val suggestedCategories = remember(transaction, allCategories, isTransfer) {
        val matched = allCategories.filter { it.type == transaction.type }
        if (isTransfer) {
            val transferCatId = if (transaction.type == TransactionType.INCOME) "cat_transfer_in" else "cat_transfer_out"
            val targetCat = matched.find { it.id == transferCatId }
            if (targetCat != null) {
                listOf(targetCat) + matched.filter { it.id != transferCatId }.take(2)
            } else {
                matched.take(3)
            }
        } else {
            matched.take(3)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.counterparty?.takeIf { it.isNotBlank() }
                            ?: transaction.note.takeIf { it.isNotBlank() }
                            ?: "Giao dịch tự động",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Từ ${transaction.source} · Chạm danh mục để xác nhận ngay",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                MoneyText(
                    amountMinor = transaction.amountMinor,
                    type = transaction.type,
                    masked = masked,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Quick Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                suggestedCategories.forEach { cat ->
                    AssistChip(
                        onClick = { onQuickConfirm(cat.id) },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }

                AssistChip(
                    onClick = onOpenMore,
                    label = { Text("Khác…", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}
