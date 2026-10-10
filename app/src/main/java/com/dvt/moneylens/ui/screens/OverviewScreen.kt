package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.calc.CreditCardCycleCalculator
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.*
import androidx.compose.ui.graphics.Color
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.MoneyRow
import com.dvt.moneylens.ui.theme.MoneySub
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    uiState: MainUiState,
    onQuickConfirmCategory: (Transaction, String) -> Unit,
    onOpenConfirmSheet: (Transaction) -> Unit,
    onViewAllTransactions: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToInvestment: () -> Unit,
    onNavigateToCard: (String) -> Unit,
    onPayCardClick: (String) -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenBudgetSettings: (() -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    onSelectYear: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current

    Scaffold(
        topBar = {
            OverviewTopAppBar(
                uiState = uiState,
                onOpenSettings = onOpenSettings,
                onTogglePrivacy = onTogglePrivacy
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
            // 1. Tổng Tài Sản Ròng Hiện Có Đa Nguồn (Net Worth Breakdown)
            item {
                NetWorthBreakdownCard(
                    uiState = uiState,
                    onNavigateToInvestment = onNavigateToInvestment,
                    onNavigateToDebts = onNavigateToDebts,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
            }

            // 1b. Biểu Đồ Đường Cong Xu Hướng Tài Sản Ròng (Historical Net Worth Trend Chart)
            item {
                NetWorthTrendChart(
                    allPoints = uiState.historicalNetWorthPoints,
                    currentNetWorthMinor = uiState.netWorthMinor,
                    masked = uiState.isMasked,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
            }

            // 2. Hero Financial Status - Ngân sách chi tiêu tháng
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = "Còn có thể chi trong tháng",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isMasked) "••••••" else Money(uiState.availableToSpendMinor).formatVnd(),
                            style = MoneyHero,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Overall Month Progress
                        val totalExpense = uiState.totalExpenseThisMonthMinor
                        val totalBudget = uiState.totalBudgetLimitMinor
                        val spentPercent = if (totalBudget > 0) ((totalExpense.toDouble() / totalBudget) * 100).toInt() else 0

                        BudgetBar(
                            spentMinor = totalExpense,
                            limitMinor = totalBudget,
                            masked = uiState.isMasked
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Thu nhập",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    amountMinor = uiState.totalIncomeThisMonthMinor,
                                    type = TransactionType.INCOME,
                                    masked = uiState.isMasked,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Tổng chi tiêu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    amountMinor = uiState.totalExpenseThisMonthMinor,
                                    type = TransactionType.EXPENSE,
                                    masked = uiState.isMasked,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
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
                                    text = "Đã chi ${uiState.budgetUsagePercent}% (${Money(uiState.totalExpenseThisMonthMinor).formatVnd()} / ${Money(uiState.totalBudgetLimitMinor).formatVnd()}). Chạm để nâng hạn mức.",
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
                                    text = "Bạn đã tiêu ${Money(uiState.totalExpenseThisMonthMinor).formatVnd()} / ${Money(uiState.totalBudgetLimitMinor).formatVnd()}. Hãy kiểm soát chi tiêu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8D3B00)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Review Tray (Amber Focus Ring) if pending items exist
            if (uiState.pendingTransactions.isNotEmpty()) {
                item {
                    ReviewTray(
                        pendingTransactions = uiState.pendingTransactions,
                        allCategories = uiState.categories,
                        onQuickConfirmCategory = onQuickConfirmCategory,
                        onOpenConfirmSheet = onOpenConfirmSheet,
                        onViewAllPending = onViewAllTransactions,
                        masked = uiState.isMasked,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    )
                }
            }

            // 3. Urgent Card Due Alerts
            val upcomingDueCards = uiState.creditCards.map { card ->
                Pair(card, CreditCardCycleCalculator.calculateCycle(card))
            }.filter { it.second.isDueSoon || it.second.isOverdue }

            if (upcomingDueCards.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                        upcomingDueCards.forEach { (card, cycle) ->
                            AlertItem(
                                title = "${card.nickname} (••••${card.last4})",
                                body = "${cycle.dueStatusText} · Tối thiểu: ${if (uiState.isMasked) "••••••" else cycle.minPaymentRequired.formatVnd()}",
                                actionLabel = "Thanh toán",
                                onActionClick = { onPayCardClick(card.id) },
                                isWarning = true,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            )
                        }
                    }
                }
            }

            // 4. Monthly Income / Expense Chart
            item {
                MonthlyBarChart(
                    monthlyData = uiState.monthlyBarData,
                    selectedYear = uiState.selectedChartYear,
                    availableYears = uiState.availableChartYears,
                    onSelectYear = onSelectYear,
                    masked = uiState.isMasked,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
            }

            // 5. Spending by Category (Top Categories)
            item {
                SectionHeader(
                    title = "Chi theo danh mục",
                    actionLabel = "Cài đặt",
                    onActionClick = { onOpenBudgetSettings?.invoke() ?: onNavigateToBudget() }
                )
            }

            val expenseTxs = uiState.transactions.filter { it.type == TransactionType.EXPENSE }
            val topCategories = uiState.categories
                .filter { it.type == TransactionType.EXPENSE }
                .map { cat ->
                    val spent = expenseTxs.filter { it.categoryId == cat.id }.sumOf { it.amountMinor }
                    val budget = uiState.budgets.find { it.categoryId == cat.id }?.limitMinor ?: 3_000_000L
                    Triple(cat, spent, budget)
                }
                .sortedByDescending { it.second }
                .take(4)

            items(topCategories) { (cat, spent, limit) ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryAvatar(category = cat)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                MoneyText(
                                    amountMinor = spent,
                                    type = TransactionType.EXPENSE,
                                    masked = uiState.isMasked,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            BudgetBar(spentMinor = spent, limitMinor = limit, masked = uiState.isMasked)
                        }
                    }
                }
            }

            // 5. Investment Snapshot
            item {
                SectionHeader(
                    title = "Đầu tư & Tích sản",
                    actionLabel = "Xem chi tiết",
                    onActionClick = onNavigateToInvestment
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                        .clickable(onClick = onNavigateToInvestment)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tổng giá trị danh mục",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.isMasked) "••••••" else Money(uiState.totalPortfolioValueMinor).formatVnd(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        val isGain = uiState.totalUnrealizedPnlMinor >= 0
                        val pnlPrefix = if (isGain) "▲ +" else "▼ "
                        val pnlColor = if (isGain) lensColors.income else lensColors.expense

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = pnlColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$pnlPrefix${String.format("%.1f", uiState.overallPnlPercent)}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = pnlColor,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                            )
                        }
                    }
                }
            }

            // 6. Recent Transactions
            item {
                SectionHeader(
                    title = "Giao dịch gần đây",
                    actionLabel = "Xem tất cả",
                    onActionClick = onViewAllTransactions
                )
            }

            val recentTxs = uiState.transactions.take(5)
            if (recentTxs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "Chưa có giao dịch",
                        body = "Giao dịch từ ngân hàng sẽ tự hiện ở đây hoặc bạn có thể thêm thủ công."
                    )
                }
            } else {
                items(recentTxs) { tx ->
                    val cat = uiState.categories.find { it.id == tx.categoryId }
                    val acc = uiState.accounts.find { it.id == tx.accountId }
                    val card = tx.creditCardId?.let { id -> uiState.creditCards.find { it.id == id } }
                        ?: uiState.creditCards.find { it.accountId == tx.accountId }
                    TransactionRow(
                        transaction = tx,
                        category = cat,
                        account = acc,
                        creditCard = card,
                        onClick = { onOpenConfirmSheet(tx) },
                        masked = uiState.isMasked
                    )
                }
            }
        }
    }
}
