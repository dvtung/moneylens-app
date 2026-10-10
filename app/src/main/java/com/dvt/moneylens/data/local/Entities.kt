package com.dvt.moneylens.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dvt.moneylens.domain.model.*

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val balanceMinor: Long,
    val bankCode: String?,
    val currency: String,
    val ownerMemberId: String?,
    val isArchived: Boolean
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.BANK),
        balanceMinor = balanceMinor,
        bankCode = bankCode,
        currency = currency,
        ownerMemberId = ownerMemberId,
        isArchived = isArchived
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type.name,
            balanceMinor = account.balanceMinor,
            bankCode = account.bankCode,
            currency = account.currency,
            ownerMemberId = account.ownerMemberId,
            isArchived = account.isArchived
        )
    }
}

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val nickname: String,
    val issuerBank: String,
    val network: String,
    val last4: String,
    val creditLimitMinor: Long,
    val statementDay: Int,
    val dueDaysAfterStatement: Int,
    val minPaymentPercent: Double,
    val minPaymentFloorMinor: Long,
    val currentBalanceOwedMinor: Long,
    val annualFeeMinor: Long,
    val colorHex: String,
    val ownerMemberId: String?,
    val cardType: String = "CREDIT"
) {
    fun toDomain(): CreditCard = CreditCard(
        id = id,
        accountId = accountId,
        nickname = nickname,
        issuerBank = issuerBank,
        network = runCatching { CardNetwork.valueOf(network) }.getOrDefault(CardNetwork.VISA),
        last4 = last4,
        creditLimitMinor = creditLimitMinor,
        statementDay = statementDay,
        dueDaysAfterStatement = dueDaysAfterStatement,
        minPaymentPercent = minPaymentPercent,
        minPaymentFloorMinor = minPaymentFloorMinor,
        currentBalanceOwedMinor = currentBalanceOwedMinor,
        annualFeeMinor = annualFeeMinor,
        colorHex = colorHex,
        ownerMemberId = ownerMemberId,
        cardType = runCatching { CardType.valueOf(cardType) }.getOrDefault(CardType.CREDIT)
    )

    companion object {
        fun fromDomain(card: CreditCard): CreditCardEntity = CreditCardEntity(
            id = card.id,
            accountId = card.accountId,
            nickname = card.nickname,
            issuerBank = card.issuerBank,
            network = card.network.name,
            last4 = card.last4,
            creditLimitMinor = card.creditLimitMinor,
            statementDay = card.statementDay,
            dueDaysAfterStatement = card.dueDaysAfterStatement,
            minPaymentPercent = card.minPaymentPercent,
            minPaymentFloorMinor = card.minPaymentFloorMinor,
            currentBalanceOwedMinor = card.currentBalanceOwedMinor,
            annualFeeMinor = card.annualFeeMinor,
            colorHex = card.colorHex,
            ownerMemberId = card.ownerMemberId,
            cardType = card.cardType.name
        )
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amountMinor: Long,
    val type: String,
    val categoryId: String?,
    val note: String,
    val counterparty: String?,
    val status: String,
    val occurredAt: Long,
    val balanceAfterMinor: Long?,
    val source: String,
    val dedupKey: String?,
    val memberId: String?,
    val paymentSource: String = "DEBIT",
    val creditCardId: String? = null
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        accountId = accountId,
        amountMinor = amountMinor,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        categoryId = categoryId,
        note = note,
        counterparty = counterparty,
        status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.CONFIRMED),
        occurredAt = occurredAt,
        balanceAfterMinor = balanceAfterMinor,
        source = source,
        dedupKey = dedupKey,
        memberId = memberId,
        paymentSource = runCatching { PaymentSourceType.valueOf(paymentSource) }.getOrDefault(PaymentSourceType.DEBIT),
        creditCardId = creditCardId
    )

    companion object {
        fun fromDomain(tx: Transaction): TransactionEntity = TransactionEntity(
            id = tx.id,
            accountId = tx.accountId,
            amountMinor = tx.amountMinor,
            type = tx.type.name,
            categoryId = tx.categoryId,
            note = tx.note,
            counterparty = tx.counterparty,
            status = tx.status.name,
            occurredAt = tx.occurredAt,
            balanceAfterMinor = tx.balanceAfterMinor,
            source = tx.source,
            dedupKey = tx.dedupKey,
            memberId = tx.memberId,
            paymentSource = tx.paymentSource.name,
            creditCardId = tx.creditCardId
        )
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: String
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
    )

    companion object {
        fun fromDomain(cat: Category): CategoryEntity = CategoryEntity(
            id = cat.id,
            name = cat.name,
            icon = cat.icon,
            colorHex = cat.colorHex,
            type = cat.type.name
        )
    }
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val limitMinor: Long,
    val periodMonth: String
) {
    fun toDomain(): Budget = Budget(
        id = id,
        categoryId = categoryId,
        limitMinor = limitMinor,
        periodMonth = periodMonth
    )

    companion object {
        fun fromDomain(b: Budget): BudgetEntity = BudgetEntity(
            id = b.id,
            categoryId = b.categoryId,
            limitMinor = b.limitMinor,
            periodMonth = b.periodMonth
        )
    }
}

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val currentPriceMinor: Long,
    val costBasisMinor: Long,
    val notes: String?,
    val lastUpdated: Long
) {
    fun toDomain(): Asset = Asset(
        id = id,
        type = runCatching { AssetType.valueOf(type) }.getOrDefault(AssetType.STOCK),
        symbol = symbol,
        name = name,
        quantity = quantity,
        currentPriceMinor = currentPriceMinor,
        costBasisMinor = costBasisMinor,
        notes = notes,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomain(a: Asset): AssetEntity = AssetEntity(
            id = a.id,
            type = a.type.name,
            symbol = a.symbol,
            name = a.name,
            quantity = a.quantity,
            currentPriceMinor = a.currentPriceMinor,
            costBasisMinor = a.costBasisMinor,
            notes = a.notes,
            lastUpdated = a.lastUpdated
        )
    }
}

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val role: String,
    val colorHex: String
) {
    fun toDomain(): Member = Member(
        id = id,
        displayName = displayName,
        role = runCatching { MemberRole.valueOf(role) }.getOrDefault(MemberRole.SELF),
        colorHex = colorHex
    )

    companion object {
        fun fromDomain(m: Member): MemberEntity = MemberEntity(
            id = m.id,
            displayName = m.displayName,
            role = m.role.name,
            colorHex = m.colorHex
        )
    }
}

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val counterparty: String,
    val type: String,
    val originalAmountMinor: Long,
    val remainingPrincipalMinor: Long,
    val interestRate: Double,
    val interestType: String,
    val termMonths: Int,
    val monthlyPaymentMinor: Long,
    val startDate: Long,
    val dueDayOfMonth: Int,
    val status: String,
    val notes: String?,
    val linkedAccountId: String?,
    val colorHex: String,
    val conversionFeeMinor: Long = 0L,
    val conversionFeeSchedule: String = "FIRST_MONTH",
    val conversionFeePercent: Double = 0.0
) {
    fun toDomain(): Loan = Loan(
        id = id,
        title = title,
        counterparty = counterparty,
        type = runCatching { LoanType.valueOf(type) }.getOrDefault(LoanType.BORROWING),
        originalAmountMinor = originalAmountMinor,
        remainingPrincipalMinor = remainingPrincipalMinor,
        interestRate = interestRate,
        interestType = runCatching { InterestType.valueOf(interestType) }.getOrDefault(InterestType.PERCENT_YEAR),
        termMonths = termMonths,
        monthlyPaymentMinor = monthlyPaymentMinor,
        startDate = startDate,
        dueDayOfMonth = dueDayOfMonth,
        status = runCatching { LoanStatus.valueOf(status) }.getOrDefault(LoanStatus.ACTIVE),
        notes = notes,
        linkedAccountId = linkedAccountId,
        colorHex = colorHex,
        conversionFeeMinor = conversionFeeMinor,
        conversionFeeSchedule = runCatching { ConversionFeeSchedule.valueOf(conversionFeeSchedule) }.getOrDefault(ConversionFeeSchedule.FIRST_MONTH),
        conversionFeePercent = conversionFeePercent
    )

    companion object {
        fun fromDomain(l: Loan): LoanEntity = LoanEntity(
            id = l.id,
            title = l.title,
            counterparty = l.counterparty,
            type = l.type.name,
            originalAmountMinor = l.originalAmountMinor,
            remainingPrincipalMinor = l.remainingPrincipalMinor,
            interestRate = l.interestRate,
            interestType = l.interestType.name,
            termMonths = l.termMonths,
            monthlyPaymentMinor = l.monthlyPaymentMinor,
            startDate = l.startDate,
            dueDayOfMonth = l.dueDayOfMonth,
            status = l.status.name,
            notes = l.notes,
            linkedAccountId = l.linkedAccountId,
            colorHex = l.colorHex,
            conversionFeeMinor = l.conversionFeeMinor,
            conversionFeeSchedule = l.conversionFeeSchedule.name,
            conversionFeePercent = l.conversionFeePercent
        )
    }
}

