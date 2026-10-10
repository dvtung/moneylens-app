package com.dvt.moneylens.ui.screens.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.screens.formatManagedSourceChipLabel
import com.dvt.moneylens.ui.theme.Spacing

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

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Tên gợi nhớ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isCredit) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                CurrencyAmountTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = "Hạn mức tín dụng (VNĐ)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text("Màu sắc nhận diện:", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                colors.forEach { hex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
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
                            Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            val parsedLimit = VietnameseCurrencyFormatter.parseToMinor(limitStr)
            val isValid = nickname.isNotBlank() && (!isCredit || parsedLimit > 0)

            Button(
                onClick = {
                    if (isValid) {
                        val updated = card.copy(
                            nickname = nickname,
                            creditLimitMinor = if (isCredit) parsedLimit else 0L,
                            colorHex = selectedColor
                        )
                        onSave(updated)
                        onDismiss()
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Cập nhật thông tin", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
