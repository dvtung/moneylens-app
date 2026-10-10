package com.dvt.moneylens.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@JvmInline
value class Money(val minor: Long) {
    fun formatVnd(masked: Boolean = false): String {
        if (masked) return "••••••"
        val symbols = DecimalFormatSymbols(Locale.GERMANY).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(minor)
        return "$formattedNumber ₫"
    }

    operator fun plus(other: Money): Money = Money(this.minor + other.minor)
    operator fun minus(other: Money): Money = Money(this.minor - other.minor)
    operator fun times(factor: Long): Money = Money(this.minor * factor)
    operator fun compareTo(other: Money): Int = this.minor.compareTo(other.minor)

    companion object {
        val ZERO = Money(0L)
    }
}
