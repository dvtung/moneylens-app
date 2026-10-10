package com.dvt.moneylens.ui.screens.modals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentBottomSheet(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (
        type: AssetType,
        symbol: String,
        name: String,
        quantity: Double,
        priceMinor: Long,
        action: InvestmentAction,
        note: String,
        accountId: String?
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(AssetType.STOCK) }
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var priceStr by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(InvestmentAction.BUY) }
    var note by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    val parsedPrice = VietnameseCurrencyFormatter.parseToMinor(priceStr)
    val parsedQuantity = quantityStr.toDoubleOrNull() ?: 1.0

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
            Text(
                text = "Thêm tài sản đầu tư",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase() },
                label = { Text("Mã tài sản (VD: FPT, SJC, BTC)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên gọi đầy đủ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Số lượng") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                CurrencyAmountTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = "Đơn giá (VNĐ)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            val canSave = symbol.isNotBlank() && parsedQuantity > 0 && parsedPrice > 0L
            Button(
                onClick = {
                    if (canSave) {
                        onSave(selectedType, symbol, name, parsedQuantity, parsedPrice, action, note, selectedAccountId)
                        onDismiss()
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Lưu tài sản", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