@Entity(tableName = "loan_payments")
data class LoanPaymentEntity(
    @PrimaryKey val id: String,
    val loanId: String,
    val amountMinor: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val paidAt: Long,
    val note: String
) {
    fun toDomain(): LoanPayment = LoanPayment(
        id = id,
        loanId = loanId,
        amountMinor = amountMinor,
        principalMinor = principalMinor,
        interestMinor = interestMinor,
        paidAt = paidAt,
        note = note
    )

    companion object {
        fun fromDomain(lp: LoanPayment): LoanPaymentEntity = LoanPaymentEntity(
            id = lp.id,
            loanId = lp.loanId,
            amountMinor = lp.amountMinor,
            principalMinor = lp.principalMinor,
            interestMinor = lp.interestMinor,
            paidAt = lp.paidAt,
            note = lp.note
        )
    }
}

@Entity(tableName = "investment_transactions")
data class InvestmentTransactionEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val symbol: String,
    val action: String, // BUY, SELL, DIVIDEND, VALUATION
    val quantity: Double,
    val priceMinor: Long,
    val totalAmountMinor: Long,
    val accountId: String?,
    val accountName: String?,
    val note: String,
    val feeAndTaxMinor: Long = 0L,
    val timestamp: Long
) {
    fun toDomain(): InvestmentTransaction = InvestmentTransaction(
        id = id,
        assetId = assetId,
        symbol = symbol,
        action = runCatching { InvestmentAction.valueOf(action) }.getOrDefault(InvestmentAction.BUY),
        quantity = quantity,
        priceMinor = priceMinor,
        totalAmountMinor = totalAmountMinor,
        accountId = accountId,
        accountName = accountName,
        note = note,
        feeAndTaxMinor = feeAndTaxMinor,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(itx: InvestmentTransaction): InvestmentTransactionEntity = InvestmentTransactionEntity(
            id = itx.id,
            assetId = itx.assetId,
            symbol = itx.symbol,
            action = itx.action.name,
            quantity = itx.quantity,
            priceMinor = itx.priceMinor,
            totalAmountMinor = itx.totalAmountMinor,
            accountId = itx.accountId,
            accountName = itx.accountName,
            note = itx.note,
            feeAndTaxMinor = itx.feeAndTaxMinor,
            timestamp = itx.timestamp
        )
    }
}

