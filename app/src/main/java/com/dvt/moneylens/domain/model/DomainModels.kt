package com.dvt.moneylens.domain.model

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balanceMinor: Long,
    val bankCode: String? = null,
    val currency: String = "VND",
    val ownerMemberId: String? = null,
    val isArchived: Boolean = false
) {
    val balance: Money get() = Money(balanceMinor)
}

data class CreditCard(
    val id: String,
    val accountId: String,
    val nickname: String,
    val issuerBank: String,
    val network: CardNetwork,
    val last4: String, // ONLY 4 digits, never store full PAN/CVV
    val creditLimitMinor: Long,
    val statementDay: Int, // 1..31
    val dueDaysAfterStatement: Int = 20, // e.g. 15 or 20 days
    val minPaymentPercent: Double = 0.05, // 5%
    val minPaymentFloorMinor: Long = 100_000L,
    val currentBalanceOwedMinor: Long,
    val annualFeeMinor: Long = 0L,
    val colorHex: String = "#0A6B5C",
    val ownerMemberId: String? = null,
    val cardType: CardType = CardType.CREDIT
) {
    val isCredit: Boolean get() = cardType == CardType.CREDIT
    val isDebit: Boolean get() = cardType == CardType.DEBIT
    val isWallet: Boolean get() = cardType == CardType.WALLET
    val isBankAccount: Boolean get() = cardType == CardType.BANK_ACCOUNT
    val creditLimit: Money get() = Money(creditLimitMinor)
    val balanceOwed: Money get() = Money(currentBalanceOwedMinor)
    val availableLimit: Money get() = Money((creditLimitMinor - currentBalanceOwedMinor).coerceAtLeast(0L))
    val utilizationRate: Double get() = if (creditLimitMinor > 0) currentBalanceOwedMinor.toDouble() / creditLimitMinor else 0.0
}

data class Transaction(
    val id: String,
    val accountId: String,
    val amountMinor: Long,
    val type: TransactionType,
    val categoryId: String? = null,
    val note: String = "",
    val counterparty: String? = null,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val occurredAt: Long = System.currentTimeMillis(),
    val balanceAfterMinor: Long? = null,
    val source: String = "MANUAL", // SMS, NOTIFICATION, MANUAL
    val dedupKey: String? = null,
    val memberId: String? = null,
    val paymentSource: PaymentSourceType = PaymentSourceType.DEBIT,
    val creditCardId: String? = null
) {
    val amount: Money get() = Money(amountMinor)
    val isCredit: Boolean get() = paymentSource == PaymentSourceType.CREDIT
}

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: TransactionType
)

data class Budget(
    val id: String,
    val categoryId: String,
    val limitMinor: Long,
    val periodMonth: String // "2026-10"
) {
    val limit: Money get() = Money(limitMinor)
}

data class Asset(
    val id: String,
    val type: AssetType,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val currentPriceMinor: Long,
    val costBasisMinor: Long,
    val notes: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val currentTotalValueMinor: Long get() = (quantity * currentPriceMinor).toLong()
    val totalCostBasisMinor: Long get() = (quantity * costBasisMinor).toLong()
    val unrealizedPnlMinor: Long get() = currentTotalValueMinor - totalCostBasisMinor
    val pnlPercentage: Double get() = if (totalCostBasisMinor > 0) (unrealizedPnlMinor.toDouble() / totalCostBasisMinor) * 100.0 else 0.0
    val currentTotalValue: Money get() = Money(currentTotalValueMinor)
    val unrealizedPnl: Money get() = Money(unrealizedPnlMinor)
}

data class Member(
    val id: String,
    val displayName: String,
    val role: MemberRole,
    val colorHex: String
)

data class Loan(
    val id: String,
    val title: String,
    val counterparty: String,
    val type: LoanType,
    val originalAmountMinor: Long,
    val remainingPrincipalMinor: Long,
    val interestRate: Double = 0.0,
    val interestType: InterestType = InterestType.PERCENT_YEAR,
    val termMonths: Int = 12,
    val monthlyPaymentMinor: Long = 0L,
    val startDate: Long = System.currentTimeMillis(),
    val dueDayOfMonth: Int = 15,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val notes: String? = null,
    val linkedAccountId: String? = null,
    val colorHex: String = "#1E88E5",
    val conversionFeeMinor: Long = 0L,
    val conversionFeeSchedule: ConversionFeeSchedule = ConversionFeeSchedule.FIRST_MONTH,
    val conversionFeePercent: Double = 0.0
) {
    val isPaidOff: Boolean get() = remainingPrincipalMinor <= 0L || status == LoanStatus.PAID_OFF
    val paidAmountMinor: Long get() = (originalAmountMinor - remainingPrincipalMinor).coerceAtLeast(0L)
    val progressPercent: Double get() = if (originalAmountMinor > 0) ((originalAmountMinor - remainingPrincipalMinor).toDouble() / originalAmountMinor) * 100.0 else 100.0
    val originalAmount: Money get() = Money(originalAmountMinor)
    val remainingPrincipal: Money get() = Money(remainingPrincipalMinor)
    val monthlyPayment: Money get() = Money(monthlyPaymentMinor)
    val conversionFee: Money get() = Money(conversionFeeMinor)

    // Helper calculations for installment fee schedule
    val firstMonthPaymentMinor: Long get() {
        return if (conversionFeeSchedule == ConversionFeeSchedule.FIRST_MONTH) {
            monthlyPaymentMinor + conversionFeeMinor
        } else {
            val monthlyFee = if (termMonths > 0) conversionFeeMinor / termMonths else 0L
            monthlyPaymentMinor + monthlyFee
        }
    }

    val regularMonthlyPaymentMinor: Long get() {
        return if (conversionFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY) {
            val monthlyFee = if (termMonths > 0) conversionFeeMinor / termMonths else 0L
            monthlyPaymentMinor + monthlyFee
        } else {
            monthlyPaymentMinor
        }
    }

    val totalLoanCostMinor: Long get() = originalAmountMinor + conversionFeeMinor
}

data class LoanPayment(
    val id: String,
    val loanId: String,
    val amountMinor: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val paidAt: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    val amount: Money get() = Money(amountMinor)
    val principal: Money get() = Money(principalMinor)
    val interest: Money get() = Money(interestMinor)
}

data class InvestmentTransaction(
    val id: String,
    val assetId: String,
    val symbol: String,
    val action: InvestmentAction,
    val quantity: Double,
    val priceMinor: Long,
    val totalAmountMinor: Long,
    val accountId: String? = null,
    val accountName: String? = null,
    val note: String = "",
    val feeAndTaxMinor: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalAmount: Money get() = Money(totalAmountMinor)
    val price: Money get() = Money(priceMinor)
}
