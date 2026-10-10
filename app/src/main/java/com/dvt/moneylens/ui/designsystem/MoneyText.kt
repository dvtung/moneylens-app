package com.dvt.moneylens.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.MoneyRow

@Composable
fun MoneyText(
    amountMinor: Long,
    type: TransactionType = TransactionType.EXPENSE,
    modifier: Modifier = Modifier,
    style: TextStyle = MoneyRow,
    masked: Boolean = false,
    overrideColor: Color? = null
) {
    val lensColors = LocalMoneyLensColors.current

    if (masked) {
        Text(
            text = "••••••",
            style = style,
            color = overrideColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = modifier.semantics {
                contentDescription = "Số tiền được ẩn"
            }
        )
        return
    }

    val (prefix, defaultColor, typeDescription) = when (type) {
        TransactionType.INCOME -> Triple("+", lensColors.income, "thu")
        TransactionType.EXPENSE -> Triple("−", lensColors.expense, "chi")
        TransactionType.TRANSFER -> Triple("", lensColors.transfer, "chuyển khoản")
    }

    val formattedNumber = Money(amountMinor).formatVnd(masked = false, absValue = prefix.isNotEmpty())
    val displayText = "$prefix$formattedNumber"

    Text(
        text = displayText,
        style = style,
        color = overrideColor ?: defaultColor,
        modifier = modifier.semantics {
            contentDescription = "$typeDescription $formattedNumber"
        }
    )
}
