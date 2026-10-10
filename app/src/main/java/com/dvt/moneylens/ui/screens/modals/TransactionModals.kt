package com.dvt.moneylens.ui.screens.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.DateTimePickerField
import com.dvt.moneylens.ui.designsystem.MoneyText
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.screens.formatManagedSourceChipLabel
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    accounts: List<Account>,
    categories: List<Category>,
    creditCards: List<CreditCard> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (amountMinor: Long, type: TransactionType, categoryId: String?, accountId: String, note: String, paymentSource: PaymentSourceType, creditCardId: String?, occurredAt: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var paymentSource by remember {
        mutableStateOf(if (creditCards.firstOrNull()?.cardType == CardType.CREDIT) PaymentSourceType.CREDIT else PaymentSourceType.DEBIT)
    }
    var selectedCreditCardId by remember { mutableStateOf<String?>(creditCards.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf<String?>(categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id) }
    var selectedAccountId by remember {
        mutableStateOf(creditCards.firstOrNull()?.accountId ?: accounts.firstOrNull { it.type != AccountType.CREDIT_CARD }?.id.orEmpty())
    }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var showOverLimitWarningDialog by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == selectedType }
    val parsedAmount = VietnameseCurrencyFormatter.parseToMinor(amountStr)
    val selectedCard = creditCards.find { it.id == selectedCreditCardId }
    val focusManager = LocalFocusManager.current
    val noteFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Thêm giao dịch mới",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Selector loại giao dịch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        selectedCategoryId = categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                    },
                    label = { Text("Khoản chi") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedCategoryId = categories.firstOrNull { it.type == TransactionType.INCOME }?.id
                    },
                    label = { Text("Khoản thu") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = {
                        selectedType = TransactionType.TRANSFER
                        selectedCategoryId = "cat_transfer"
                    },
                    label = { Text("Chuyển tiền") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            CurrencyAmountTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Số tiền (VND)",
                placeholder = "0",
                allowDecimals = true,
                suffix = "₫",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try { noteFocusRequester.requestFocus() } catch (_: Exception) { focusManager.moveFocus(FocusDirection.Down) }
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(text = "Danh mục", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Spacing.xs))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
                items(filteredCategories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú / Đối tác") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(noteFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            DateTimePickerField(
                timestamp = selectedTimestamp,
                onTimestampChange = { selectedTimestamp = it },
                label = "Thời gian giao dịch"
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            val canSave = parsedAmount > 0L
            Button(
                onClick = {
                    if (canSave) {
                        val finalAccId = selectedAccountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() }
                        onSave(parsedAmount, selectedType, selectedCategoryId, finalAccId, note, paymentSource, selectedCreditCardId, selectedTimestamp)
                        onDismiss()
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Lưu giao dịch", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
