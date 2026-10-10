package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.BudgetBar
import com.dvt.moneylens.ui.designsystem.CategoryAvatar
import com.dvt.moneylens.ui.designsystem.EmptyState
import com.dvt.moneylens.ui.designsystem.FintechScreenTopAppBar
import com.dvt.moneylens.ui.designsystem.MoneyText
import com.dvt.moneylens.ui.designsystem.PrivacyToggle
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    uiState: MainUiState,
    onTogglePrivacy: () -> Unit,
    onOpenBudgetSettings: (() -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    val expenseTxs = uiState.transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = uiState.totalExpenseThisMonthMinor
    val totalBudget = uiState.totalBudgetLimitMinor
    val remaining = totalBudget - totalExpense

    Scaffold(
        topBar = {
            FintechScreenTopAppBar(
                title = "Ngân sách tháng",
                subtitle = null,
                icon = Icons.Rounded.PieChart,
                onOpenSettings = onOpenSettings,
                actions = {
                    PrivacyToggle(isMasked = uiState.isMasked, onToggle = onTogglePrivacy)
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Overall Month Budget Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = "Hạn mức chi tiêu Tháng 10/2026",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isMasked) "••••••" else Money(totalBudget).formatVnd(),
                            style = MoneyHero,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        BudgetBar(
                            spentMinor = totalExpense,
                            limitMinor = totalBudget,
                            masked = uiState.isMasked
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Đã chi: ${if (uiState.isMasked) "••••••" else Money(totalExpense).formatVnd()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val statusText = if (remaining >= 0) "Còn lại:" else "Vượt mức:"
                            Text(
                                text = "$statusText ${if (uiState.isMasked) "••••••" else Money(if (remaining >= 0) remaining else -remaining).formatVnd()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Banner cảnh báo hạn mức chi tiêu tháng (80% hoặc 100%)
            if (uiState.isBudgetCritical100) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = 4.dp)
                            .clickable { onOpenBudgetSettings?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚨 BÁO ĐỘNG: Đã vượt 100% ngân sách tháng!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Đã chi ${uiState.budgetUsagePercent}% (${Money(totalExpense).formatVnd()} / ${Money(totalBudget).formatVnd()}). Chạm để nâng hạn mức.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            } else if (uiState.isBudgetWarning80) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = 4.dp)
                            .clickable { onOpenBudgetSettings?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚠️ CẢNH BÁO: Đã chạm ${uiState.budgetUsagePercent}% ngân sách!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "Bạn đã tiêu ${Money(totalExpense).formatVnd()} / ${Money(totalBudget).formatVnd()}. Hãy kiểm soát chi tiêu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8D3B00)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chi tiết theo danh mục",
                        style = MaterialTheme.typography.titleLarge
                    )

                    AssistChip(
                        onClick = { onOpenBudgetSettings?.invoke() },
                        label = { Text("Chỉnh hạn mức") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.EditCalendar,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Categories list sorted by risk (highest utilization first)
            val budgetItems = uiState.categories
                .filter { it.type == TransactionType.EXPENSE }
                .map { cat ->
                    val spent = expenseTxs.filter { it.categoryId == cat.id }.sumOf { it.amountMinor }
                    val budget = uiState.budgets.find { it.categoryId == cat.id }?.limitMinor ?: 3_000_000L
                    val utilization = if (budget > 0) spent.toDouble() / budget else 0.0
                    BudgetCategoryData(cat, spent, budget, utilization)
                }
                .sortedByDescending { it.utilization }

            if (budgetItems.isEmpty()) {
                item {
                    EmptyState(
                        icon = androidx.compose.material.icons.Icons.Rounded.AccountBalanceWallet,
                        title = "Chưa có danh mục ngân sách",
                        body = "Thiết lập hạn mức ngân sách hàng tháng để kiểm soát chi tiêu hiệu quả.",
                        actionLabel = "Thiết lập ngân sách",
                        onActionClick = { onOpenBudgetSettings?.invoke() }
                    )
                }
            } else {
                items(budgetItems) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryAvatar(category = item.category)
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.category.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Hạn mức: ${if (uiState.isMasked) "••••••" else Money(item.budgetLimit).formatVnd()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    MoneyText(
                                        amountMinor = item.spent,
                                        type = TransactionType.EXPENSE,
                                        masked = uiState.isMasked,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            BudgetBar(
                                spentMinor = item.spent,
                                limitMinor = item.budgetLimit,
                                masked = uiState.isMasked
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BudgetCategoryData(
    val category: com.dvt.moneylens.domain.model.Category,
    val spent: Long,
    val budgetLimit: Long,
    val utilization: Double
)
