package com.dvt.moneylens.data.backup

import android.os.Build
import com.dvt.moneylens.data.local.*
import org.json.JSONArray
import org.json.JSONObject

data class DataBackupDto(
    val schemaVersion: Int = 6,
    val appVersion: String = "1.0.0",
    val appName: String = "MoneyLens",
    val exportedAt: Long = System.currentTimeMillis(),
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val accounts: List<AccountEntity>,
    val creditCards: List<CreditCardEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val assets: List<AssetEntity>,
    val members: List<MemberEntity>,
    val loans: List<LoanEntity>,
    val loanPayments: List<LoanPaymentEntity>,
    val investmentTransactions: List<InvestmentTransactionEntity> = emptyList()
)

data class BackupSummary(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val deviceModel: String,
    val accountCount: Int,
    val creditCardCount: Int,
    val transactionCount: Int,
    val categoryCount: Int,
    val budgetCount: Int,
    val assetCount: Int,
    val memberCount: Int,
    val loanCount: Int,
    val loanPaymentCount: Int,
    val investmentTransactionCount: Int = 0
) {
    val totalRecordCount: Int
        get() = accountCount + creditCardCount + transactionCount + categoryCount + budgetCount + assetCount + memberCount + loanCount + loanPaymentCount + investmentTransactionCount
}

object DataBackupManager {

    const val CURRENT_SCHEMA_VERSION = 6

    fun toJson(dto: DataBackupDto): String {
        val root = JSONObject().apply {
            put("schemaVersion", dto.schemaVersion)
            put("appVersion", dto.appVersion)
            put("appName", dto.appName)
            put("exportedAt", dto.exportedAt)
            put("deviceModel", dto.deviceModel)

            // 1. Accounts
            put("accounts", JSONArray().apply {
                dto.accounts.forEach { acc ->
                    put(JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("type", acc.type)
                        put("balanceMinor", acc.balanceMinor)
                        acc.bankCode?.let { put("bankCode", it) }
                        put("currency", acc.currency)
                        acc.ownerMemberId?.let { put("ownerMemberId", it) }
                        put("isArchived", acc.isArchived)
                    })
                }
            })

            // 2. Credit Cards
            put("creditCards", JSONArray().apply {
                dto.creditCards.forEach { card ->
                    put(JSONObject().apply {
                        put("id", card.id)
                        put("accountId", card.accountId)
                        put("nickname", card.nickname)
                        put("issuerBank", card.issuerBank)
                        put("network", card.network)
                        put("last4", card.last4)
                        put("creditLimitMinor", card.creditLimitMinor)
                        put("statementDay", card.statementDay)
                        put("dueDaysAfterStatement", card.dueDaysAfterStatement)
                        put("minPaymentPercent", card.minPaymentPercent)
                        put("minPaymentFloorMinor", card.minPaymentFloorMinor)
                        put("currentBalanceOwedMinor", card.currentBalanceOwedMinor)
                        put("annualFeeMinor", card.annualFeeMinor)
                        put("colorHex", card.colorHex)
                        card.ownerMemberId?.let { put("ownerMemberId", it) }
                        put("cardType", card.cardType)
                    })
                }
            })

            // 3. Categories
            put("categories", JSONArray().apply {
                dto.categories.forEach { cat ->
                    put(JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("icon", cat.icon)
                        put("colorHex", cat.colorHex)
                        put("type", cat.type)
                    })
                }
            })

            // 4. Transactions
            put("transactions", JSONArray().apply {
                dto.transactions.forEach { tx ->
                    put(JSONObject().apply {
                        put("id", tx.id)
                        put("accountId", tx.accountId)
                        put("amountMinor", tx.amountMinor)
                        put("type", tx.type)
                        tx.categoryId?.let { put("categoryId", it) }
                        put("note", tx.note)
                        tx.counterparty?.let { put("counterparty", it) }
                        put("status", tx.status)
                        put("occurredAt", tx.occurredAt)
                        tx.balanceAfterMinor?.let { put("balanceAfterMinor", it) }
                        put("source", tx.source)
                        tx.dedupKey?.let { put("dedupKey", it) }
                        tx.memberId?.let { put("memberId", it) }
                        put("paymentSource", tx.paymentSource)
                        tx.creditCardId?.let { put("creditCardId", it) }
                    })
                }
            })

