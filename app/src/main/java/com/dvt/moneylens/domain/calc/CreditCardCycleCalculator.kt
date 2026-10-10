package com.dvt.moneylens.domain.calc

import com.dvt.moneylens.domain.model.CreditCard
import com.dvt.moneylens.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CreditCardCycleInfo(
    val currentPeriodStart: LocalDate,
    val currentPeriodEnd: LocalDate, // Statement date
    val dueDate: LocalDate,
    val daysUntilDue: Long,
    val isDueSoon: Boolean, // <= 3 days
    val isOverdue: Boolean,
    val dueStatusText: String,
    val minPaymentRequired: Money
)

object CreditCardCycleCalculator {

    fun calculateCycle(
        card: CreditCard,
        today: LocalDate = LocalDate.now()
    ): CreditCardCycleInfo {
        val currentYearMonth = YearMonth.from(today)
        val statementDayInMonth = resolveDayInMonth(currentYearMonth, card.statementDay)
        val thisMonthStatementDate = currentYearMonth.atDay(statementDayInMonth)

        val statementDate = if (today.isAfter(thisMonthStatementDate)) {
            // Previous statement already closed, looking at upcoming close next month
            val nextMonth = currentYearMonth.plusMonths(1)
            nextMonth.atDay(resolveDayInMonth(nextMonth, card.statementDay))
        } else {
            thisMonthStatementDate
        }

        // Start of period is day after previous statement
        val prevMonth = YearMonth.from(statementDate).minusMonths(1)
        val prevStatementDate = prevMonth.atDay(resolveDayInMonth(prevMonth, card.statementDay))
        val periodStartDate = prevStatementDate.plusDays(1)

        // Due date calculation (e.g. 20 days after statement closing)
        val dueDate = statementDate.plusDays(card.dueDaysAfterStatement.toLong())

        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
        val isOverdue = daysUntilDue < 0
        val isDueSoon = daysUntilDue in 0..3

        val statusText = when {
            isOverdue -> "Quá hạn ${-daysUntilDue} ngày"
            daysUntilDue == 0L -> "Đến hạn hôm nay"
            daysUntilDue <= 3L -> "Còn $daysUntilDue ngày"
            daysUntilDue <= 15L -> "Còn $daysUntilDue ngày"
            else -> "Chưa đến hạn ($daysUntilDue ngày)"
        }

        // Calculate minimum payment (e.g. 5% of balance owed or floor)
        val calculatedMin = (card.currentBalanceOwedMinor * card.minPaymentPercent).toLong()
        val minPaymentMinor = calculatedMin.coerceAtLeast(card.minPaymentFloorMinor)
            .coerceAtMost(card.currentBalanceOwedMinor)

        return CreditCardCycleInfo(
            currentPeriodStart = periodStartDate,
            currentPeriodEnd = statementDate,
            dueDate = dueDate,
            daysUntilDue = daysUntilDue,
            isDueSoon = isDueSoon,
            isOverdue = isOverdue,
            dueStatusText = statusText,
            minPaymentRequired = Money(minPaymentMinor)
        )
    }

    private fun resolveDayInMonth(yearMonth: YearMonth, targetDay: Int): Int {
        val maxDay = yearMonth.lengthOfMonth()
        return targetDay.coerceIn(1, maxDay)
    }
}
