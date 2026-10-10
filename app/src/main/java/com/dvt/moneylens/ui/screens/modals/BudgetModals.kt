package com.dvt.moneylens.ui.screens.modals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.CategoryAvatar
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetBottomSheet(
    currentTotalBudgetMinor: Long,
    categories: List<Category>,
    budgets: List<Budget>,
    onDismiss: () -> Unit,
    onSaveTotalBudget: (Long) -> Unit,
    onSaveCategoryBudget: (categoryId: String, limitMinor: Long) -> Unit
) {
    val expenseCategories = remember(categories) { categories.filter { it.type == TransactionType.EXPENSE } }
    var totalBudgetStr by remember {
        mutableStateOf(VietnameseCurrencyFormatter.formatMinor(if (currentTotalBudgetMinor > 0) currentTotalBudgetMinor else 20_000_000L))
    }

    val categoryBudgetsMap = remember(budgets, expenseCategories) {
        mutableStateMapOf<String, String>().apply {
            expenseCategories.forEach { cat ->
                val b = budgets.find { it.categoryId == cat.id }?.limitMinor
                    ?: (if (currentTotalBudgetMinor > 0) currentTotalBudgetMinor / expenseCategories.size.coerceAtLeast(1) else 3_000_000L)
                put(cat.id, VietnameseCurrencyFormatter.formatMinor(b))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "Cài đặt hạn mức ngân sách tháng",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            val parsedTotalBudget = VietnameseCurrencyFormatter.parseToMinor(totalBudgetStr)
            CurrencyAmountTextField(
                value = totalBudgetStr,
                onValueChange = { totalBudgetStr = it },
                label = "Tổng ngân sách tháng (VND)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text("Phân bổ ngân sách theo danh mục:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(expenseCategories) { cat ->
                    val currentVal = categoryBudgetsMap[cat.id] ?: VietnameseCurrencyFormatter.formatMinor(3_000_000L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryAvatar(category = cat)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(cat.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        CurrencyAmountTextField(
                            value = currentVal,
                            onValueChange = { categoryBudgetsMap[cat.id] = VietnameseCurrencyFormatter.formatInput(it) },
                            label = "Hạn mức",
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = {
                    val total = parsedTotalBudget.takeIf { it > 0 } ?: 20_000_000L
                    onSaveTotalBudget(total)
                    categoryBudgetsMap.forEach { (catId, strVal) ->
                        val catLimit = VietnameseCurrencyFormatter.parseToMinor(strVal)
                        if (catLimit > 0) {
                            onSaveCategoryBudget(catId, catLimit)
                        }
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Lưu thiết lập ngân sách", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
