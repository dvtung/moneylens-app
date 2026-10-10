package com.dvt.moneylens.ui.screens.modals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

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

    val parsedAmount = VietnameseCurrencyFormatter.parseToMinor(amountStr)
    val parsedTerm = termMonthsStr.toIntOrNull()?.coerceAtLeast(1) ?: 12

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                text = if (selectedType == LoanType.BORROWING) "Tạo khoản Vay / Trả góp" else "Tạo khoản Cho vay",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tên khoản vay") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text("Đối tác / Ngân hàng") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            CurrencyAmountTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Số tiền (VNĐ)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            val canSave = title.isNotBlank() && parsedAmount > 0L
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            title,
                            counterparty,
                            selectedType,
                            parsedAmount,
                            interestRateStr.toDoubleOrNull() ?: 0.0,
                            interestType,
                            parsedTerm,
                            parsedAmount / parsedTerm,
                            System.currentTimeMillis(),
                            dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 15,
                            note.ifBlank { null },
                            selectedAccountId,
                            "#1E88E5",
                            0L,
                            ConversionFeeSchedule.FIRST_MONTH,
                            0.0
                        )
                        onDismiss()
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Lưu khoản vay", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
