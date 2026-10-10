package com.dvt.moneylens.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing

data class MonthlyBarData(
    val monthLabel: String,
    val incomeMinor: Long,
    val expenseMinor: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBarChart(
    monthlyData: List<MonthlyBarData>,
    selectedYear: Int = 2026,
    availableYears: List<Int> = listOf(2026),
    onSelectYear: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    masked: Boolean = false
) {
    val lensColors = LocalMoneyLensColors.current
    var yearMenuExpanded by remember { mutableStateOf(false) }

    val hasAnyData = remember(monthlyData) {
        monthlyData.any { it.incomeMinor > 0L || it.expenseMinor > 0L }
    }

    var selectedIndex by remember(monthlyData) {
        val lastWithData = monthlyData.indexOfLast { it.incomeMinor > 0L || it.expenseMinor > 0L }
        mutableIntStateOf(if (lastWithData >= 0) lastWithData else monthlyData.lastIndex.coerceAtLeast(0))
    }

    val selectedData = monthlyData.getOrNull(selectedIndex) ?: monthlyData.lastOrNull()

    val maxAmount = remember(monthlyData) {
        monthlyData.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }?.coerceAtLeast(1L) ?: 1L
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Header: Title, Year Dropdown & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Thu chi các tháng",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Year Selector Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .padding(start = Spacing.sm)
                                    .clickable {
                                        if (availableYears.size > 1) {
                                            yearMenuExpanded = true
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$selectedYear",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (availableYears.size > 1) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = "Chọn năm",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = yearMenuExpanded,
                                onDismissRequest = { yearMenuExpanded = false }
                            ) {
                                availableYears.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text("Năm $yr", fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            onSelectYear(yr)
                                            yearMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (hasAnyData) "Chạm cột để xem chi tiết từng tháng" else "Chưa có phát sinh thu chi trong năm $selectedYear",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(lensColors.income)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Thu",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(lensColors.expense)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            if (!hasAnyData) {
                // Empty state when data is cleared
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Không có giao dịch nào trong năm $selectedYear",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Chart Canvas / Bar Area with Horizontal Scroll if > 6 months
                val chartHeight = 140.dp
                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .then(if (monthlyData.size > 7) Modifier.horizontalScroll(scrollState) else Modifier),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEachIndexed { index, data ->
                        val isSelected = index == selectedIndex
                        val hasMonthData = data.incomeMinor > 0L || data.expenseMinor > 0L

                        val incomeFraction = if (hasMonthData) {
                            (data.incomeMinor.toFloat() / maxAmount).coerceIn(0.04f, 1f)
                        } else 0.02f

                        val expenseFraction = if (hasMonthData) {
                            (data.expenseMinor.toFloat() / maxAmount).coerceIn(0.04f, 1f)
                        } else 0.02f

                        val animatedIncome by animateFloatAsState(
                            targetValue = incomeFraction,
                            animationSpec = tween(durationMillis = 500),
                            label = "income_bar"
                        )
                        val animatedExpense by animateFloatAsState(
                            targetValue = expenseFraction,
                            animationSpec = tween(durationMillis = 500),
                            label = "expense_bar"
                        )

                        val itemWidth = if (monthlyData.size > 7) 44.dp else Modifier.weight(1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = (if (monthlyData.size > 7) Modifier.width(44.dp) else Modifier.weight(1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else Color.Transparent
                                )
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Paired Bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                            ) {
                                // Income Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(animatedIncome)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(if (hasMonthData) lensColors.income else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )

                                // Expense Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(animatedExpense)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(if (hasMonthData) lensColors.expense else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.xs))

                            // Month Label (e.g. T1, T2.. or Tháng 1)
                            Text(
                                text = data.monthLabel.replace("Tháng ", "T"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Selected Month Detail Info Box
                selectedData?.let { sel ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${sel.monthLabel} năm $selectedYear",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val surplus = sel.incomeMinor - sel.expenseMinor
                                val surplusText = if (surplus >= 0) "Thặng dư" else "Thâm hụt"
                                val surplusColor = if (surplus >= 0) lensColors.income else lensColors.expense
                                Text(
                                    text = "$surplusText: ${if (masked) "••••••" else Money(Math.abs(surplus)).formatVnd()}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = surplusColor
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Thu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (masked) "••••••" else Money(sel.incomeMinor).formatVnd(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = lensColors.income
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Chi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (masked) "••••••" else Money(sel.expenseMinor).formatVnd(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = lensColors.expense
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
