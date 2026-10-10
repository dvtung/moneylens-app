package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

object VietnameseCurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.GERMANY).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val integerFormatter = DecimalFormat("#,###", symbols)

    /**
     * Làm sạch chuỗi đầu vào thành chuỗi số nguyên/thập phân thuần (raw):
     * Chỉ chứa chữ số và tối đa 1 dấu phẩy (,) phân tách thập phân.
     * Loại bỏ các dấu chấm hàng nghìn cũ.
     */
    fun cleanToRaw(input: String, allowDecimals: Boolean = true): String {
        if (input.isBlank()) return ""

        val trimmed = input.trim()
        val hasComma = trimmed.contains(',')
        val hasDot = trimmed.contains('.')

        // Xác định xem người dùng có gõ dấu phân cách thập phân không
        // Nếu có dấu phẩy -> đó là dấu thập phân
        // Nếu không có dấu phẩy nhưng có đúng 1 dấu chấm VÀ chuỗi ngắn hoặc mới gõ dấu chấm ở cuối -> coi như dấu thập phân
        val decimalIndex = when {
            hasComma -> trimmed.indexOf(',')
            hasDot && trimmed.endsWith(".") -> trimmed.length - 1
            hasDot && trimmed.count { it == '.' } == 1 && (trimmed.length - trimmed.indexOf('.') <= 3) && !trimmed.contains(",") -> {
                // Trường hợp người dùng gõ từ bàn phím số có phím dấu chấm
                trimmed.indexOf('.')
            }
            else -> -1
        }

        if (decimalIndex != -1 && allowDecimals) {
            val intPart = trimmed.substring(0, decimalIndex).filter { it.isDigit() }
            val decPart = trimmed.substring(decimalIndex + 1).filter { it.isDigit() }.take(4)
            return if (trimmed.endsWith(",") || trimmed.endsWith(".")) {
                if (intPart.isEmpty()) "0," else "$intPart,"
            } else {
                if (intPart.isEmpty()) "0,$decPart" else "$intPart,$decPart"
            }
        }

        // Không có phần thập phân: chỉ giữ lại các chữ số
        return trimmed.filter { it.isDigit() }
    }

    /**
     * Định dạng chuỗi raw (chỉ chứa số và tối đa 1 dấu phẩy) thành chuỗi hiển thị chuẩn Việt Nam (1.000.000,5)
     */
    fun formatRaw(raw: String): String {
        if (raw.isEmpty()) return ""

        val commaIndex = raw.indexOf(',')
        if (commaIndex != -1) {
            val intPart = raw.substring(0, commaIndex)
            val decPart = raw.substring(commaIndex + 1)
            val formattedInt = if (intPart.isNotEmpty()) {
                intPart.toLongOrNull()?.let { integerFormatter.format(it) } ?: intPart
            } else "0"
            return if (decPart.isNotEmpty()) "$formattedInt,$decPart" else "$formattedInt,"
        } else {
            return raw.toLongOrNull()?.let { integerFormatter.format(it) } ?: raw
        }
    }

    /**
     * Định dạng chuỗi nhập liệu bất kỳ
     */
    fun formatInput(input: String, allowDecimals: Boolean = true): String {
        val raw = cleanToRaw(input, allowDecimals)
        return formatRaw(raw)
    }

    /**
     * Chuyển chuỗi định dạng (ví dụ "1.000.000,5" hoặc "1000000") thành giá trị Double
     */
    fun parseToDouble(formattedText: String): Double {
        if (formattedText.isBlank()) return 0.0
        val normalized = formattedText
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return normalized.toDoubleOrNull() ?: 0.0
    }

    /**
     * Chuyển chuỗi định dạng thành số nguyên Long (VND minor)
     */
    fun parseToMinor(formattedText: String): Long {
        val doubleVal = parseToDouble(formattedText)
        return doubleVal.roundToLong().coerceAtLeast(0L)
    }

    /**
     * Format một số Long sẵn có ra chuỗi hiển thị (ví dụ 1000000 -> "1.000.000")
     */
    fun formatMinor(amountMinor: Long): String {
        return integerFormatter.format(amountMinor)
    }
}

/**
 * VisualTransformation định dạng số tiền chuẩn Việt Nam theo thời gian thực (real-time)
 * Giữ nguyên vị trí con trỏ (Cursor) mượt mà và chính xác 100% khi người dùng gõ liên tục.
 */
class VietnameseCurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = VietnameseCurrencyFormatter.formatRaw(raw)

        // Xây dựng bảng ánh xạ vị trí con trỏ chính xác
        val rawToTrans = IntArray(raw.length + 1)
        val transToRaw = IntArray(formatted.length + 1)

        var r = 0
        var t = 0

        while (r < raw.length && t < formatted.length) {
            val rChar = raw[r]
            val tChar = formatted[t]
            if (rChar == tChar) {
                rawToTrans[r] = t
                transToRaw[t] = r
                r++
                t++
            } else if (tChar == '.') {
                transToRaw[t] = r
                t++
            } else {
                r++
                t++
            }
        }

        while (r <= raw.length) {
            rawToTrans[r] = formatted.length
            r++
        }
        while (t <= formatted.length) {
            transToRaw[t] = raw.length
            t++
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return rawToTrans[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToRaw[offset.coerceIn(0, formatted.length)]
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Text field nhập tiền tệ chuẩn Việt Nam có phân tách hàng nghìn (.) và thập phân (,)
 * Sử dụng VisualTransformation để đảm bảo trải nghiệm gõ số mượt mà, không bị nhảy con trỏ.
 */
@Composable
fun CurrencyAmountTextField(
    value: String,
    onValueChange: (raw: String) -> Unit,
    label: String = "Số tiền (VND)",
    placeholder: String = "0",
    modifier: Modifier = Modifier.fillMaxWidth(),
    allowDecimals: Boolean = true,
    suffix: String? = "₫",
    isError: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    supportingText: @Composable (() -> Unit)? = null
) {
    // Giá trị thô (chỉ số và tối đa 1 dấu phẩy)
    val rawValue = remember(value) {
        VietnameseCurrencyFormatter.cleanToRaw(value, allowDecimals)
    }

    OutlinedTextField(
        value = rawValue,
        onValueChange = { input ->
            val cleaned = VietnameseCurrencyFormatter.cleanToRaw(input, allowDecimals)
            onValueChange(cleaned)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = remember { VietnameseCurrencyVisualTransformation() },
        textStyle = textStyle,
        singleLine = true,
        suffix = if (suffix != null) {
            { Text(suffix, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        } else null,
        trailingIcon = if (rawValue.isNotBlank()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Xóa số tiền")
                }
            }
        } else null,
        supportingText = supportingText,
        isError = isError,
        modifier = modifier
    )
}
