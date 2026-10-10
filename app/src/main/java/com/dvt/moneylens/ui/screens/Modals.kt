package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.calc.CreditCardCycleCalculator
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.CategoryAvatar
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.DateTimePickerField
import com.dvt.moneylens.ui.designsystem.MoneyText
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.Spacing

fun formatManagedSourceChipLabel(card: CreditCard): String {
    return when (card.cardType) {
        CardType.CREDIT -> "${card.nickname} (${card.last4})"
        CardType.DEBIT -> "${card.nickname} (${card.last4})"
        CardType.WALLET -> "${card.nickname} (${if (card.last4.isNotBlank()) card.last4 else "Ví"})"
        CardType.BANK_ACCOUNT -> "${card.nickname} (${card.last4})"
    }
}

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

            // Transaction Type Selector
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

            // Nguồn chi tiêu cho KHOẢN CHI (chỉ chọn từ danh sách đã nhập và quản lý bên Tab Tín dụng)
            if (selectedType == TransactionType.EXPENSE) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Nguồn tiền chi tiêu (Tab Tín dụng):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (creditCards.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(creditCards) { card ->
                            val isSelected = selectedCreditCardId == card.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCreditCardId = card.id
                                    selectedAccountId = card.accountId
                                    paymentSource = if (card.cardType == CardType.CREDIT) PaymentSourceType.CREDIT else PaymentSourceType.DEBIT
                                },
                                label = { Text(formatManagedSourceChipLabel(card)) }
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Chưa có thẻ, ví hoặc tài khoản nào trong Tab Tín dụng. Vui lòng thêm nguồn tiền ở Tab Tín dụng trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(Spacing.sm)
                        )
                    }
                }
            } else if (selectedType == TransactionType.INCOME) {
                // Ví điện tử hoặc Tài khoản nhận tiền cho KHOẢN THU (chọn từ Tab Tín dụng)
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Ví điện tử / Tài khoản nhận tiền (Tab Tín dụng):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val receiveSources = creditCards.filter {
                    it.cardType == CardType.WALLET || it.cardType == CardType.BANK_ACCOUNT || it.cardType == CardType.DEBIT
                }
                if (receiveSources.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(receiveSources) { card ->
                            val isSelected = selectedCreditCardId == card.id || selectedAccountId == card.accountId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCreditCardId = card.id
                                    selectedAccountId = card.accountId
                                    paymentSource = PaymentSourceType.DEBIT
                                },
                                label = { Text(formatManagedSourceChipLabel(card)) }
                            )
                        }
                    }
                } else {
                    val nonCreditAccounts = accounts.filter { it.type != AccountType.CREDIT_CARD }
                    val displayAccounts = if (nonCreditAccounts.isNotEmpty()) nonCreditAccounts else accounts
                    if (displayAccounts.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(displayAccounts) { acc ->
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Chưa có Ví điện tử hoặc Tài khoản nào. Hãy thêm tại tab Tín dụng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Amount Input với định dạng hàng nghìn (.) và thập phân (,) chuẩn Việt Nam
            CurrencyAmountTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Số tiền (VND)",
                placeholder = "0",
                allowDecimals = true,
                suffix = "₫",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try {
                        noteFocusRequester.requestFocus()
                    } catch (e: Exception) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                supportingText = if (parsedAmount > 0L) {
                    {
                        Text(
                            text = "Bằng chữ: ${Money(parsedAmount).formatVnd()}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Category Chips
            Text(
                text = "Danh mục",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredCategories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú / Đối tác") },
                placeholder = { Text("Ăn trưa, cà phê...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(noteFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Date & Time Picker
            DateTimePickerField(
                timestamp = selectedTimestamp,
                onTimestampChange = { selectedTimestamp = it },
                label = "Thời gian giao dịch (Chính xác đến Ngày & Giờ)"
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Save Action
            val isOverLimitOrBalance = if (selectedType == TransactionType.EXPENSE && selectedCard != null && parsedAmount > 0L) {
                if (selectedCard.cardType == CardType.CREDIT) {
                    val remainingLimit = selectedCard.creditLimitMinor - selectedCard.currentBalanceOwedMinor
                    parsedAmount > remainingLimit
                } else {
                    val availBalance = accounts.find { it.id == selectedCard.accountId }?.balanceMinor ?: 0L
                    parsedAmount > availBalance
                }
            } else false

            val isCreditInvalid = selectedType == TransactionType.EXPENSE && paymentSource == PaymentSourceType.CREDIT && (creditCards.isEmpty() || selectedCreditCardId == null)
            val canSave = parsedAmount > 0L && !isCreditInvalid

            val executeSave = {
                val finalAccId = if (selectedType == TransactionType.EXPENSE && paymentSource == PaymentSourceType.CREDIT) {
                    creditCards.find { it.id == selectedCreditCardId }?.accountId ?: selectedAccountId
                } else if (selectedType == TransactionType.INCOME) {
                    selectedAccountId.ifBlank { creditCards.find { it.id == selectedCreditCardId }?.accountId ?: accounts.firstOrNull()?.id.orEmpty() }
                } else {
                    selectedAccountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() }
                }
                onSave(parsedAmount, selectedType, selectedCategoryId, finalAccId, note, paymentSource, selectedCreditCardId, selectedTimestamp)
                onDismiss()
            }

            Button(
                onClick = {
                    if (canSave) {
                        if (isOverLimitOrBalance) {
                            showOverLimitWarningDialog = true
                        } else {
                            executeSave()
                        }
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Lưu giao dịch", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }

    if (showOverLimitWarningDialog && selectedCard != null) {
        val isCredit = selectedCard.cardType == CardType.CREDIT
        val avail = if (isCredit) {
            (selectedCard.creditLimitMinor - selectedCard.currentBalanceOwedMinor).coerceAtLeast(0L)
        } else {
            accounts.find { it.id == selectedCard.accountId }?.balanceMinor ?: 0L
        }
        val typeName = when (selectedCard.cardType) {
            CardType.CREDIT -> "thẻ tín dụng"
            CardType.DEBIT -> "thẻ Debit"
            CardType.WALLET -> "ví điện tử"
            CardType.BANK_ACCOUNT -> "tài khoản ngân hàng"
            else -> "nguồn tiền"
        }
        val limitOrBalanceName = if (isCredit) "hạn mức khả dụng còn lại" else "số dư khả dụng hiện tại"

        AlertDialog(
            onDismissRequest = { showOverLimitWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Cảnh báo vượt $limitOrBalanceName")
            },
            text = {
                Text(
                    "Số tiền chi tiêu (${Money(parsedAmount).formatVnd()}) lớn hơn $limitOrBalanceName của $typeName ${formatManagedSourceChipLabel(selectedCard)} (${Money(avail).formatVnd()}).\n\nBạn có muốn tiếp tục lưu giao dịch này không?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverLimitWarningDialog = false
                        val finalAccId = if (selectedType == TransactionType.EXPENSE && paymentSource == PaymentSourceType.CREDIT) {
                            creditCards.find { it.id == selectedCreditCardId }?.accountId ?: selectedAccountId
                        } else if (selectedType == TransactionType.INCOME) {
                            selectedAccountId.ifBlank { creditCards.find { it.id == selectedCreditCardId }?.accountId ?: accounts.firstOrNull()?.id.orEmpty() }
                        } else {
                            selectedAccountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() }
                        }
                        onSave(parsedAmount, selectedType, selectedCategoryId, finalAccId, note, paymentSource, selectedCreditCardId, selectedTimestamp)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Vẫn tiếp tục lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverLimitWarningDialog = false }) {
                    Text("Kiểm tra lại")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmCategorySheet(
    transaction: Transaction,
    allCategories: List<Category>,
    creditCards: List<CreditCard> = emptyList(),
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onUpdate: ((Transaction) -> Unit)? = null,
    onConvertToInstallment: ((title: String, counterparty: String, amount: Long, term: Int, feeMinor: Long, feeSchedule: ConversionFeeSchedule, feePct: Double) -> Unit)? = null
) {
    val creditOnlyCards = remember(creditCards) { creditCards.filter { it.isCredit } }
    val detectedCreditCard = remember(transaction, creditOnlyCards) {
        val text = "${transaction.note} ${transaction.counterparty.orEmpty()}".uppercase()
        // 1. Check last 4 digits
        creditOnlyCards.find { it.last4.isNotBlank() && text.contains(it.last4) }
            // 2. Check nickname
            ?: creditOnlyCards.find { it.nickname.isNotBlank() && text.contains(it.nickname.uppercase()) }
            // 3. Check issuer bank code (e.g. TCB, VCB, MB, ACB, VPB)
            ?: creditOnlyCards.find { it.issuerBank.isNotBlank() && text.contains(it.issuerBank.uppercase()) }
    }
    var selectedPayingCreditCardId by remember {
        mutableStateOf(
            if (transaction.categoryId == "cat_credit_pay" && transaction.creditCardId != null) {
                transaction.creditCardId
            } else {
                detectedCreditCard?.id ?: creditOnlyCards.firstOrNull()?.id
            }
        )
    }

    val isProbableCreditPayment = remember(transaction) {
        val t = "${transaction.note} ${transaction.counterparty.orEmpty()}".uppercase()
        t.contains("THANH TOAN THE") || t.contains("TRA NO THE") || t.contains("THANH TOAN SAO KE") ||
        t.contains("CREDIT CARD") || t.contains("THANH TOAN DU NO") || t.contains("PAYMENT THE")
    }
    val isProbableTransfer = remember(transaction) {
        val t = "${transaction.note} ${transaction.counterparty.orEmpty()}".uppercase()
        (t.contains("CHUYEN TIEN") || t.contains("CHUYEN KHOAN") || t.contains("IBFT") || t.contains("CHUYEN DI") || t.contains("NHAN TIEN") || t.contains("NAP TIEN")) && !isProbableCreditPayment
    }

    var selectedCatId by remember {
        mutableStateOf(
            transaction.categoryId ?: when {
                isProbableCreditPayment && allCategories.any { it.id == "cat_credit_pay" } -> "cat_credit_pay"
                isProbableTransfer && transaction.type == TransactionType.INCOME && allCategories.any { it.id == "cat_transfer_in" } -> "cat_transfer_in"
                isProbableTransfer && transaction.type == TransactionType.EXPENSE && allCategories.any { it.id == "cat_transfer_out" } -> "cat_transfer_out"
                isProbableTransfer && allCategories.any { it.id == "cat_transfer_out" } -> "cat_transfer_out"
                else -> allCategories.firstOrNull { it.type == transaction.type }?.id.orEmpty()
            }
        )
    }
    val initialCard = transaction.creditCardId?.let { id -> creditCards.find { it.id == id } }
        ?: creditCards.find { it.accountId == transaction.accountId }
    var currentCreditCardId by remember {
        mutableStateOf(initialCard?.id ?: creditCards.firstOrNull()?.id)
    }
    var currentPaymentSource by remember {
        mutableStateOf(
            if (initialCard != null) {
                if (initialCard.cardType == CardType.CREDIT) PaymentSourceType.CREDIT else PaymentSourceType.DEBIT
            } else {
                transaction.paymentSource
            }
        )
    }
    var currentAccountId by remember {
        mutableStateOf(transaction.accountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() })
    }
    var currentNote by remember {
        mutableStateOf(transaction.note)
    }
    var currentOccurredAt by remember {
        mutableStateOf(transaction.occurredAt)
    }
    var showConfirmOverLimitWarningDialog by remember { mutableStateOf(false) }

    // Installment Conversion Sub-Dialog State
    var showInstallmentConfig by remember { mutableStateOf(false) }
    var installmentTermStr by remember { mutableStateOf("12") }
    var installmentFeeEnabled by remember { mutableStateOf(false) }
    var feeModeIsPercent by remember { mutableStateOf(false) }
    var installmentFeeAmountStr by remember { mutableStateOf("") }
    var installmentFeePercentStr by remember { mutableStateOf("3.0") }
    var installmentFeeSchedule by remember { mutableStateOf(ConversionFeeSchedule.FIRST_MONTH) }

    val parsedTerm = installmentTermStr.toIntOrNull()?.coerceAtLeast(1) ?: 12
    val parsedFeeAmount = if (feeModeIsPercent) {
        val pct = installmentFeePercentStr.toDoubleOrNull() ?: 0.0
        if (transaction.amountMinor > 0 && pct > 0) ((transaction.amountMinor * pct) / 100.0).toLong() else 0L
    } else {
        VietnameseCurrencyFormatter.parseToMinor(installmentFeeAmountStr)
    }
    val calculatedFeePct = if (transaction.amountMinor > 0 && parsedFeeAmount > 0) {
        (parsedFeeAmount.toDouble() / transaction.amountMinor) * 100.0
    } else 0.0
    val finalFeeMinor = if (installmentFeeEnabled) parsedFeeAmount else 0L

    val firstMonthMonthly = if (installmentFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) {
        (transaction.amountMinor / parsedTerm) + finalFeeMinor
    } else {
        (transaction.amountMinor / parsedTerm) + (finalFeeMinor / parsedTerm)
    }
    val regularMonthly = if (installmentFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY) {
        (transaction.amountMinor / parsedTerm) + (finalFeeMinor / parsedTerm)
    } else {
        (transaction.amountMinor / parsedTerm)
    }

    val currentCard = creditCards.find { it.id == currentCreditCardId }

    val finalAccId = if (selectedCatId == "cat_credit_pay") {
        currentAccountId.ifBlank { transaction.accountId }
    } else if (transaction.type == TransactionType.EXPENSE && currentPaymentSource == PaymentSourceType.CREDIT) {
        creditCards.find { it.id == currentCreditCardId }?.accountId ?: currentAccountId
    } else if (transaction.type == TransactionType.INCOME) {
        currentAccountId.ifBlank { creditCards.find { it.id == currentCreditCardId }?.accountId ?: transaction.accountId }
    } else {
        currentAccountId.ifBlank { transaction.accountId }
    }

    val isOverLimitOrBalance = if (transaction.type == TransactionType.EXPENSE && transaction.amountMinor > 0L) {
        if (selectedCatId == "cat_credit_pay") {
            val availBalance = accounts.find { it.id == finalAccId }?.balanceMinor ?: 0L
            transaction.amountMinor > availBalance
        } else if (currentCard != null) {
            if (currentCard.cardType == CardType.CREDIT) {
                val remainingLimit = currentCard.creditLimitMinor - currentCard.currentBalanceOwedMinor
                transaction.amountMinor > remainingLimit
            } else {
                val availBalance = accounts.find { it.id == currentCard.accountId }?.balanceMinor ?: 0L
                transaction.amountMinor > availBalance
            }
        } else false
    } else false

    val executeConfirmOrUpdate = {
        val effectivePaymentSource = if (selectedCatId == "cat_credit_pay") {
            PaymentSourceType.DEBIT
        } else if (transaction.type == TransactionType.INCOME) {
            PaymentSourceType.DEBIT
        } else {
            currentPaymentSource
        }

        val effectiveCreditCardId = if (selectedCatId == "cat_credit_pay") {
            selectedPayingCreditCardId
        } else {
            currentCreditCardId
        }

        val updatedTx = transaction.copy(
            accountId = finalAccId,
            categoryId = selectedCatId,
            paymentSource = effectivePaymentSource,
            creditCardId = effectiveCreditCardId,
            note = currentNote,
            occurredAt = currentOccurredAt
        )

        if (transaction.status == TransactionStatus.PENDING) {
            onConfirm(updatedTx.copy(status = TransactionStatus.CONFIRMED))
        } else {
            onUpdate?.invoke(updatedTx)
        }
        onDismiss()
    }
    val focusManager = LocalFocusManager.current

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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = transaction.counterparty ?: transaction.note.ifBlank { "Chi tiết giao dịch" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Nguồn: ${transaction.source} · Trạng thái: ${if (transaction.status == TransactionStatus.PENDING) "Chờ xác nhận" else "Đã duyệt"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MoneyText(
                    amountMinor = transaction.amountMinor,
                    type = transaction.type,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Receiving Account Picker for INCOME Transactions
            if (transaction.type == TransactionType.INCOME) {
                Text(
                    text = "Ví điện tử / Tài khoản nhận tiền (Tab Tín dụng):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val receiveSources = creditCards.filter {
                    it.cardType == CardType.WALLET || it.cardType == CardType.BANK_ACCOUNT || it.cardType == CardType.DEBIT
                }
                if (receiveSources.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(receiveSources) { card ->
                            val isSelected = currentCreditCardId == card.id || currentAccountId == card.accountId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    currentCreditCardId = card.id
                                    currentAccountId = card.accountId
                                },
                                label = { Text(formatManagedSourceChipLabel(card)) }
                            )
                        }
                    }
                } else {
                    val nonCreditAccounts = accounts.filter { it.type != AccountType.CREDIT_CARD }
                    val displayAccounts = if (nonCreditAccounts.isNotEmpty()) nonCreditAccounts else accounts
                    if (displayAccounts.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(displayAccounts) { acc ->
                                FilterChip(
                                    selected = currentAccountId == acc.id,
                                    onClick = { currentAccountId = acc.id },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Chưa có Ví điện tử hoặc Tài khoản nào. Hãy thêm tại tab Tín dụng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Nguồn chi tiêu cho KHOẢN CHI (chỉ chọn từ danh sách đã nhập và quản lý bên Tab Tín dụng)
            if (transaction.type == TransactionType.EXPENSE) {
                Text(
                    text = if (selectedCatId == "cat_credit_pay") "Tài khoản nguồn trích tiền thanh toán thẻ (Tab Tín dụng):" else "Nguồn tiền chi tiêu (Tab Tín dụng):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (creditCards.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(creditCards) { card ->
                            val isSelected = currentCreditCardId == card.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    currentCreditCardId = card.id
                                    currentAccountId = card.accountId
                                    currentPaymentSource = if (card.cardType == CardType.CREDIT) PaymentSourceType.CREDIT else PaymentSourceType.DEBIT
                                },
                                label = { Text(formatManagedSourceChipLabel(card)) }
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Chưa có nguồn tiền nào được lưu trong Tab Tín dụng. Vui lòng thêm thẻ, ví hoặc tài khoản trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(Spacing.sm)
                        )
                    }
                }
            }

                // INSTALLMENT CONVERSION OPTION FOR CREDIT TRANSACTIONS (Chỉ áp dụng khi chi tiêu thẻ, không áp dụng cho thanh toán trả nợ thẻ)
                if ((currentPaymentSource == PaymentSourceType.CREDIT || transaction.isCredit) && selectedCatId != "cat_credit_pay") {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(
                                        text = "Chuyển đổi sang Trả góp 0%",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = { showInstallmentConfig = !showInstallmentConfig }) {
                                    Text(if (showInstallmentConfig) "Thu gọn" else "Cấu hình")
                                }
                            }

                            if (showInstallmentConfig) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Term Selector
                                Text("Kỳ hạn trả góp:", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    listOf("3", "6", "12", "24").forEach { term ->
                                        FilterChip(
                                            selected = installmentTermStr == term,
                                            onClick = { installmentTermStr = term },
                                            label = { Text("$term thg") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Fee Toggle & Inputs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Phí chuyển đổi trả góp", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = installmentFeeEnabled,
                                        onCheckedChange = { installmentFeeEnabled = it }
                                    )
                                }

                                if (installmentFeeEnabled) {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                    ) {
                                        FilterChip(
                                            selected = !feeModeIsPercent,
                                            onClick = { feeModeIsPercent = false },
                                            label = { Text("VNĐ") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = feeModeIsPercent,
                                            onClick = { feeModeIsPercent = true },
                                            label = { Text("Tỷ lệ %") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    if (feeModeIsPercent) {
                                        OutlinedTextField(
                                            value = installmentFeePercentStr,
                                            onValueChange = { installmentFeePercentStr = it },
                                            label = { Text("Tỷ lệ phí (%)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                            supportingText = if (finalFeeMinor > 0) {
                                                { Text("Tương đương: ${Money(finalFeeMinor).formatVnd()}") }
                                            } else null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        CurrencyAmountTextField(
                                            value = installmentFeeAmountStr,
                                            onValueChange = { installmentFeeAmountStr = it },
                                            label = "Số tiền phí (VNĐ)",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                            supportingText = if (finalFeeMinor > 0) {
                                                { Text("Bằng chữ: ${Money(finalFeeMinor).formatVnd()}") }
                                            } else null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                    ) {
                                        FilterChip(
                                            selected = installmentFeeSchedule == ConversionFeeSchedule.FIRST_MONTH,
                                            onClick = { installmentFeeSchedule = ConversionFeeSchedule.FIRST_MONTH },
                                            label = { Text("Thu kỳ đầu", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = installmentFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY,
                                            onClick = { installmentFeeSchedule = ConversionFeeSchedule.SPREAD_MONTHLY },
                                            label = { Text("Chia đều", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Live Calculation Preview Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(Spacing.sm)) {
                                        Text(
                                            text = "• Kỳ 1: ${Money(firstMonthMonthly).formatVnd()} · Kỳ 2-$parsedTerm: ${Money(regularMonthly).formatVnd()}/tháng",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "• Giảm dư nợ thẻ: ${Money(transaction.amountMinor).formatVnd()} (chuyển sang nợ trả góp)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.md))

                                Button(
                                    onClick = {
                                        val counterparty = creditCards.find { it.id == currentCreditCardId }?.let { "${it.issuerBank} (${it.nickname})" } ?: transaction.counterparty ?: "Thẻ tín dụng"
                                        val title = "Trả góp ${transaction.note.ifBlank { "khoản chi tiêu" }}"
                                        onConvertToInstallment?.invoke(
                                            title,
                                            counterparty,
                                            transaction.amountMinor,
                                            parsedTerm,
                                            finalFeeMinor,
                                            installmentFeeSchedule,
                                            calculatedFeePct
                                        )
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Xác nhận chuyển sang trả góp")
                                }
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Date & Time Picker for editing old/manual transactions
            DateTimePickerField(
                timestamp = currentOccurredAt,
                onTimestampChange = { currentOccurredAt = it },
                label = "Thời gian giao dịch (Điều chỉnh ngày & giờ)"
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = currentNote,
                onValueChange = { currentNote = it },
                label = { Text("Ghi chú") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Danh mục phân loại:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            val baseCats = allCategories.filter { it.type == transaction.type }
            val availableCats = if (transaction.type == TransactionType.EXPENSE) {
                val list = baseCats.toMutableList()
                if (list.none { it.id == "cat_transfer_out" }) {
                    list.add(Category("cat_transfer_out", "Chuyển tiền", "swap_horiz", "#5B6B8C", TransactionType.EXPENSE))
                }
                if (list.none { it.id == "cat_credit_pay" }) {
                    list.add(Category("cat_credit_pay", "Thanh toán Credit", "credit_card", "#3949AB", TransactionType.EXPENSE))
                }
                list
            } else if (transaction.type == TransactionType.INCOME) {
                val list = baseCats.toMutableList()
                if (list.none { it.id == "cat_transfer_in" }) {
                    list.add(Category("cat_transfer_in", "Chuyển tiền", "swap_horiz", "#5B6B8C", TransactionType.INCOME))
                }
                list
            } else {
                baseCats
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                availableCats.forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCatId == cat.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClick = { selectedCatId = cat.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryAvatar(category = cat)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (cat.id == "cat_credit_pay") {
                                    Text(
                                        text = "Thanh toán nợ thẻ tín dụng · Giảm dư nợ",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (cat.id == "cat_transfer_out") {
                                    Text(
                                        text = "Chuyển tiền / Chuyển khoản ngoài",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (cat.id == "cat_transfer_in") {
                                    Text(
                                        text = "Nhận tiền chuyển khoản / Chuyển đến",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (selectedCatId == cat.id) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Nếu chọn "Thanh toán Credit", hiển thị ngay khu vực chọn thẻ tín dụng được thanh toán
                    if (cat.id == "cat_credit_pay" && selectedCatId == "cat_credit_pay") {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chọn thẻ tín dụng được thanh toán:",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (creditOnlyCards.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        creditOnlyCards.forEach { card ->
                                            val isCardSelected = selectedPayingCreditCardId == card.id
                                            val isSuggested = detectedCreditCard?.id == card.id
                                            val newBalanceOwed = (card.currentBalanceOwedMinor - transaction.amountMinor).coerceAtLeast(0L)

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isCardSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                border = if (isCardSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                onClick = { selectedPayingCreditCardId = card.id },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.weight(1f),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RadioButton(
                                                                selected = isCardSelected,
                                                                onClick = { selectedPayingCreditCardId = card.id },
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "${card.issuerBank} (${card.nickname}) ••••${card.last4}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                        }
                                                        if (isSuggested) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    text = "Gợi ý từ nội dung",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Dư nợ hiện tại: ${Money(card.currentBalanceOwedMinor).formatVnd()} ➔ Sau thanh toán: ${Money(newBalanceOwed).formatVnd()}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                        modifier = Modifier.padding(start = 28.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "ⓘ Khoản này sẽ giảm trừ dư nợ thẻ ${Money(transaction.amountMinor).formatVnd()} và không tính trùng vào chi tiêu sinh hoạt tháng.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Chưa có thẻ tín dụng nào trong Tab Tín dụng. Vui lòng thêm thẻ tín dụng để ghi nhận giảm nợ.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = {
                        onDelete(transaction)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Xóa")
                }

                Button(
                    onClick = {
                        if (isOverLimitOrBalance) {
                            showConfirmOverLimitWarningDialog = true
                        } else {
                            executeConfirmOrUpdate()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(if (transaction.status == TransactionStatus.PENDING) "Xác nhận" else "Lưu thay đổi")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }

    if (showConfirmOverLimitWarningDialog && currentCard != null) {
        val isCredit = currentCard.cardType == CardType.CREDIT
        val avail = if (isCredit) {
            (currentCard.creditLimitMinor - currentCard.currentBalanceOwedMinor).coerceAtLeast(0L)
        } else {
            accounts.find { it.id == currentCard.accountId }?.balanceMinor ?: 0L
        }
        val typeName = when (currentCard.cardType) {
            CardType.CREDIT -> "thẻ tín dụng"
            CardType.DEBIT -> "thẻ Debit"
            CardType.WALLET -> "ví điện tử"
            CardType.BANK_ACCOUNT -> "tài khoản ngân hàng"
            else -> "nguồn tiền"
        }
        val limitOrBalanceName = if (isCredit) "hạn mức khả dụng còn lại" else "số dư khả dụng hiện tại"

        AlertDialog(
            onDismissRequest = { showConfirmOverLimitWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Cảnh báo vượt $limitOrBalanceName")
            },
            text = {
                Text(
                    "Số tiền chi tiêu (${Money(transaction.amountMinor).formatVnd()}) lớn hơn $limitOrBalanceName của $typeName ${formatManagedSourceChipLabel(currentCard)} (${Money(avail).formatVnd()}).\n\nBạn có muốn tiếp tục ghi nhận giao dịch này không?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmOverLimitWarningDialog = false
                        executeConfirmOrUpdate()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Vẫn tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmOverLimitWarningDialog = false }) {
                    Text("Kiểm tra lại")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreditCardSheet(
    card: CreditCard,
    onDismiss: () -> Unit,
    onSave: (CreditCard) -> Unit
) {
    var nickname by remember { mutableStateOf(card.nickname) }
    var limitStr by remember { mutableStateOf(VietnameseCurrencyFormatter.formatMinor(card.creditLimitMinor)) }
    var stmtDayStr by remember { mutableStateOf(card.statementDay.toString()) }
    var dueDaysStr by remember { mutableStateOf(card.dueDaysAfterStatement.toString()) }
    var selectedColor by remember { mutableStateOf(card.colorHex) }

    val colors = listOf("#0A6B5C", "#E53935", "#1E88E5", "#43A047", "#8E24AA", "#F57C00")
    val focusManager = LocalFocusManager.current

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
                .verticalScroll(rememberScrollState())
        ) {
            val isCredit = card.cardType == CardType.CREDIT
            Text(
                text = when (card.cardType) {
                    CardType.CREDIT -> "Chỉnh sửa thẻ tín dụng"
                    CardType.DEBIT -> "Chỉnh sửa thẻ Debit"
                    CardType.WALLET -> "Chỉnh sửa Ví điện tử"
                    CardType.BANK_ACCOUNT -> "Chỉnh sửa Tài khoản ngân hàng"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = when (card.cardType) {
                    CardType.CREDIT -> "${card.issuerBank} · ${card.network.label} ••••${card.last4}"
                    CardType.DEBIT -> "${card.issuerBank} · ${card.network.label} ••••${card.last4}"
                    CardType.WALLET -> "${card.issuerBank} ${if (card.last4.isNotBlank()) "••••${card.last4}" else ""}"
                    CardType.BANK_ACCOUNT -> "${card.issuerBank} · STK: ${card.last4}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(if (card.cardType == CardType.WALLET) "Tên gợi nhớ ví" else "Tên gợi nhớ thẻ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = if (isCredit) ImeAction.Next else ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (isCredit) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                CurrencyAmountTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = "Hạn mức tín dụng (VNĐ)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    supportingText = VietnameseCurrencyFormatter.parseToMinor(limitStr).let {
                        if (it > 0) { { Text("Bằng chữ: ${Money(it).formatVnd()}") } } else null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = stmtDayStr,
                        onValueChange = { stmtDayStr = it },
                        label = { Text("Ngày sao kê (1-31)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dueDaysStr,
                        onValueChange = { dueDaysStr = it },
                        label = { Text("Hạn thanh toán (ngày)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text("Màu sắc nhận diện:", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                colors.forEach { hex ->
                    val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = hex }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == hex) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            val parsedLimit = VietnameseCurrencyFormatter.parseToMinor(limitStr)
            val parsedStmtDay = stmtDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 15
            val parsedDueDays = dueDaysStr.toIntOrNull()?.coerceIn(1, 45) ?: 20
            val isValid = nickname.isNotBlank() && (!isCredit || parsedLimit > 0)

            Button(
                onClick = {
                    if (isValid) {
                        val updated = card.copy(
                            nickname = nickname,
                            creditLimitMinor = if (isCredit) parsedLimit else 0L,
                            statementDay = if (isCredit) parsedStmtDay else 0,
                            dueDaysAfterStatement = if (isCredit) parsedDueDays else 0,
                            colorHex = selectedColor
                        )
                        onSave(updated)
                        onDismiss()
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isCredit) "Cập nhật thông tin thẻ" else "Lưu thông tin", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayCreditCardSheet(
    card: CreditCard,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmPayment: (fromAccountId: String, amountMinor: Long) -> Unit,
    onDeleteCard: ((String) -> Unit)? = null
) {
    val cycle = CreditCardCycleCalculator.calculateCycle(card)
    val bankAccounts = accounts.filter { it.type == AccountType.BANK }
    var selectedFromAccountId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: accounts.firstOrNull()?.id.orEmpty()) }
    var paymentAmountMinor by remember { mutableStateOf(cycle.minPaymentRequired.minor) }
    var customAmountStr by remember { mutableStateOf(VietnameseCurrencyFormatter.formatMinor(paymentAmountMinor)) }
    val focusManager = LocalFocusManager.current

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
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Thanh toán thẻ ${card.nickname}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = "${card.issuerBank} ••••${card.last4} · Dư nợ: ${card.balanceOwed.formatVnd()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text("Gợi ý số tiền:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                FilterChip(
                    selected = paymentAmountMinor == cycle.minPaymentRequired.minor,
                    onClick = {
                        paymentAmountMinor = cycle.minPaymentRequired.minor
                        customAmountStr = VietnameseCurrencyFormatter.formatMinor(paymentAmountMinor)
                    },
                    label = { Text("Tối thiểu: ${cycle.minPaymentRequired.formatVnd()}") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = paymentAmountMinor == card.currentBalanceOwedMinor,
                    onClick = {
                        paymentAmountMinor = card.currentBalanceOwedMinor
                        customAmountStr = VietnameseCurrencyFormatter.formatMinor(paymentAmountMinor)
                    },
                    label = { Text("Toàn bộ dư nợ") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            CurrencyAmountTextField(
                value = customAmountStr,
                onValueChange = {
                    customAmountStr = it
                    paymentAmountMinor = VietnameseCurrencyFormatter.parseToMinor(it)
                },
                label = "Số tiền thanh toán (VNĐ)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                supportingText = if (paymentAmountMinor > 0) {
                    { Text("Bằng chữ: ${Money(paymentAmountMinor).formatVnd()}") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text("Trích từ tài khoản:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Spacing.xs))
            bankAccounts.forEach { acc ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedFromAccountId == acc.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = { selectedFromAccountId = acc.id },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(acc.name, style = MaterialTheme.typography.titleSmall)
                        Text(acc.balance.formatVnd(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ⓘ Thanh toán thẻ được ghi là chuyển nội bộ (TRANSFER), không tính vào chi tiêu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Button(
                onClick = {
                    if (paymentAmountMinor > 0 && selectedFromAccountId.isNotBlank()) {
                        onConfirmPayment(selectedFromAccountId, paymentAmountMinor)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Xác nhận thanh toán ${Money(paymentAmountMinor).formatVnd()}")
            }

            if (onDeleteCard != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                var showDeleteConfirm by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Xóa thẻ tín dụng này")
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Xác nhận xóa thẻ") },
                        text = { Text("Bạn có chắc chắn muốn xóa thẻ tín dụng ${card.nickname} (${card.issuerBank} ••••${card.last4}) không? Tài khoản liên kết và hạn mức sẽ bị xóa.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDeleteCard(card.id)
                                    onDismiss()
                                }
                            ) {
                                Text("Xóa thẻ", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Hủy")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentBottomSheet(
    existingAssets: List<Asset>,
    initialAsset: Asset? = null,
    initialAction: InvestmentAction = InvestmentAction.BUY,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (
        action: InvestmentAction,
        assetId: String?,
        symbol: String,
        name: String,
        type: AssetType,
        quantity: Double,
        priceMinor: Long,
        accountId: String?,
        note: String,
        occurredAt: Long
    ) -> Unit
) {
    var selectedAction by remember(initialAction) { mutableStateOf(initialAction) }
    var selectedAssetId by remember { mutableStateOf<String?>(initialAsset?.id) }
    val currentSelectedAsset = existingAssets.find { it.id == selectedAssetId }

    var assetType by remember { mutableStateOf(initialAsset?.type ?: AssetType.STOCK) }
    var symbol by remember { mutableStateOf(initialAsset?.symbol.orEmpty()) }
    var name by remember { mutableStateOf(initialAsset?.name.orEmpty()) }
    var quantityStr by remember {
        mutableStateOf(
            if (initialAsset != null) {
                if (initialAsset.quantity % 1.0 == 0.0) initialAsset.quantity.toLong().toString()
                else initialAsset.quantity.toString()
            } else ""
        )
    }
    var priceStr by remember { mutableStateOf(if (initialAsset != null) VietnameseCurrencyFormatter.formatMinor(initialAsset.currentPriceMinor) else "") }
    var selectedAccountId by remember { mutableStateOf<String?>(accounts.firstOrNull()?.id) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf(initialAsset?.notes.orEmpty()) }

    val parsedQty = VietnameseCurrencyFormatter.parseToDouble(quantityStr)
    val parsedPrice = VietnameseCurrencyFormatter.parseToMinor(priceStr)
    val totalCost = (parsedQty * parsedPrice).toLong()

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val nameFocusRequester = remember { FocusRequester() }
    val quantityFocusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }
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
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giao dịch đầu tư & Tích sản",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {

            // Action Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = selectedAction == InvestmentAction.BUY,
                    onClick = { selectedAction = InvestmentAction.BUY },
                    label = { Text("Mua thêm", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedAction == InvestmentAction.SELL,
                    onClick = { selectedAction = InvestmentAction.SELL },
                    label = { Text("Bán bớt", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedAction == InvestmentAction.DIVIDEND,
                    onClick = { selectedAction = InvestmentAction.DIVIDEND },
                    label = { Text("Cổ tức", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedAction == InvestmentAction.VALUATION,
                    onClick = { selectedAction = InvestmentAction.VALUATION },
                    label = { Text("Định giá", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Existing Assets Chips or New
            if (existingAssets.isNotEmpty()) {
                Text("Chọn tài sản:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Spacing.xs))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedAction == InvestmentAction.BUY) {
                        item {
                            FilterChip(
                                selected = selectedAssetId == null,
                                onClick = {
                                    selectedAssetId = null
                                    symbol = ""
                                    name = ""
                                    priceStr = ""
                                },
                                label = { Text("+ Tài sản mới") }
                            )
                        }
                    }
                    items(existingAssets) { ast ->
                        FilterChip(
                            selected = selectedAssetId == ast.id,
                            onClick = {
                                selectedAssetId = ast.id
                                assetType = ast.type
                                symbol = ast.symbol
                                name = ast.name
                                priceStr = VietnameseCurrencyFormatter.formatMinor(ast.currentPriceMinor)
                                if (note.isBlank() && !ast.notes.isNullOrBlank()) {
                                    note = ast.notes
                                }
                            },
                            label = { Text(ast.symbol) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // If creating new asset or no asset selected
            if (selectedAssetId == null) {
                Text("Loại tài sản:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Spacing.xs))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        FilterChip(
                            selected = assetType == AssetType.REAL_ESTATE,
                            onClick = { assetType = AssetType.REAL_ESTATE },
                            label = { Text("Bất động sản (BĐS)") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = assetType == AssetType.STOCK,
                            onClick = { assetType = AssetType.STOCK },
                            label = { Text("Cổ phiếu") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = assetType == AssetType.GOLD,
                            onClick = { assetType = AssetType.GOLD },
                            label = { Text("Vàng") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = assetType == AssetType.CRYPTO,
                            onClick = { assetType = AssetType.CRYPTO },
                            label = { Text("Crypto") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = assetType == AssetType.FUND,
                            onClick = { assetType = AssetType.FUND },
                            label = { Text("Quỹ") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Mã tài sản") },
                        placeholder = { Text("VNM, SJC") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                nameFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên tài sản") },
                        placeholder = { Text("Vinamilk") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                if (selectedAction != InvestmentAction.DIVIDEND) {
                                    quantityFocusRequester.requestFocus()
                                } else {
                                    priceFocusRequester.requestFocus()
                                }
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }),
                        modifier = Modifier
                            .weight(1.5f)
                            .focusRequester(nameFocusRequester)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Quantity & Price Inputs
            if (selectedAction != InvestmentAction.DIVIDEND) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text(if (selectedAction == InvestmentAction.VALUATION) "Số lượng hiện có" else "Số lượng") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                priceFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(quantityFocusRequester)
                    )

                    CurrencyAmountTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = if (selectedAction == InvestmentAction.VALUATION) "Giá mới (VND)" else "Đơn giá (VND)",
                        allowDecimals = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                noteFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }),
                        modifier = Modifier
                            .weight(1.5f)
                            .focusRequester(priceFocusRequester)
                    )
                }
            } else {
                CurrencyAmountTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = "Số tiền cổ tức (VND)",
                    allowDecimals = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        try {
                            noteFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }),
                    supportingText = if (parsedPrice > 0L) {
                        { Text("Bằng chữ: ${Money(parsedPrice).formatVnd()}") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(priceFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Account selection (for cash flow)
            if (selectedAction != InvestmentAction.VALUATION) {
                Text(
                    text = if (selectedAction == InvestmentAction.BUY) "Trừ tiền từ tài khoản:" else "Cộng tiền vào tài khoản:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts.filter { it.type != AccountType.CREDIT_CARD }) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text("Không liên kết ví") }
                        )
                    }
                }
                if (selectedAction == InvestmentAction.BUY) {
                    Text(
                        text = if (selectedAccountId != null) "💡 Tiền mua tài sản sẽ trích từ tài khoản để khớp số dư ngân hàng thực tế, danh mục đầu tư tăng tương ứng (Tài sản ròng bảo toàn nguyên vẹn)"
                               else "💡 Không liên kết ví: Chỉ ghi nhận vào danh mục tài sản, số dư tài khoản tiền mặt không đổi",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú đầu tư") },
                placeholder = { Text("Ghi chú đầu tư...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(noteFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Date & Time Picker
            DateTimePickerField(
                timestamp = selectedTimestamp,
                onTimestampChange = { selectedTimestamp = it },
                label = "Thời gian thực hiện (Chính xác đến Ngày & Giờ)"
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Calculation Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    when (selectedAction) {
                        InvestmentAction.BUY -> {
                            Text(
                                text = "Tổng tiền thanh toán: ${Money(totalCost).formatVnd()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (currentSelectedAsset != null && parsedQty > 0 && parsedPrice > 0) {
                                val projectedCostBasis = com.dvt.moneylens.domain.calc.CostBasisCalculator.calculateBuyWeightedAverage(
                                    currentQuantity = currentSelectedAsset.quantity,
                                    currentCostBasisMinor = currentSelectedAsset.costBasisMinor,
                                    buyQuantity = parsedQty,
                                    buyPriceMinor = parsedPrice
                                )
                                Text(
                                    text = "Giá vốn bình quân mới: ${Money(projectedCostBasis).formatVnd()} / đv (Cũ: ${Money(currentSelectedAsset.costBasisMinor).formatVnd()})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        InvestmentAction.SELL -> {
                            val realizedPnL = if (currentSelectedAsset != null) {
                                com.dvt.moneylens.domain.calc.CostBasisCalculator.calculateSellRealizedPnL(
                                    currentCostBasisMinor = currentSelectedAsset.costBasisMinor,
                                    sellQuantity = parsedQty,
                                    sellPriceMinor = parsedPrice
                                )
                            } else 0L

                            Text(
                                text = "Tổng tiền thu về: ${Money(totalCost).formatVnd()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Lãi/Lỗ dự tính: ${if (realizedPnL >= 0) "+" else ""}${Money(realizedPnL).formatVnd()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (realizedPnL >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        InvestmentAction.DIVIDEND -> {
                            Text(
                                text = "Cổ tức nhận vào: ${Money(parsedPrice).formatVnd()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        InvestmentAction.VALUATION -> {
                            Text(
                                text = "Cập nhật định giá thị trường mới: ${Money(parsedPrice).formatVnd()} / đv",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Pinned Bottom Action Bar
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                ) {
                    val isFormValid = symbol.isNotBlank() && (parsedPrice > 0 || parsedQty > 0)
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onSave(
                                    selectedAction,
                                    selectedAssetId,
                                    symbol,
                                    name.ifBlank { symbol },
                                    assetType,
                                    if (selectedAction == InvestmentAction.DIVIDEND) 1.0 else parsedQty,
                                    parsedPrice,
                                    selectedAccountId,
                                    note,
                                    selectedTimestamp
                                )
                                onDismiss()
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = when (selectedAction) {
                                InvestmentAction.BUY -> "Xác nhận Mua tài sản"
                                InvestmentAction.SELL -> "Xác nhận Bán tài sản"
                                InvestmentAction.DIVIDEND -> "Ghi nhận Cổ tức"
                                InvestmentAction.VALUATION -> "Lưu Định giá mới"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditCardSheet(
    onDismiss: () -> Unit,
    onSave: (
        nickname: String,
        issuerBank: String,
        network: CardNetwork,
        last4: String,
        creditLimitMinor: Long,
        statementDay: Int,
        dueDaysAfterStatement: Int,
        colorHex: String,
        cardType: CardType,
        initialBalanceMinor: Long
    ) -> Unit
) {
    var selectedCardType by remember { mutableStateOf(CardType.CREDIT) }
    val banks = com.dvt.moneylens.domain.calc.BankParserEngine.allSupportedBanks
    val walletProviders = listOf(
        Pair("MoMo", "#D82D8B"),
        Pair("ZaloPay", "#0068FF"),
        Pair("Viettel Money", "#EE0033"),
        Pair("ShopeePay", "#EE4D2D"),
        Pair("VNPay", "#005BAA"),
        Pair("Khác", "#0A6B5C")
    )
    var selectedWalletProvider by remember { mutableStateOf(walletProviders.first()) }
    var selectedBank by remember { mutableStateOf(banks.firstOrNull { it.code == "SHB" } ?: banks.first()) }
    var selectedNetwork by remember { mutableStateOf(CardNetwork.VISA) }
    var nickname by remember { mutableStateOf("${selectedBank.shortName} Platinum") }
    var last4 by remember { mutableStateOf("8888") }
    var creditLimitStr by remember { mutableStateOf("50000000") }
    var initialBalanceStr by remember { mutableStateOf("0") }
    var statementDayStr by remember { mutableStateOf("20") }
    var dueDaysStr by remember { mutableStateOf("25") }

    val focusManager = LocalFocusManager.current
    val last4FocusRequester = remember { FocusRequester() }
    val amountOrLimitFocusRequester = remember { FocusRequester() }
    val stmtDayFocusRequester = remember { FocusRequester() }
    val dueDaysFocusRequester = remember { FocusRequester() }

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
                text = "Thêm thẻ",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Quản lý thẻ tín dụng, thẻ ghi nợ (Debit) hoặc ví điện tử",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Card Type Selector: Credit vs Debit vs Wallet vs Bank Account (Hàng cuộn ngang duy nhất)
            Text("Chọn loại thẻ / ví / tài khoản:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = selectedCardType == CardType.CREDIT,
                    onClick = {
                        selectedCardType = CardType.CREDIT
                        nickname = "${selectedBank.shortName} Platinum"
                    },
                    label = { Text("💳 Thẻ Credit") }
                )
                FilterChip(
                    selected = selectedCardType == CardType.DEBIT,
                    onClick = {
                        selectedCardType = CardType.DEBIT
                        nickname = "${selectedBank.shortName} Debit"
                        selectedNetwork = CardNetwork.NAPAS
                    },
                    label = { Text("💳 Thẻ Debit") }
                )
                FilterChip(
                    selected = selectedCardType == CardType.WALLET,
                    onClick = {
                        selectedCardType = CardType.WALLET
                        nickname = "Ví ${selectedWalletProvider.first}"
                    },
                    label = { Text("📱 Ví điện tử") }
                )
                FilterChip(
                    selected = selectedCardType == CardType.BANK_ACCOUNT,
                    onClick = {
                        selectedCardType = CardType.BANK_ACCOUNT
                        nickname = "TK ${selectedBank.shortName}"
                    },
                    label = { Text("🏛️ Tài khoản") }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (selectedCardType == CardType.WALLET) {
                // Wallet Provider Selection
                Text("Chọn ví điện tử:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Spacing.xs))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(walletProviders) { wp ->
                        FilterChip(
                            selected = selectedWalletProvider.first == wp.first,
                            onClick = {
                                selectedWalletProvider = wp
                                nickname = "Ví ${wp.first}"
                            },
                            label = { Text(wp.first) }
                        )
                    }
                }
            } else {
                // Bank Selection for Credit, Debit & Bank Account
                Text("Chọn ngân hàng phát hành:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Spacing.xs))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(banks) { b ->
                        FilterChip(
                            selected = selectedBank.code == b.code,
                            onClick = {
                                selectedBank = b
                                nickname = when (selectedCardType) {
                                    CardType.CREDIT -> "${b.shortName} Platinum"
                                    CardType.DEBIT -> "${b.shortName} Debit"
                                    CardType.BANK_ACCOUNT -> "TK ${b.shortName}"
                                    CardType.WALLET -> nickname
                                }
                            },
                            label = { Text(b.shortName) }
                        )
                    }
                }

                if (selectedCardType == CardType.CREDIT || selectedCardType == CardType.DEBIT) {
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text("Tổ chức phát hành thẻ:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        CardNetwork.values().forEach { net ->
                            FilterChip(
                                selected = selectedNetwork == net,
                                onClick = { selectedNetwork = net },
                                label = { Text(net.label, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(when (selectedCardType) {
                    CardType.WALLET -> "Tên gợi nhớ ví"
                    CardType.BANK_ACCOUNT -> "Tên gợi nhớ tài khoản"
                    else -> "Tên gọi gợi nhớ thẻ"
                }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try {
                        last4FocusRequester.requestFocus()
                    } catch (e: Exception) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Number input and Balance / Limit input
            if (selectedCardType == CardType.CREDIT) {
                // Credit Card: 4 số cuối & Hạn mức
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("4 số cuối") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                amountOrLimitFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(last4FocusRequester)
                    )

                    CurrencyAmountTextField(
                        value = creditLimitStr,
                        onValueChange = { creditLimitStr = it },
                        label = "Hạn mức thẻ (VND)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                stmtDayFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }),
                        supportingText = VietnameseCurrencyFormatter.parseToMinor(creditLimitStr).let {
                            if (it > 0) { { Text("Bằng chữ: ${Money(it).formatVnd()}") } } else null
                        },
                        modifier = Modifier
                            .weight(2f)
                            .focusRequester(amountOrLimitFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Ngày sao kê & Ngày miễn lãi (CHỈ CÓ Ở CREDIT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = statementDayStr,
                        onValueChange = { statementDayStr = it },
                        label = { Text("Ngày sao kê (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                dueDaysFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(stmtDayFocusRequester)
                    )

                    OutlinedTextField(
                        value = dueDaysStr,
                        onValueChange = { dueDaysStr = it },
                        label = { Text("Số ngày miễn lãi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(dueDaysFocusRequester)
                    )
                }
            } else if (selectedCardType == CardType.DEBIT) {
                // Debit Card: 4 số cuối thẻ & Số dư ban đầu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("4 số cuối thẻ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                amountOrLimitFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(last4FocusRequester)
                    )

                    CurrencyAmountTextField(
                        value = initialBalanceStr,
                        onValueChange = { initialBalanceStr = it },
                        label = "Số dư ban đầu (VNĐ)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        supportingText = VietnameseCurrencyFormatter.parseToMinor(initialBalanceStr).let {
                            if (it > 0) { { Text("Bằng chữ: ${Money(it).formatVnd()}") } } else null
                        },
                        modifier = Modifier
                            .weight(2f)
                            .focusRequester(amountOrLimitFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0D8253).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF0D8253),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Thẻ Debit không tính lãi hay sao kê. Bạn có thể chọn thẻ này để nhận tiền trực tiếp từ các khoản thu nhập.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0D8253)
                        )
                    }
                }
            } else if (selectedCardType == CardType.BANK_ACCOUNT) {
                // Bank Account: 2 rows riêng biệt (Row 1: Số tài khoản, Row 2: Số dư ban đầu)
                OutlinedTextField(
                    value = last4,
                    onValueChange = { last4 = it },
                    label = { Text("Số tài khoản ngân hàng") },
                    placeholder = { Text("Số tài khoản") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        try {
                            amountOrLimitFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(last4FocusRequester)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                CurrencyAmountTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it },
                    label = "Số dư khả dụng ban đầu (VNĐ)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    supportingText = VietnameseCurrencyFormatter.parseToMinor(initialBalanceStr).let {
                        if (it > 0) { { Text("Bằng chữ: ${Money(it).formatVnd()}") } } else null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountOrLimitFocusRequester)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1565C0).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Tài khoản ngân hàng thông thường được sử dụng để nhận tiền từ các khoản thu và thanh toán chi tiêu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1565C0)
                        )
                    }
                }
            } else {
                // Wallet: SĐT/4 số cuối & Số dư ví ban đầu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { last4 = it },
                        label = { Text("SĐT / 4 số cuối") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                amountOrLimitFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(last4FocusRequester)
                    )

                    CurrencyAmountTextField(
                        value = initialBalanceStr,
                        onValueChange = { initialBalanceStr = it },
                        label = "Số dư ví ban đầu (VNĐ)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        supportingText = VietnameseCurrencyFormatter.parseToMinor(initialBalanceStr).let {
                            if (it > 0) { { Text("Bằng chữ: ${Money(it).formatVnd()}") } } else null
                        },
                        modifier = Modifier
                            .weight(2f)
                            .focusRequester(amountOrLimitFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Ví điện tử giúp bạn quản lý số dư khả dụng, chi tiêu hàng ngày và nhận tiền từ các khoản thu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            val limit = VietnameseCurrencyFormatter.parseToMinor(creditLimitStr)
            val initialBalance = VietnameseCurrencyFormatter.parseToMinor(initialBalanceStr)
            val statementDay = (statementDayStr.toIntOrNull() ?: 15).coerceIn(1, 31)
            val dueDays = (dueDaysStr.toIntOrNull() ?: 20).coerceIn(15, 55)

            val isFormValid = when (selectedCardType) {
                CardType.CREDIT -> nickname.isNotBlank() && last4.isNotBlank() && limit > 0
                CardType.DEBIT -> nickname.isNotBlank() && last4.isNotBlank()
                CardType.WALLET -> nickname.isNotBlank()
                CardType.BANK_ACCOUNT -> nickname.isNotBlank() && last4.isNotBlank()
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        val issuer = if (selectedCardType == CardType.WALLET) selectedWalletProvider.first else selectedBank.shortName
                        val color = if (selectedCardType == CardType.WALLET) selectedWalletProvider.second else selectedBank.colorHex
                        onSave(
                            nickname,
                            issuer,
                            selectedNetwork,
                            last4.ifBlank { "0000" },
                            if (selectedCardType == CardType.CREDIT) limit else 0L,
                            if (selectedCardType == CardType.CREDIT) statementDay else 0,
                            if (selectedCardType == CardType.CREDIT) dueDays else 0,
                            color,
                            selectedCardType,
                            if (selectedCardType != CardType.CREDIT) initialBalance else 0L
                        )
                        onDismiss()
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when (selectedCardType) {
                        CardType.CREDIT -> "Lưu thẻ Credit ${selectedBank.shortName}"
                        CardType.DEBIT -> "Lưu thẻ Debit ${selectedBank.shortName}"
                        CardType.WALLET -> "Lưu ${nickname.ifBlank { "Ví điện tử" }}"
                        CardType.BANK_ACCOUNT -> "Lưu tài khoản ${selectedBank.shortName}"
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

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
    var totalBudgetStr by remember {
        mutableStateOf(VietnameseCurrencyFormatter.formatMinor(if (currentTotalBudgetMinor > 0) currentTotalBudgetMinor else 20_000_000L))
    }
    val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
    val categoryBudgetsMap = remember(budgets, expenseCategories) {
        val map = mutableStateMapOf<String, String>()
        expenseCategories.forEach { cat ->
            val b = budgets.find { it.categoryId == cat.id }?.limitMinor
                ?: (if (currentTotalBudgetMinor > 0) currentTotalBudgetMinor / expenseCategories.size.coerceAtLeast(1) else 3_000_000L)
            map[cat.id] = VietnameseCurrencyFormatter.formatMinor(b)
        }
        map
    }

    val focusManager = LocalFocusManager.current
    val categoryFocusRequesters = remember(expenseCategories) {
        expenseCategories.associate { it.id to FocusRequester() }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Thiết lập Hạn mức Ngân sách",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = "Hệ thống sẽ gửi thông báo cảnh báo khi chi tiêu chạm 80% hoặc vượt 100% hạn mức.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Tổng hạn mức chi tiêu tháng (VND):",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            val parsedTotalBudget = VietnameseCurrencyFormatter.parseToMinor(totalBudgetStr)
            CurrencyAmountTextField(
                value = totalBudgetStr,
                onValueChange = { totalBudgetStr = it },
                label = "Tổng ngân sách cả tháng",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    val firstCatId = expenseCategories.firstOrNull()?.id
                    if (firstCatId != null) {
                        try {
                            categoryFocusRequesters[firstCatId]?.requestFocus()
                        } catch (e: Exception) {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    } else {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                supportingText = if (parsedTotalBudget > 0L) {
                    { Text("Bằng chữ: ${Money(parsedTotalBudget).formatVnd()}") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                listOf(
                    15_000_000L to "15 triệu",
                    20_000_000L to "20 triệu",
                    25_000_000L to "25 triệu",
                    30_000_000L to "30 triệu"
                ).forEach { (amount, label) ->
                    FilterChip(
                        selected = parsedTotalBudget == amount,
                        onClick = { totalBudgetStr = VietnameseCurrencyFormatter.formatMinor(amount) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Hạn mức chi tiết theo từng danh mục:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            // Hiển thị trực tiếp các danh mục bên trong Column cuộn để không bị bàn phím che khuất
            expenseCategories.forEachIndexed { index, cat ->
                val currentVal = categoryBudgetsMap[cat.id] ?: VietnameseCurrencyFormatter.formatMinor(3_000_000L)
                val isLast = index == expenseCategories.lastIndex
                val nextCatId = if (!isLast) expenseCategories.getOrNull(index + 1)?.id else null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryAvatar(category = cat, size = 32.dp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { categoryBudgetsMap[cat.id] = VietnameseCurrencyFormatter.formatInput(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (nextCatId != null) {
                                    try {
                                        categoryFocusRequesters[nextCatId]?.requestFocus()
                                    } catch (e: Exception) {
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                } else {
                                    focusManager.clearFocus()
                                }
                            },
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .width(140.dp)
                            .focusRequester(categoryFocusRequesters[cat.id] ?: remember { FocusRequester() })
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Lưu hạn mức ngân sách")
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
fun OnboardingDialog(
    initialUserName: String = "MoneyLens",
    onComplete: (useDemoData: Boolean, userName: String, userAvatar: String, pinCode: String?, enableBiometric: Boolean) -> Unit
) {
    var selectedDataOption by remember { mutableStateOf(true) } // true: Demo, false: Clean
    var userNameInput by remember { mutableStateOf(initialUserName) }
    val avatarOptions = listOf("💼", "🪙", "📊", "🌟", "🚀")
    var selectedAvatar by remember { mutableStateOf("💼") }
    var enablePin by remember { mutableStateOf(true) }
    var pinCodeInput by remember { mutableStateOf("123456") }
    var enableBio by remember { mutableStateOf(true) }
    var pinError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Không cho dismiss ngoài màn hình */ },
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = selectedAvatar, fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Chào mừng đến MoneyLens",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Thiết lập nhanh để bắt đầu quản lý tài chính",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 1. Lựa chọn dữ liệu
                item {
                    Text(
                        text = "1. Chế độ dữ liệu ban đầu:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        FilterChip(
                            selected = selectedDataOption,
                            onClick = { selectedDataOption = true },
                            label = { Text("🚀 Dữ liệu mẫu") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !selectedDataOption,
                            onClick = { selectedDataOption = false },
                            label = { Text("🆕 Bắt đầu mới") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (selectedDataOption)
                            "Nạp 10 giao dịch mẫu, thẻ tín dụng và danh mục BĐS để trải nghiệm tính năng ngay."
                        else
                            "Tạo sổ thu chi hoàn toàn trống sạch để bạn tự ghi chép từ đầu.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 2. Thông tin cá nhân
                item {
                    Text(
                        text = "2. Thông tin cá nhân của bạn:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        label = { Text("Họ và tên / Biệt danh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Chọn biểu tượng đại diện:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        avatarOptions.forEach { avatar ->
                            val isSelected = selectedAvatar == avatar
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (isSelected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { selectedAvatar = avatar }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = avatar, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Thiết lập mật khẩu PIN 6 số
                item {
                    Text(
                        text = "3. Bảo mật ứng dụng:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bảo vệ bằng mã PIN 6 số",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = enablePin,
                            onCheckedChange = {
                                enablePin = it
                                if (!it) enableBio = false
                            }
                        )
                    }

                    if (enablePin) {
                        OutlinedTextField(
                            value = pinCodeInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    pinCodeInput = it
                                    pinError = ""
                                }
                            },
                            label = { Text("Mã PIN (6 chữ số)") },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError.isNotBlank(),
                            supportingText = if (pinError.isNotBlank()) {
                                { Text(pinError, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mở khóa bằng Vân tay / Khuôn mặt",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = enableBio && enablePin,
                                enabled = enablePin,
                                onCheckedChange = { enableBio = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enablePin && pinCodeInput.length != 6) {
                        pinError = "Mã PIN phải đủ 6 chữ số!"
                        return@Button
                    }
                    onComplete(
                        selectedDataOption,
                        userNameInput.ifBlank { "MoneyLens" },
                        selectedAvatar,
                        if (enablePin) pinCodeInput else null,
                        enableBio && enablePin
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bắt đầu khám phá MoneyLens")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanBottomSheet(
    accounts: List<Account>,
    initialType: LoanType = LoanType.BORROWING,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        counterparty: String,
        type: LoanType,
        originalAmountMinor: Long,
        interestRate: Double,
        interestType: InterestType,
        termMonths: Int,
        monthlyPaymentMinor: Long,
        startDate: Long,
        dueDayOfMonth: Int,
        notes: String?,
        linkedAccountId: String?,
        colorHex: String,
        conversionFeeMinor: Long,
        conversionFeeSchedule: ConversionFeeSchedule,
        conversionFeePercent: Double
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var interestType by remember { mutableStateOf(InterestType.ZERO_PERCENT) }
    var interestRateStr by remember { mutableStateOf("0.0") }
    var termMonthsStr by remember { mutableStateOf("12") }
    var dueDayStr by remember { mutableStateOf("15") }
    var note by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    // Conversion Fee State
    var hasConversionFee by remember { mutableStateOf(false) }
    var feeModeIsPercent by remember { mutableStateOf(false) } // false = VNĐ, true = %
    var conversionFeeAmountStr by remember { mutableStateOf("") }
    var conversionFeePercentStr by remember { mutableStateOf("3.0") }
    var conversionFeeSchedule by remember { mutableStateOf(ConversionFeeSchedule.FIRST_MONTH) }

    // Flexible vs Installment Repayment State
    var isFlexibleRepayment by remember { mutableStateOf(false) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val currentCal = java.util.Calendar.getInstance()
    val defaultMonthStr = String.format(Locale.US, "%02d/%04d", currentCal.get(java.util.Calendar.MONTH) + 1, currentCal.get(java.util.Calendar.YEAR))
    var startMonthStr by remember { mutableStateOf(defaultMonthStr) }

    val colors = listOf("#1E88E5", "#0A6B5C", "#E8A317", "#8E24AA", "#D32F2F")
    var selectedColor by remember { mutableStateOf(colors.first()) }

    // Live calculation for monthly installment
    val parsedAmount = VietnameseCurrencyFormatter.parseToMinor(amountStr)
    val parsedTerm = if (isFlexibleRepayment) 0 else (termMonthsStr.toIntOrNull()?.coerceAtLeast(1) ?: 12)
    val parsedRate = if (isFlexibleRepayment) 0.0 else (interestRateStr.toDoubleOrNull() ?: 0.0)

    // Parse start date from MM/yyyy
    val parsedStartDate = remember(startMonthStr, dueDayStr) {
        runCatching {
            val parts = startMonthStr.trim().split("/")
            if (parts.size == 2) {
                val m = parts[0].toInt() - 1
                val y = parts[1].toInt()
                val d = dueDayStr.toIntOrNull()?.coerceIn(1, 28) ?: 15
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, y)
                    set(java.util.Calendar.MONTH, m)
                    set(java.util.Calendar.DAY_OF_MONTH, d)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                }
                cal.timeInMillis
            } else {
                System.currentTimeMillis()
            }
        }.getOrDefault(System.currentTimeMillis())
    }

    val calculatedMonthlyPayment = remember(parsedAmount, parsedTerm, parsedRate, interestType, isFlexibleRepayment) {
        if (isFlexibleRepayment || parsedAmount <= 0 || parsedTerm <= 0) 0L
        else when (interestType) {
            InterestType.ZERO_PERCENT, InterestType.NO_INTEREST -> parsedAmount / parsedTerm
            InterestType.PERCENT_YEAR -> {
                val monthlyPrincipal = parsedAmount / parsedTerm
                val monthlyInterest = (parsedAmount * (parsedRate / 100.0) / 12.0).toLong()
                monthlyPrincipal + monthlyInterest
            }
            InterestType.FIXED_INTEREST -> {
                val totalWithInterest = (parsedAmount * (1.0 + parsedRate / 100.0)).toLong()
                totalWithInterest / parsedTerm
            }
        }
    }

    // Conversion fee calculation
    val parsedFeeAmount = if (feeModeIsPercent) {
        val pct = conversionFeePercentStr.toDoubleOrNull() ?: 0.0
        if (parsedAmount > 0 && pct > 0) ((parsedAmount * pct) / 100.0).toLong() else 0L
    } else {
        VietnameseCurrencyFormatter.parseToMinor(conversionFeeAmountStr)
    }

    val calculatedFeePercent = if (parsedAmount > 0 && parsedFeeAmount > 0) {
        (parsedFeeAmount.toDouble() / parsedAmount) * 100.0
    } else if (feeModeIsPercent) {
        conversionFeePercentStr.toDoubleOrNull() ?: 0.0
    } else 0.0

    val finalConversionFeeMinor = if (hasConversionFee) parsedFeeAmount else 0L

    val firstMonthPayment = remember(calculatedMonthlyPayment, finalConversionFeeMinor, conversionFeeSchedule, parsedTerm) {
        if (conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) {
            calculatedMonthlyPayment + finalConversionFeeMinor
        } else {
            val monthlyFee = if (parsedTerm > 0) finalConversionFeeMinor / parsedTerm else 0L
            calculatedMonthlyPayment + monthlyFee
        }
    }

    val regularMonthlyPayment = remember(calculatedMonthlyPayment, finalConversionFeeMinor, conversionFeeSchedule, parsedTerm) {
        if (conversionFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY) {
            val monthlyFee = if (parsedTerm > 0) finalConversionFeeMinor / parsedTerm else 0L
            calculatedMonthlyPayment + monthlyFee
        } else {
            calculatedMonthlyPayment
        }
    }

    val totalCostMinor = remember(parsedAmount, finalConversionFeeMinor, calculatedMonthlyPayment, parsedTerm) {
        if (calculatedMonthlyPayment > 0) {
            (calculatedMonthlyPayment * parsedTerm) + (if (conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) finalConversionFeeMinor else 0L)
        } else {
            parsedAmount + finalConversionFeeMinor
        }
    }
    val focusManager = LocalFocusManager.current
    val counterpartyFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }
    val termFocusRequester = remember { FocusRequester() }
    val rateFocusRequester = remember { FocusRequester() }
    val dueDayFocusRequester = remember { FocusRequester() }
    val feeFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = if (selectedType == LoanType.BORROWING) "Thêm khoản Đi vay & Trả góp" else "Thêm khoản Cho vay",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Loan Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = selectedType == LoanType.BORROWING,
                    onClick = { selectedType = LoanType.BORROWING },
                    label = { Text("🏦 Đi vay / Mua trả góp") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == LoanType.LENDING,
                    onClick = { selectedType = LoanType.LENDING },
                    label = { Text("🤝 Cho vay (Phải thu)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Title & Counterparty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên khoản nợ") },
                    placeholder = { Text("VD: Trả góp iPhone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        try {
                            counterpartyFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    }),
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text("Đối tác") },
                    placeholder = { Text("Đối tác") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        try {
                            amountFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(counterpartyFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Original Amount
            CurrencyAmountTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Số tiền vay ban đầu (VNĐ)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (isFlexibleRepayment) ImeAction.Done else ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        try {
                            termFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    },
                    onDone = { focusManager.clearFocus() }
                ),
                supportingText = if (parsedAmount > 0) {
                    { Text("Bằng chữ: ${Money(parsedAmount).formatVnd()}") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Repayment Mode Selector (Trả tự do vs Trả góp định kỳ)
            Text(
                text = "Hình thức trả nợ:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = isFlexibleRepayment,
                    onClick = {
                        isFlexibleRepayment = true
                        interestType = InterestType.NO_INTEREST
                        interestRateStr = "0.0"
                    },
                    label = { Text("🚀 Trả tự do") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isFlexibleRepayment,
                    onClick = {
                        isFlexibleRepayment = false
                        if (interestType == InterestType.NO_INTEREST) {
                            interestType = InterestType.ZERO_PERCENT
                        }
                    },
                    label = { Text("📅 Trả góp định kỳ") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isFlexibleRepayment) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Date & Time Picker for Flexible Loan
                DateTimePickerField(
                    timestamp = selectedTimestamp,
                    onTimestampChange = { selectedTimestamp = it },
                    label = if (selectedType == LoanType.BORROWING) "Thời gian nhận tiền vay" else "Thời gian cho vay",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Interest Type Selector for Installment
                Text("Loại lãi suất:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    FilterChip(
                        selected = interestType == InterestType.ZERO_PERCENT,
                        onClick = {
                            interestType = InterestType.ZERO_PERCENT
                            interestRateStr = "0.0"
                        },
                        label = { Text("0% Trả góp") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = interestType == InterestType.PERCENT_YEAR,
                        onClick = {
                            interestType = InterestType.PERCENT_YEAR
                            if (interestRateStr == "0.0") interestRateStr = "8.5"
                        },
                        label = { Text("%/Năm") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = interestType == InterestType.NO_INTEREST,
                        onClick = {
                            interestType = InterestType.NO_INTEREST
                            interestRateStr = "0.0"
                        },
                        label = { Text("Không lãi") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Tháng bắt đầu & Kỳ hạn & Ngày trả
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = startMonthStr,
                        onValueChange = { startMonthStr = it },
                        label = { Text("Tháng bắt đầu") },
                        placeholder = { Text("MM/yyyy") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                termFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = termMonthsStr,
                        onValueChange = { termMonthsStr = it },
                        label = { Text("Kỳ hạn (tháng)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                if (interestType == InterestType.PERCENT_YEAR) {
                                    rateFocusRequester.requestFocus()
                                } else {
                                    dueDayFocusRequester.requestFocus()
                                }
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(termFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Lãi suất & Ngày trả hàng tháng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (interestType == InterestType.PERCENT_YEAR) {
                        OutlinedTextField(
                            value = interestRateStr,
                            onValueChange = { interestRateStr = it },
                            label = { Text("Lãi suất %/năm") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = {
                                try {
                                    dueDayFocusRequester.requestFocus()
                                } catch (e: Exception) {
                                    focusManager.moveFocus(FocusDirection.Right)
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(rateFocusRequester)
                        )
                    }
                    OutlinedTextField(
                        value = dueDayStr,
                        onValueChange = { dueDayStr = it },
                        label = { Text("Ngày trả (1-31)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (selectedType == LoanType.BORROWING && hasConversionFee) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                try {
                                    feeFocusRequester.requestFocus()
                                } catch (e: Exception) {
                                    focusManager.clearFocus()
                                }
                            },
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(dueDayFocusRequester)
                    )
                }

                // PHÍ CHUYỂN ĐỔI TRẢ GÓP (Dành cho Đi vay / Trả góp)
                if (selectedType == LoanType.BORROWING) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, if (hasConversionFee) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (hasConversionFee) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Column {
                                        Text(
                                            text = "Phí chuyển đổi trả góp",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Phí trả cho bên cho vay để chuyển đổi trả góp",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = hasConversionFee,
                                    onCheckedChange = { hasConversionFee = it }
                                )
                            }

                            if (hasConversionFee) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Chọn đơn vị nhập (VNĐ hoặc %)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    FilterChip(
                                        selected = !feeModeIsPercent,
                                        onClick = { feeModeIsPercent = false },
                                        label = { Text("Theo số tiền (VNĐ)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = feeModeIsPercent,
                                        onClick = { feeModeIsPercent = true },
                                        label = { Text("Theo tỷ lệ %") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(Spacing.xs))

                                if (feeModeIsPercent) {
                                    OutlinedTextField(
                                        value = conversionFeePercentStr,
                                        onValueChange = { conversionFeePercentStr = it },
                                        label = { Text("Phí chuyển đổi (%)") },
                                        placeholder = { Text("VD: 2.99, 3.5") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        supportingText = if (parsedAmount > 0 && parsedFeeAmount > 0) {
                                            { Text("Tương đương: ${Money(parsedFeeAmount).formatVnd()}") }
                                        } else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(feeFocusRequester)
                                    )
                                } else {
                                    CurrencyAmountTextField(
                                        value = conversionFeeAmountStr,
                                        onValueChange = { conversionFeeAmountStr = it },
                                        label = "Phí chuyển đổi (VNĐ)",
                                        placeholder = "0",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        supportingText = if (parsedAmount > 0 && parsedFeeAmount > 0) {
                                            { Text("Bằng chữ: ${Money(parsedFeeAmount).formatVnd()} (~${String.format(Locale.US, "%.2f", calculatedFeePercent)}%)") }
                                        } else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(feeFocusRequester)
                                    )
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Hình thức phân bổ phí
                                Text(
                                    text = "Phương thức thu phí:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    FilterChip(
                                        selected = conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH,
                                        onClick = { conversionFeeSchedule = ConversionFeeSchedule.FIRST_MONTH },
                                        label = { Text("Thu 1 lần tháng đầu", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = conversionFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY,
                                        onClick = { conversionFeeSchedule = ConversionFeeSchedule.SPREAD_MONTHLY },
                                        label = { Text("Chia đều các tháng", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Calculation Preview Box
            if (parsedAmount > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        if (isFlexibleRepayment) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hình thức khoản vay:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Trả tự do",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Không tính lãi suất · Trả linh hoạt từng đợt hoặc tất toán khi có tiền",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Dư nợ ban đầu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = Money(parsedAmount).formatVnd(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (hasConversionFee && conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) "Thanh toán tháng đầu (Kỳ 1):" else "Số tiền trả định kỳ hàng tháng:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (hasConversionFee && conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) {
                                            "${Money(firstMonthPayment).formatVnd()} (Kỳ 1)"
                                        } else {
                                            "${Money(regularMonthlyPayment).formatVnd()} / tháng"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (hasConversionFee && conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH && parsedTerm > 1) {
                                        Text(
                                            text = "Từ tháng 2 đến $parsedTerm: ${Money(regularMonthlyPayment).formatVnd()}/tháng",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Kỳ hạn $parsedTerm tháng",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Bắt đầu: $startMonthStr",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            if (hasConversionFee && finalConversionFeeMinor > 0) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⚡ Phí chuyển đổi: ${Money(finalConversionFeeMinor).formatVnd()} (${String.format(Locale.US, "%.2f", calculatedFeePercent)}%) · ${conversionFeeSchedule.label}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Submit Button
            val isFormValid = title.isNotBlank() && counterparty.isNotBlank() && parsedAmount > 0
            Button(
                onClick = {
                    if (isFormValid) {
                        onSave(
                            title,
                            counterparty,
                            selectedType,
                            parsedAmount,
                            if (isFlexibleRepayment) 0.0 else parsedRate,
                            if (isFlexibleRepayment) InterestType.NO_INTEREST else interestType,
                            if (isFlexibleRepayment) 0 else parsedTerm,
                            if (isFlexibleRepayment) 0L else regularMonthlyPayment,
                            if (isFlexibleRepayment) selectedTimestamp else parsedStartDate,
                            if (isFlexibleRepayment) 0 else (dueDayStr.toIntOrNull() ?: 15),
                            note.ifBlank { null },
                            selectedAccountId,
                            selectedColor,
                            if (isFlexibleRepayment) 0L else finalConversionFeeMinor,
                            if (isFlexibleRepayment) ConversionFeeSchedule.FIRST_MONTH else conversionFeeSchedule,
                            if (isFlexibleRepayment) 0.0 else calculatedFeePercent
                        )
                        onDismiss()
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (selectedType == LoanType.BORROWING) "Lưu khoản Vay / Trả góp" else "Lưu khoản Cho vay",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayLoanBottomSheet(
    loan: Loan,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmPayment: (amountMinor: Long, principalMinor: Long, interestMinor: Long, accountId: String?, note: String, paidAt: Long) -> Unit
) {
    val isBorrowing = loan.type == LoanType.BORROWING
    val defaultInitialAmount = when {
        loan.conversionFeeMinor > 0 && loan.conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH -> {
            if (loan.paidAmountMinor == 0L) loan.firstMonthPaymentMinor else loan.regularMonthlyPaymentMinor
        }
        loan.conversionFeeMinor > 0 && loan.conversionFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY -> {
            loan.regularMonthlyPaymentMinor
        }
        loan.monthlyPaymentMinor > 0 -> loan.monthlyPaymentMinor
        else -> loan.remainingPrincipalMinor
    }

    val isFlexibleLoan = loan.termMonths == 0 || loan.monthlyPaymentMinor == 0L

    var amountStr by remember {
        mutableStateOf(VietnameseCurrencyFormatter.formatMinor(defaultInitialAmount))
    }
    var note by remember {
        mutableStateOf(if (isFlexibleLoan) (if (isBorrowing) "Trả nợ: ${loan.title}" else "Thu nợ: ${loan.title}") else "Thanh toán kỳ hạn ${loan.title}")
    }
    var selectedAccountId by remember {
        mutableStateOf(loan.linkedAccountId ?: accounts.firstOrNull()?.id)
    }
    var selectedTimestamp by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    val parsedAmount = VietnameseCurrencyFormatter.parseToMinor(amountStr)

    // Auto-split principal and interest
    val estimatedInterest = remember(parsedAmount, loan) {
        if (loan.interestRate > 0 && loan.interestType == InterestType.PERCENT_YEAR) {
            ((loan.remainingPrincipalMinor * (loan.interestRate / 100.0) / 12.0).toLong()).coerceAtMost(parsedAmount)
        } else 0L
    }
    val estimatedPrincipal = (parsedAmount - estimatedInterest).coerceAtLeast(0L)
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = if (isBorrowing) "Thanh toán nợ: ${loan.title}" else "Thu hồi nợ: ${loan.title}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (isFlexibleLoan) {
                    "Trả tự do · Đối tác: ${loan.counterparty} · Dư nợ gốc: ${Money(loan.remainingPrincipalMinor).formatVnd()}"
                } else {
                    "Đối tác: ${loan.counterparty} · Dư nợ gốc: ${Money(loan.remainingPrincipalMinor).formatVnd()}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Quick Pay Amount Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                if (isFlexibleLoan) {
                    // Quick chips for flexible loans: All, 50%, etc.
                    FilterChip(
                        selected = parsedAmount == loan.remainingPrincipalMinor,
                        onClick = { amountStr = VietnameseCurrencyFormatter.formatMinor(loan.remainingPrincipalMinor) },
                        label = { Text("Tất toán (${Money(loan.remainingPrincipalMinor).formatVnd()})") },
                        modifier = Modifier.weight(1f)
                    )
                    if (loan.remainingPrincipalMinor >= 2_000_000L) {
                        val halfAmount = loan.remainingPrincipalMinor / 2
                        FilterChip(
                            selected = parsedAmount == halfAmount,
                            onClick = { amountStr = VietnameseCurrencyFormatter.formatMinor(halfAmount) },
                            label = { Text("Trả 50% (${Money(halfAmount).formatVnd()})") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    if (loan.conversionFeeMinor > 0 && loan.conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) {
                        if (loan.paidAmountMinor == 0L) {
                            FilterChip(
                                selected = parsedAmount == loan.firstMonthPaymentMinor,
                                onClick = { amountStr = VietnameseCurrencyFormatter.formatMinor(loan.firstMonthPaymentMinor) },
                                label = { Text("Kỳ 1 (${Money(loan.firstMonthPaymentMinor).formatVnd()})") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FilterChip(
                            selected = parsedAmount == loan.regularMonthlyPaymentMinor,
                            onClick = { amountStr = VietnameseCurrencyFormatter.formatMinor(loan.regularMonthlyPaymentMinor) },
                            label = { Text("Kỳ sau (${Money(loan.regularMonthlyPaymentMinor).formatVnd()})") },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (loan.regularMonthlyPaymentMinor > 0) {
                        FilterChip(
                            selected = parsedAmount == loan.regularMonthlyPaymentMinor,
                            onClick = { amountStr = VietnameseCurrencyFormatter.formatMinor(loan.regularMonthlyPaymentMinor) },
                            label = { Text("Kỳ hạn (${Money(loan.regularMonthlyPaymentMinor).formatVnd()})") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = parsedAmount == loan.remainingPrincipalMinor,
                        onClick = { amountStr = VietnameseCurrencyFormatter.formatMinor(loan.remainingPrincipalMinor) },
                        label = { Text("Tất toán (${Money(loan.remainingPrincipalMinor).formatVnd()})") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            CurrencyAmountTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = if (isBorrowing) "Số tiền trả (VNĐ)" else "Số tiền thu hồi (VNĐ)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                supportingText = if (parsedAmount > 0) {
                    { Text("Bằng chữ: ${Money(parsedAmount).formatVnd()}") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Thời gian trả nợ / thu nợ
            DateTimePickerField(
                timestamp = selectedTimestamp,
                onTimestampChange = { selectedTimestamp = it },
                label = if (isBorrowing) "Thời gian thực hiện trả nợ" else "Thời gian thực hiện thu nợ",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Chú thích / Ghi chú cho lần trả này
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú lần trả") },
                placeholder = { Text("Trả đợt 1, trả trước hạn...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Breakdown Box (Gốc & Lãi & Phí)
            if (parsedAmount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Khấu trừ nợ gốc:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                Money(estimatedPrincipal).formatVnd(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (estimatedInterest > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tiền lãi kỳ hạn:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Money(estimatedInterest).formatVnd(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                        if (loan.conversionFeeMinor > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Phí chuyển đổi:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${Money(loan.conversionFeeMinor).formatVnd()} (${loan.conversionFeeSchedule.label})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Select Account
            Text(
                text = if (isBorrowing) "Trích tiền từ tài khoản:" else "Nhận tiền vào tài khoản:",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(accounts.filter { it.type != AccountType.CREDIT_CARD }) { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = { selectedAccountId = acc.id },
                        label = { Text("${acc.name} (${Money(acc.balanceMinor).formatVnd()})") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = {
                    if (parsedAmount > 0) {
                        onConfirmPayment(
                            parsedAmount,
                            estimatedPrincipal,
                            estimatedInterest,
                            selectedAccountId,
                            note,
                            selectedTimestamp
                        )
                        onDismiss()
                    }
                },
                enabled = parsedAmount > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isBorrowing) "Xác nhận Trả nợ" else "Xác nhận Thu nợ",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Modal popup xem chi tiết khoản vay / nợ / trả góp kèm lịch sử các lần thu / trả nợ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailBottomSheet(
    loan: Loan,
    payments: List<LoanPayment>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeletePaymentClick: (paymentId: String) -> Unit = {}
) {
    val isBorrowing = loan.type == LoanType.BORROWING
    val primaryColor = runCatching { Color(android.graphics.Color.parseColor(loan.colorHex)) }
        .getOrDefault(if (isBorrowing) MaterialTheme.colorScheme.primary else Color(0xFF8E24AA))

    var paymentToDelete by remember { mutableStateOf<LoanPayment?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBorrowing) Icons.Rounded.RequestQuote else Icons.Rounded.Handshake,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = loan.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${if (isBorrowing) "Vay từ" else "Cho mượn"}: ${loan.counterparty}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Nút chỉnh sửa khoản vay
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Edit,
                            contentDescription = "Chỉnh sửa thông tin khoản vay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Nút xóa khoản vay
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xóa khoản vay",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Status & Progress Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isBorrowing) "Dư nợ gốc còn lại" else "Số tiền còn phải thu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Money(loan.remainingPrincipalMinor).formatVnd(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (loan.isPaidOff) Color(0xFF0D8253) else if (isBorrowing) Color(0xFFC93B2B) else primaryColor
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (loan.isPaidOff) Color(0xFF0D8253).copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (loan.isPaidOff) "Đã tất toán ✓" else if (isBorrowing) "Đang vay nợ" else "Đang cho vay",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (loan.isPaidOff) Color(0xFF0D8253) else primaryColor,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    LinearProgressIndicator(
                        progress = { (loan.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (loan.isPaidOff) Color(0xFF0D8253) else primaryColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Đã thu/trả: ${Money(loan.paidAmountMinor).formatVnd()} (${String.format(Locale.US, "%.1f", loan.progressPercent)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tổng gốc: ${Money(loan.originalAmountMinor).formatVnd()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Loan Detailed Attributes Grid
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Thông tin hợp đồng & điều khoản",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    val startFormatted = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(loan.startDate))
                    DetailInfoRow("Ngày bắt đầu:", startFormatted)

                    val isFlexible = loan.termMonths == 0 || loan.monthlyPaymentMinor == 0L
                    if (isFlexible) {
                        DetailInfoRow("Hình thức trả:", "🚀 Trả tự do (Linh hoạt)")
                    } else {
                        DetailInfoRow("Kỳ hạn vay:", "${loan.termMonths} tháng")
                        DetailInfoRow("Hạn trả định kỳ:", "Ngày ${loan.dueDayOfMonth} hàng tháng")
                        DetailInfoRow("Số tiền trả/kỳ:", Money(loan.monthlyPaymentMinor).formatVnd())
                    }

                    val interestText = when (loan.interestType) {
                        InterestType.PERCENT_YEAR -> "${loan.interestRate}% / năm"
                        InterestType.ZERO_PERCENT -> "0% (Trả góp lãi suất 0)"
                        InterestType.FIXED_INTEREST -> "Lãi suất cố định"
                        InterestType.NO_INTEREST -> "Không tính lãi"
                    }
                    DetailInfoRow("Lãi suất:", interestText)

                    if (loan.conversionFeeMinor > 0) {
                        DetailInfoRow(
                            "Phí chuyển đổi:",
                            "${Money(loan.conversionFeeMinor).formatVnd()} (${loan.conversionFeeSchedule.label})"
                        )
                    }

                    val linkedAcc = accounts.firstOrNull { it.id == loan.linkedAccountId }
                    if (linkedAcc != null) {
                        DetailInfoRow("Ví/Tài khoản liên kết:", linkedAcc.name)
                    }

                    if (!loan.notes.isNullOrBlank()) {
                        DetailInfoRow("Ghi chú hợp đồng:", loan.notes)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Action Button: Trả nợ / Thu nợ ngay
            if (!loan.isPaidOff) {
                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (isBorrowing) Icons.Rounded.Payment else Icons.Rounded.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = if (isBorrowing) "Ghi nhận Trả nợ" else "Ghi nhận Thu hồi nợ",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            // SECTION: LỊCH SỬ CÁC LẦN THU / TRẢ NỢ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch sử các lần ${if (isBorrowing) "trả nợ" else "thu nợ"} (${payments.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (payments.isNotEmpty()) {
                    Text(
                        text = "Tổng: ${Money(payments.sumOf { it.amountMinor }).formatVnd()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (payments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Chưa có lần ${if (isBorrowing) "trả nợ" else "thu nợ"} nào được lưu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Mỗi khi thực hiện thu/trả nợ, lịch sử chi tiết sẽ hiển thị tại đây.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    val dateFormat = java.text.SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault())
                    payments.forEach { payment ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isBorrowing) Color(0xFFC93B2B).copy(alpha = 0.12f) else Color(0xFF0D8253).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isBorrowing) Icons.Rounded.Payment else Icons.Rounded.Savings,
                                            contentDescription = null,
                                            tint = if (isBorrowing) Color(0xFFC93B2B) else Color(0xFF0D8253),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column {
                                        Text(
                                            text = Money(payment.amountMinor).formatVnd(),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isBorrowing) Color(0xFFC93B2B) else Color(0xFF0D8253)
                                        )
                                        Text(
                                            text = dateFormat.format(java.util.Date(payment.paidAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (payment.note.isNotBlank()) {
                                            Text(
                                                text = "📝 ${payment.note}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (payment.principalMinor > 0L || payment.interestMinor > 0L) {
                                            Text(
                                                text = "Gốc: ${Money(payment.principalMinor).formatVnd()}" + if (payment.interestMinor > 0L) " · Lãi: ${Money(payment.interestMinor).formatVnd()}" else "",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { paymentToDelete = payment },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Xóa lượt trả nợ này",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }

    // Dialog xác nhận xóa 1 lần trả nợ
    paymentToDelete?.let { payment ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Hủy lượt ghi nhận thu/trả nợ?") },
            text = {
                Text("Xóa lịch sử lần trả ${Money(payment.amountMinor).formatVnd()} sẽ hoàn lại số tiền gốc vào dư nợ của khoản vay này.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePaymentClick(payment.id)
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Modal cập nhật thông tin khoản vay / nợ / trả góp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanBottomSheet(
    loan: Loan,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (updatedLoan: Loan) -> Unit
) {
    var title by remember { mutableStateOf(loan.title) }
    var counterparty by remember { mutableStateOf(loan.counterparty) }
    var originalAmountStr by remember { mutableStateOf(VietnameseCurrencyFormatter.formatMinor(loan.originalAmountMinor)) }
    var remainingAmountStr by remember { mutableStateOf(VietnameseCurrencyFormatter.formatMinor(loan.remainingPrincipalMinor)) }
    var selectedType by remember { mutableStateOf(loan.type) }
    var isFlexibleRepayment by remember { mutableStateOf(loan.termMonths == 0 || loan.monthlyPaymentMinor == 0L) }
    var interestType by remember { mutableStateOf(loan.interestType) }
    var interestRateStr by remember { mutableStateOf(loan.interestRate.toString()) }
    var termMonthsStr by remember { mutableStateOf(if (loan.termMonths > 0) loan.termMonths.toString() else "12") }
    var dueDayStr by remember { mutableStateOf(loan.dueDayOfMonth.toString()) }
    var selectedTimestamp by remember { mutableStateOf(loan.startDate) }
    var notes by remember { mutableStateOf(loan.notes.orEmpty()) }
    var selectedAccountId by remember { mutableStateOf(loan.linkedAccountId) }
    var selectedColorHex by remember { mutableStateOf(loan.colorHex) }
    var conversionFeeStr by remember { mutableStateOf(if (loan.conversionFeeMinor > 0) VietnameseCurrencyFormatter.formatMinor(loan.conversionFeeMinor) else "") }
    var conversionFeeSchedule by remember { mutableStateOf(loan.conversionFeeSchedule) }

    val parsedOriginal = VietnameseCurrencyFormatter.parseToMinor(originalAmountStr)
    val parsedRemaining = VietnameseCurrencyFormatter.parseToMinor(remainingAmountStr)
    val parsedConversionFee = VietnameseCurrencyFormatter.parseToMinor(conversionFeeStr)

    val colors = listOf("#0A6B5C", "#0D8253", "#1E88E5", "#8E24AA", "#E8A317", "#D81B60", "#F4511E", "#3949AB")

    val focusManager = LocalFocusManager.current
    val counterpartyFocusRequester = remember { FocusRequester() }
    val originalAmountFocusRequester = remember { FocusRequester() }
    val remainingAmountFocusRequester = remember { FocusRequester() }
    val termFocusRequester = remember { FocusRequester() }
    val dueDayFocusRequester = remember { FocusRequester() }
    val rateFocusRequester = remember { FocusRequester() }
    val feeFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = Spacing.xl)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cập nhật khoản ${if (selectedType == LoanType.BORROWING) "Vay / Nợ" else "Cho vay"}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Tên và đối tác
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tên khoản vay / nợ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try {
                        counterpartyFocusRequester.requestFocus()
                    } catch (e: Exception) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (selectedType == LoanType.BORROWING) "Vay từ (Ngân hàng / Người cho vay)" else "Cho ai vay (Người vay)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try {
                        originalAmountFocusRequester.requestFocus()
                    } catch (e: Exception) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(counterpartyFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Gốc ban đầu
            CurrencyAmountTextField(
                value = originalAmountStr,
                onValueChange = { originalAmountStr = it },
                label = "Số tiền gốc ban đầu (VNĐ)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try {
                        remainingAmountFocusRequester.requestFocus()
                    } catch (e: Exception) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                supportingText = if (parsedOriginal > 0) {
                    { Text("Bằng chữ: ${Money(parsedOriginal).formatVnd()}") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(originalAmountFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Dư nợ hiện tại
            CurrencyAmountTextField(
                value = remainingAmountStr,
                onValueChange = { remainingAmountStr = it },
                label = "Dư nợ còn lại hiện tại (VNĐ)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (isFlexibleRepayment) ImeAction.Next else ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    try {
                        if (!isFlexibleRepayment) {
                            termFocusRequester.requestFocus()
                        } else {
                            notesFocusRequester.requestFocus()
                        }
                    } catch (e: Exception) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }),
                supportingText = if (parsedRemaining > 0) {
                    { Text("Bằng chữ: ${Money(parsedRemaining).formatVnd()}") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(remainingAmountFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Hình thức trả nợ
            Text(
                text = "Hình thức trả nợ:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = isFlexibleRepayment,
                    onClick = { isFlexibleRepayment = true },
                    label = { Text("🚀 Trả tự do") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isFlexibleRepayment,
                    onClick = { isFlexibleRepayment = false },
                    label = { Text("📅 Trả góp định kỳ") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isFlexibleRepayment) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = termMonthsStr,
                        onValueChange = { termMonthsStr = it },
                        label = { Text("Kỳ hạn (tháng)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                dueDayFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(termFocusRequester)
                    )
                    OutlinedTextField(
                        value = dueDayStr,
                        onValueChange = { dueDayStr = it },
                        label = { Text("Ngày trả (1-31)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (interestType == InterestType.PERCENT_YEAR) ImeAction.Next else if (selectedType == LoanType.BORROWING) ImeAction.Next else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                try {
                                    if (interestType == InterestType.PERCENT_YEAR) {
                                        rateFocusRequester.requestFocus()
                                    } else if (selectedType == LoanType.BORROWING) {
                                        feeFocusRequester.requestFocus()
                                    } else {
                                        notesFocusRequester.requestFocus()
                                    }
                                } catch (e: Exception) {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(dueDayFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Loại lãi suất
                Text("Loại lãi suất:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    FilterChip(
                        selected = interestType == InterestType.ZERO_PERCENT,
                        onClick = { interestType = InterestType.ZERO_PERCENT; interestRateStr = "0.0" },
                        label = { Text("0% Trả góp") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = interestType == InterestType.PERCENT_YEAR,
                        onClick = { interestType = InterestType.PERCENT_YEAR },
                        label = { Text("%/Năm") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = interestType == InterestType.NO_INTEREST,
                        onClick = { interestType = InterestType.NO_INTEREST; interestRateStr = "0.0" },
                        label = { Text("Không lãi") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (interestType == InterestType.PERCENT_YEAR) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = interestRateStr,
                        onValueChange = { interestRateStr = it },
                        label = { Text("Lãi suất %/năm") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = if (selectedType == LoanType.BORROWING) ImeAction.Next else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                if (selectedType == LoanType.BORROWING) {
                                    feeFocusRequester.requestFocus()
                                } else {
                                    notesFocusRequester.requestFocus()
                                }
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(rateFocusRequester)
                    )
                }

                if (selectedType == LoanType.BORROWING) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    CurrencyAmountTextField(
                        value = conversionFeeStr,
                        onValueChange = { conversionFeeStr = it },
                        label = "Phí chuyển đổi trả góp nếu có (VNĐ)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            try {
                                notesFocusRequester.requestFocus()
                            } catch (e: Exception) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(feeFocusRequester)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Thời gian bắt đầu
            DateTimePickerField(
                timestamp = selectedTimestamp,
                onTimestampChange = { selectedTimestamp = it },
                label = "Thời gian bắt đầu hợp đồng",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Ghi chú
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Ghi chú hợp đồng") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(notesFocusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Màu sắc nhận diện
            Text("Màu sắc nhận diện:", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                colors.forEach { hex ->
                    val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColorHex == hex) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Save Action
            val canSave = title.isNotBlank() && counterparty.isNotBlank() && parsedOriginal > 0L
            Button(
                onClick = {
                    if (canSave) {
                        val parsedTerm = termMonthsStr.toIntOrNull() ?: 12
                        val parsedDueDay = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 15
                        val parsedRate = interestRateStr.toDoubleOrNull() ?: 0.0

                        val calculatedMonthlyPayment = if (isFlexibleRepayment || parsedTerm <= 0) {
                            0L
                        } else {
                            if (parsedRate > 0.0) {
                                val monthlyRate = parsedRate / 100.0 / 12.0
                                val factor = Math.pow(1.0 + monthlyRate, parsedTerm.toDouble())
                                ((parsedOriginal * monthlyRate * factor) / (factor - 1.0)).toLong()
                            } else {
                                parsedOriginal / parsedTerm
                            }
                        }

                        val updated = loan.copy(
                            title = title.trim(),
                            counterparty = counterparty.trim(),
                            originalAmountMinor = parsedOriginal,
                            remainingPrincipalMinor = parsedRemaining,
                            interestRate = parsedRate,
                            interestType = interestType,
                            termMonths = if (isFlexibleRepayment) 0 else parsedTerm,
                            monthlyPaymentMinor = calculatedMonthlyPayment,
                            startDate = selectedTimestamp,
                            dueDayOfMonth = parsedDueDay,
                            status = if (parsedRemaining <= 0L) LoanStatus.PAID_OFF else LoanStatus.ACTIVE,
                            notes = notes.trim().ifBlank { null },
                            linkedAccountId = selectedAccountId,
                            colorHex = selectedColorHex,
                            conversionFeeMinor = parsedConversionFee,
                            conversionFeeSchedule = conversionFeeSchedule
                        )
                        onSave(updated)
                        onDismiss()
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Lưu cập nhật khoản vay", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