            // 5. Budgets
            put("budgets", JSONArray().apply {
                dto.budgets.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("categoryId", b.categoryId)
                        put("limitMinor", b.limitMinor)
                        put("periodMonth", b.periodMonth)
                    })
                }
            })

            // 6. Assets
            put("assets", JSONArray().apply {
                dto.assets.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("type", a.type)
                        put("symbol", a.symbol)
                        put("name", a.name)
                        put("quantity", a.quantity)
                        put("currentPriceMinor", a.currentPriceMinor)
                        put("costBasisMinor", a.costBasisMinor)
                        a.notes?.let { put("notes", it) }
                        put("lastUpdated", a.lastUpdated)
                    })
                }
            })

            // 7. Members
            put("members", JSONArray().apply {
                dto.members.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("displayName", m.displayName)
                        put("role", m.role)
                        put("colorHex", m.colorHex)
                    })
                }
            })

            // 8. Loans
            put("loans", JSONArray().apply {
                dto.loans.forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id)
                        put("title", l.title)
                        put("counterparty", l.counterparty)
                        put("type", l.type)
                        put("originalAmountMinor", l.originalAmountMinor)
                        put("remainingPrincipalMinor", l.remainingPrincipalMinor)
                        put("interestRate", l.interestRate)
                        put("interestType", l.interestType)
                        put("termMonths", l.termMonths)
                        put("monthlyPaymentMinor", l.monthlyPaymentMinor)
                        put("startDate", l.startDate)
                        put("dueDayOfMonth", l.dueDayOfMonth)
                        put("status", l.status)
                        l.notes?.let { put("notes", it) }
                        l.linkedAccountId?.let { put("linkedAccountId", it) }
                        put("colorHex", l.colorHex)
                        put("conversionFeeMinor", l.conversionFeeMinor)
                        put("conversionFeeSchedule", l.conversionFeeSchedule)
                        put("conversionFeePercent", l.conversionFeePercent)
                    })
                }
            })

            // 9. Loan Payments
            put("loanPayments", JSONArray().apply {
                dto.loanPayments.forEach { lp ->
                    put(JSONObject().apply {
                        put("id", lp.id)
                        put("loanId", lp.loanId)
                        put("amountMinor", lp.amountMinor)
                        put("principalMinor", lp.principalMinor)
                        put("interestMinor", lp.interestMinor)
                        put("paidAt", lp.paidAt)
                        put("note", lp.note)
                    })
                }
            })

            // 10. Investment Transactions
            put("investmentTransactions", JSONArray().apply {
                dto.investmentTransactions.forEach { itx ->
                    put(JSONObject().apply {
                        put("id", itx.id)
                        put("assetId", itx.assetId)
                        put("symbol", itx.symbol)
                        put("action", itx.action)
                        put("quantity", itx.quantity)
                        put("priceMinor", itx.priceMinor)
                        put("totalAmountMinor", itx.totalAmountMinor)
                        itx.accountId?.let { put("accountId", it) }
                        itx.accountName?.let { put("accountName", it) }
                        put("note", itx.note)
                        put("feeAndTaxMinor", itx.feeAndTaxMinor)
                        put("timestamp", itx.timestamp)
                    })
                }
            })
        }

        return root.toString(2)
    }

    fun fromJson(jsonString: String): Result<DataBackupDto> = runCatching {
        val root = JSONObject(jsonString)
        val schemaVersion = root.optInt("schemaVersion", CURRENT_SCHEMA_VERSION)
        val appVersion = root.optString("appVersion", "1.0.0")
        val appName = root.optString("appName", "MoneyLens")
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val deviceModel = root.optString("deviceModel", "Thiết bị không xác định")

        // 1. Accounts
        val accounts = mutableListOf<AccountEntity>()
        root.optJSONArray("accounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                accounts.add(
                    AccountEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        name = o.optString("name", "Tài khoản"),
                        type = o.optString("type", "BANK"),
                        balanceMinor = o.optLong("balanceMinor", 0L),
                        bankCode = if (o.isNull("bankCode") || !o.has("bankCode")) null else o.optString("bankCode"),
                        currency = o.optString("currency", "VND"),
                        ownerMemberId = if (o.isNull("ownerMemberId") || !o.has("ownerMemberId")) null else o.optString("ownerMemberId"),
                        isArchived = o.optBoolean("isArchived", false)
                    )
                )
            }
        }

        // 2. Credit Cards
        val creditCards = mutableListOf<CreditCardEntity>()
        root.optJSONArray("creditCards")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                creditCards.add(
                    CreditCardEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        accountId = o.optString("accountId", ""),
                        nickname = o.optString("nickname", "Thẻ"),
                        issuerBank = o.optString("issuerBank", "BANK"),
                        network = o.optString("network", "VISA"),
                        last4 = o.optString("last4", "0000"),
                        creditLimitMinor = o.optLong("creditLimitMinor", 0L),
                        statementDay = o.optInt("statementDay", 20),
                        dueDaysAfterStatement = o.optInt("dueDaysAfterStatement", 15),
                        minPaymentPercent = o.optDouble("minPaymentPercent", 5.0),
                        minPaymentFloorMinor = o.optLong("minPaymentFloorMinor", 50000L),
                        currentBalanceOwedMinor = o.optLong("currentBalanceOwedMinor", 0L),
                        annualFeeMinor = o.optLong("annualFeeMinor", 0L),
                        colorHex = o.optString("colorHex", "#1A73E8"),
                        ownerMemberId = if (o.isNull("ownerMemberId") || !o.has("ownerMemberId")) null else o.optString("ownerMemberId"),
                        cardType = o.optString("cardType", "CREDIT")
                    )
                )
            }
        }

        // 3. Categories
        val categories = mutableListOf<CategoryEntity>()
        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                categories.add(
                    CategoryEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        name = o.optString("name", "Danh mục"),
                        icon = o.optString("icon", "category"),
                        colorHex = o.optString("colorHex", "#1A73E8"),
                        type = o.optString("type", "EXPENSE")
                    )
                )
            }
        }

        // 4. Transactions
        val transactions = mutableListOf<TransactionEntity>()
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                transactions.add(
                    TransactionEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        accountId = o.optString("accountId", ""),
                        amountMinor = o.optLong("amountMinor", 0L),
                        type = o.optString("type", "EXPENSE"),
                        categoryId = if (o.isNull("categoryId") || !o.has("categoryId")) null else o.optString("categoryId"),
                        note = o.optString("note", ""),
                        counterparty = if (o.isNull("counterparty") || !o.has("counterparty")) null else o.optString("counterparty"),
                        status = o.optString("status", "CONFIRMED"),
                        occurredAt = o.optLong("occurredAt", System.currentTimeMillis()),
                        balanceAfterMinor = if (o.isNull("balanceAfterMinor") || !o.has("balanceAfterMinor")) null else o.optLong("balanceAfterMinor"),
                        source = o.optString("source", "MANUAL"),
                        dedupKey = if (o.isNull("dedupKey") || !o.has("dedupKey")) null else o.optString("dedupKey"),
                        memberId = if (o.isNull("memberId") || !o.has("memberId")) null else o.optString("memberId"),
                        paymentSource = o.optString("paymentSource", "DEBIT"),
                        creditCardId = if (o.isNull("creditCardId") || !o.has("creditCardId")) null else o.optString("creditCardId")
                    )
                )
            }
        }

        // 5. Budgets
        val budgets = mutableListOf<BudgetEntity>()
        root.optJSONArray("budgets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                budgets.add(
                    BudgetEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        categoryId = o.optString("categoryId", ""),
                        limitMinor = o.optLong("limitMinor", 0L),
                        periodMonth = o.optString("periodMonth", "")
                    )
                )
            }
        }

        // 6. Assets
        val assets = mutableListOf<AssetEntity>()
        root.optJSONArray("assets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                assets.add(
                    AssetEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        type = o.optString("type", "STOCK"),
                        symbol = o.optString("symbol", ""),
                        name = o.optString("name", ""),
                        quantity = o.optDouble("quantity", 0.0),
                        currentPriceMinor = o.optLong("currentPriceMinor", 0L),
                        costBasisMinor = o.optLong("costBasisMinor", 0L),
                        notes = if (o.isNull("notes") || !o.has("notes")) null else o.optString("notes"),
                        lastUpdated = o.optLong("lastUpdated", System.currentTimeMillis())
                    )
                )
            }
        }

        // 7. Members
        val members = mutableListOf<MemberEntity>()
        root.optJSONArray("members")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                members.add(
                    MemberEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        displayName = o.optString("displayName", "Thành viên"),
                        role = o.optString("role", "SELF"),
                        colorHex = o.optString("colorHex", "#1A73E8")
                    )
                )
            }
        }

        // 8. Loans
        val loans = mutableListOf<LoanEntity>()
        root.optJSONArray("loans")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                loans.add(
                    LoanEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        title = o.optString("title", "Khoản vay"),
                        counterparty = o.optString("counterparty", ""),
                        type = o.optString("type", "BORROWING"),
                        originalAmountMinor = o.optLong("originalAmountMinor", 0L),
                        remainingPrincipalMinor = o.optLong("remainingPrincipalMinor", 0L),
                        interestRate = o.optDouble("interestRate", 0.0),
                        interestType = o.optString("interestType", "PERCENT_YEAR"),
                        termMonths = o.optInt("termMonths", 12),
                        monthlyPaymentMinor = o.optLong("monthlyPaymentMinor", 0L),
                        startDate = o.optLong("startDate", System.currentTimeMillis()),
                        dueDayOfMonth = o.optInt("dueDayOfMonth", 1),
                        status = o.optString("status", "ACTIVE"),
                        notes = if (o.isNull("notes") || !o.has("notes")) null else o.optString("notes"),
                        linkedAccountId = if (o.isNull("linkedAccountId") || !o.has("linkedAccountId")) null else o.optString("linkedAccountId"),
                        colorHex = o.optString("colorHex", "#E53935"),
                        conversionFeeMinor = o.optLong("conversionFeeMinor", 0L),
                        conversionFeeSchedule = o.optString("conversionFeeSchedule", "FIRST_MONTH"),
                        conversionFeePercent = o.optDouble("conversionFeePercent", 0.0)
                    )
                )
            }
        }

        // 9. Loan Payments
        val loanPayments = mutableListOf<LoanPaymentEntity>()
        root.optJSONArray("loanPayments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                loanPayments.add(
                    LoanPaymentEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        loanId = o.optString("loanId", ""),
                        amountMinor = o.optLong("amountMinor", 0L),
                        principalMinor = o.optLong("principalMinor", 0L),
                        interestMinor = o.optLong("interestMinor", 0L),
                        paidAt = o.optLong("paidAt", System.currentTimeMillis()),
                        note = o.optString("note", "")
                    )
                )
            }
        }

        // 10. Investment Transactions
        val investmentTransactions = mutableListOf<InvestmentTransactionEntity>()
        root.optJSONArray("investmentTransactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                investmentTransactions.add(
                    InvestmentTransactionEntity(
                        id = o.optString("id", java.util.UUID.randomUUID().toString()),
                        assetId = o.optString("assetId", ""),
                        symbol = o.optString("symbol", ""),
                        action = o.optString("action", "BUY"),
                        quantity = o.optDouble("quantity", 0.0),
                        priceMinor = o.optLong("priceMinor", 0L),
                        totalAmountMinor = o.optLong("totalAmountMinor", 0L),
                        accountId = if (o.isNull("accountId") || !o.has("accountId")) null else o.optString("accountId"),
                        accountName = if (o.isNull("accountName") || !o.has("accountName")) null else o.optString("accountName"),
                        note = o.optString("note", ""),
                        feeAndTaxMinor = o.optLong("feeAndTaxMinor", 0L),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        DataBackupDto(
            schemaVersion = schemaVersion,
            appVersion = appVersion,
            appName = appName,
            exportedAt = exportedAt,
            deviceModel = deviceModel,
            accounts = accounts,
            creditCards = creditCards,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            assets = assets,
            members = members,
            loans = loans,
            loanPayments = loanPayments,
            investmentTransactions = investmentTransactions
        )
    }

    fun extractSummary(jsonString: String): Result<BackupSummary> = runCatching {
        val root = JSONObject(jsonString)
        BackupSummary(
            schemaVersion = root.optInt("schemaVersion", CURRENT_SCHEMA_VERSION),
            appVersion = root.optString("appVersion", "1.0.0"),
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            deviceModel = root.optString("deviceModel", "Thiết bị không xác định"),
            accountCount = root.optJSONArray("accounts")?.length() ?: 0,
            creditCardCount = root.optJSONArray("creditCards")?.length() ?: 0,
            transactionCount = root.optJSONArray("transactions")?.length() ?: 0,
            categoryCount = root.optJSONArray("categories")?.length() ?: 0,
            budgetCount = root.optJSONArray("budgets")?.length() ?: 0,
            assetCount = root.optJSONArray("assets")?.length() ?: 0,
            memberCount = root.optJSONArray("members")?.length() ?: 0,
            loanCount = root.optJSONArray("loans")?.length() ?: 0,
            loanPaymentCount = root.optJSONArray("loanPayments")?.length() ?: 0,
            investmentTransactionCount = root.optJSONArray("investmentTransactions")?.length() ?: 0
        )
    }
}

