package com.dvt.moneylens.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.dvt.moneylens.data.backup.DataBackupDto
import com.dvt.moneylens.data.local.*
import com.dvt.moneylens.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class MoneyLensRepository(private val db: MoneyLensDatabase) {

    val accounts: Flow<List<Account>> = db.accountDao().getAllAccounts().map { list -> list.map { it.toDomain() } }
    val creditCards: Flow<List<CreditCard>> = db.creditCardDao().getAllCards().map { list -> list.map { it.toDomain() } }
    val transactions: Flow<List<Transaction>> = db.transactionDao().getAllTransactions().map { list -> list.map { it.toDomain() } }
    val pendingTransactions: Flow<List<Transaction>> = db.transactionDao().getPendingTransactions().map { list -> list.map { it.toDomain() } }
    val categories: Flow<List<Category>> = db.categoryDao().getAllCategories().map { list -> list.map { it.toDomain() } }
    val budgets: Flow<List<Budget>> = db.budgetDao().getAllBudgets().map { list -> list.map { it.toDomain() } }
    val assets: Flow<List<Asset>> = db.assetDao().getAllAssets().map { list -> list.map { it.toDomain() } }
    val members: Flow<List<Member>> = db.memberDao().getAllMembers().map { list -> list.map { it.toDomain() } }
    val loans: Flow<List<Loan>> = db.loanDao().getAllLoans().map { list -> list.map { it.toDomain() } }
    val loanPayments: Flow<List<LoanPayment>> = db.loanPaymentDao().getAllPayments().map { list -> list.map { it.toDomain() } }
    val investmentTransactions: Flow<List<InvestmentTransaction>> = db.investmentTransactionDao().getAllTransactions().map { list -> list.map { it.toDomain() } }

    suspend fun checkAndSeedInitialData(context: Context? = null) {
        val prefs = context?.getSharedPreferences("moneylens_prefs", Context.MODE_PRIVATE)
        val hasInitialized = prefs?.getBoolean("has_initialized_db", false) ?: false
        if (!hasInitialized) {
            val existingAccounts = db.accountDao().getAllAccounts().first()
            if (existingAccounts.isEmpty()) {
                seedDemoData(context)
            }
        }
        // Luôn đảm bảo danh mục cat_investment, cat_transfer_out, cat_transfer_in, cat_credit_pay tồn tại
        val cats = db.categoryDao().getAllCategories().first()
        val missingCats = mutableListOf<CategoryEntity>()
        if (cats.none { it.id == "cat_investment" }) {
            missingCats.add(CategoryEntity("cat_investment", "Tích sản & Đầu tư", "account_balance", "#00897B", "EXPENSE"))
        }
        if (cats.none { it.id == "cat_transfer_out" }) {
            missingCats.add(CategoryEntity("cat_transfer_out", "Chuyển tiền", "swap_horiz", "#5B6B8C", "EXPENSE"))
        }
        if (cats.none { it.id == "cat_transfer_in" }) {
            missingCats.add(CategoryEntity("cat_transfer_in", "Chuyển tiền", "swap_horiz", "#5B6B8C", "INCOME"))
        }
        if (cats.none { it.id == "cat_credit_pay" }) {
            missingCats.add(CategoryEntity("cat_credit_pay", "Thanh toán Credit", "credit_card", "#3949AB", "EXPENSE"))
        }
        if (missingCats.isNotEmpty()) {
            db.categoryDao().insertAll(missingCats)
        }
        // Đảm bảo dữ liệu lịch sử đầu tư khởi tạo nếu chưa có
        val existingInvTxs = db.investmentTransactionDao().getTransactionsListForAsset("ast_fpt")
        if (existingInvTxs.isEmpty()) {
            db.investmentTransactionDao().insertAll(
                SeedDataProvider.getDefaultInvestmentTransactions().map { InvestmentTransactionEntity.fromDomain(it) }
            )
        }
    }

    suspend fun seedDemoData(context: Context? = null) {
        context?.getSharedPreferences("moneylens_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("has_initialized_db", true)?.apply()
        db.categoryDao().insertAll(SeedDataProvider.getDefaultCategories().map { CategoryEntity.fromDomain(it) })
        db.memberDao().insertAll(SeedDataProvider.getDefaultMembers().map { MemberEntity.fromDomain(it) })
        db.accountDao().insertAll(SeedDataProvider.getDefaultAccounts().map { AccountEntity.fromDomain(it) })
        db.creditCardDao().insertAll(SeedDataProvider.getDefaultCreditCards().map { CreditCardEntity.fromDomain(it) })
        db.budgetDao().insertAll(SeedDataProvider.getDefaultBudgets().map { BudgetEntity.fromDomain(it) })
        db.assetDao().insertAll(SeedDataProvider.getDefaultAssets().map { AssetEntity.fromDomain(it) })
        db.investmentTransactionDao().insertAll(SeedDataProvider.getDefaultInvestmentTransactions().map { InvestmentTransactionEntity.fromDomain(it) })
        db.loanDao().insertAll(SeedDataProvider.getDefaultLoans().map { LoanEntity.fromDomain(it) })
        db.loanPaymentDao().insertAll(SeedDataProvider.getDefaultLoanPayments().map { LoanPaymentEntity.fromDomain(it) })
        db.transactionDao().insertAll(SeedDataProvider.getDefaultTransactions().map { TransactionEntity.fromDomain(it) })
    }

    suspend fun clearAllData(context: Context? = null) {
        context?.getSharedPreferences("moneylens_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("has_initialized_db", true)?.apply()
        db.transactionDao().deleteAll()
        db.creditCardDao().deleteAll()
        db.accountDao().deleteAll()
        db.budgetDao().deleteAll()
        db.assetDao().deleteAll()
        db.investmentTransactionDao().deleteAll()
        db.loanDao().deleteAll()
        db.loanPaymentDao().deleteAll()
        if (context != null) {
            com.dvt.moneylens.service.BudgetAlertNotifier.cancelAlerts(context)
        }
        // Giữ lại hoặc tạo mới danh mục và thành viên 'Tôi' để có thể thêm dữ liệu mới
        db.categoryDao().insertAll(SeedDataProvider.getDefaultCategories().map { CategoryEntity.fromDomain(it) })
        db.memberDao().insertAll(SeedDataProvider.getDefaultMembers().map { MemberEntity.fromDomain(it) })
        db.accountDao().insertAccount(
            AccountEntity(
                id = "acc_cash",
                name = "Ví tiền mặt",
                type = "CASH",
                balanceMinor = 0L,
                bankCode = null,
                currency = "VND",
                ownerMemberId = "mem_self",
                isArchived = false
            )
        )
    }

    suspend fun exportAllDataForBackup(): DataBackupDto {
        val accounts = db.accountDao().getAllAccountsForBackup()
        val cards = db.creditCardDao().getCardsList()
        val txs = db.transactionDao().getAllTransactionsList()
        val cats = db.categoryDao().getCategoriesList()
        val budgets = db.budgetDao().getAllBudgetsList()
        val assets = db.assetDao().getAssetList()
        val members = db.memberDao().getAllMembersList()
        val loans = db.loanDao().getAllLoansList()
        val payments = db.loanPaymentDao().getAllPaymentsList()

        return DataBackupDto(
            schemaVersion = 5,
            accounts = accounts,
            creditCards = cards,
            categories = cats,
            transactions = txs,
            budgets = budgets,
            assets = assets,
            members = members,
            loans = loans,
            loanPayments = payments
        )
    }

    suspend fun restoreAllData(dto: DataBackupDto, context: Context? = null) {
        db.withTransaction {
            // Delete all records in proper dependency order
            db.loanPaymentDao().deleteAll()
            db.loanDao().deleteAll()
            db.transactionDao().deleteAll()
            db.creditCardDao().deleteAll()
            db.budgetDao().deleteAll()
            db.assetDao().deleteAll()
            db.accountDao().deleteAll()
            db.categoryDao().deleteAll()
            db.memberDao().deleteAll()

            // Restore records in dependency order
            if (dto.categories.isNotEmpty()) {
                db.categoryDao().insertAll(dto.categories)
            }
            if (dto.members.isNotEmpty()) {
                db.memberDao().insertAll(dto.members)
            }
            if (dto.accounts.isNotEmpty()) {
                db.accountDao().insertAll(dto.accounts)
            }
            if (dto.creditCards.isNotEmpty()) {
                db.creditCardDao().insertAll(dto.creditCards)
            }
            if (dto.transactions.isNotEmpty()) {
                db.transactionDao().insertAll(dto.transactions)
            }
            if (dto.budgets.isNotEmpty()) {
                db.budgetDao().insertAll(dto.budgets)
            }
            if (dto.assets.isNotEmpty()) {
                db.assetDao().insertAll(dto.assets)
            }
            if (dto.loans.isNotEmpty()) {
                db.loanDao().insertAll(dto.loans)
            }
            if (dto.loanPayments.isNotEmpty()) {
                db.loanPaymentDao().insertAll(dto.loanPayments)
            }
        }

        context?.getSharedPreferences("moneylens_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("has_initialized_db", true)
            ?.putLong("last_backup_time", System.currentTimeMillis())
            ?.apply()
    }

    suspend fun deleteCreditCard(cardId: String) {
        val card = db.creditCardDao().getCardById(cardId)
        if (card != null) {
            db.creditCardDao().deleteById(cardId)
            db.accountDao().deleteById(card.accountId)
        }
    }

    suspend fun updateBudget(categoryId: String, limitMinor: Long, periodMonth: String = "2026-10") {
        val budget = Budget(
            id = "b_${categoryId}",
            categoryId = categoryId,
            limitMinor = limitMinor,
            periodMonth = periodMonth
        )
        db.budgetDao().insertBudget(BudgetEntity.fromDomain(budget))
    }

    suspend fun updateMonthlyTotalBudget(totalBudgetMinor: Long, categories: List<Category>) {
        val expenseCats = categories.filter { it.type == TransactionType.EXPENSE }
        if (expenseCats.isEmpty()) return
        val perCat = (totalBudgetMinor / expenseCats.size).coerceAtLeast(100_000L)
        val entities = expenseCats.map { cat ->
            BudgetEntity(
                id = "b_${cat.id}",
                categoryId = cat.id,
                limitMinor = perCat,
                periodMonth = "2026-10"
            )
        }
        db.budgetDao().insertAll(entities)
    }

    suspend fun ensureDefaultCashAccount(preferredId: String? = null): String {
        val allAccounts = db.accountDao().getAccountsList()
        if (!preferredId.isNullOrBlank()) {
            val matched = allAccounts.firstOrNull { it.id == preferredId }
            if (matched != null) return matched.id
        }
        val cashOrFirst = allAccounts.firstOrNull { it.type != "CREDIT" } ?: allAccounts.firstOrNull()
        if (cashOrFirst != null) {
            return cashOrFirst.id
        }
        val defaultCash = AccountEntity(
            id = "acc_cash",
            name = "Ví tiền mặt",
            type = "CASH",
            balanceMinor = 0L,
            bankCode = null,
            currency = "VND",
            ownerMemberId = null,
            isArchived = false
        )
        db.accountDao().insertAccount(defaultCash)
        return defaultCash.id
    }

    suspend fun repairMissingAccountTransactions() {
        val defaultCashId = ensureDefaultCashAccount()
        val allAccounts = db.accountDao().getAccountsList().associateBy { it.id }
        val allTxs = db.transactionDao().getAllTransactionsList()

        var deltaForDefaultCash = 0L
        for (tx in allTxs) {
            val accExists = tx.accountId.isNotBlank() && allAccounts.containsKey(tx.accountId)
            if (!accExists) {
                // Gán vào ví mặc định
                db.transactionDao().updateAccountId(tx.id, defaultCashId)
                if (tx.status == "CONFIRMED") {
                    when (tx.type) {
                        "INCOME" -> deltaForDefaultCash += tx.amountMinor
                        "EXPENSE" -> {
                            if (tx.paymentSource != "CREDIT") {
                                deltaForDefaultCash -= tx.amountMinor
                            }
                        }
                    }
                }
            }
        }

        if (deltaForDefaultCash != 0L) {
            db.accountDao().updateBalance(defaultCashId, deltaForDefaultCash)
        }
    }

    suspend fun addTransaction(tx: Transaction) {
        val targetAccountId = ensureDefaultCashAccount(tx.accountId)
        val validTx = if (tx.accountId != targetAccountId) tx.copy(accountId = targetAccountId) else tx
        db.transactionDao().insertTransaction(TransactionEntity.fromDomain(validTx))
        // Update account or card balance immediately if CONFIRMED
        if (validTx.status == TransactionStatus.CONFIRMED) {
            when (validTx.type) {
                TransactionType.EXPENSE -> {
                    if (validTx.categoryId == "cat_credit_pay" && validTx.creditCardId != null) {
                        // Thanh toán thẻ tín dụng: Giảm dư nợ thẻ được chọn và trừ tiền tài khoản nguồn
                        db.creditCardDao().updateBalanceOwed(validTx.creditCardId, -validTx.amountMinor)
                        if (validTx.balanceAfterMinor != null && validTx.balanceAfterMinor > 0L) {
                            db.accountDao().setBalance(validTx.accountId, validTx.balanceAfterMinor)
                        } else {
                            db.accountDao().updateBalance(validTx.accountId, -validTx.amountMinor)
                        }
                    } else if (validTx.paymentSource == PaymentSourceType.CREDIT && validTx.creditCardId != null) {
                        // Chi tiêu qua thẻ tín dụng: Chỉ tăng dư nợ thẻ, KHÔNG trừ tiền mặt tài khoản
                        db.creditCardDao().updateBalanceOwed(validTx.creditCardId, validTx.amountMinor)
                    } else {
                        // Chi tiêu Debit/tiền mặt (bao gồm cả danh mục "Chuyển tiền"): Trừ số dư hoặc đồng bộ số dư sau GD
                        if (validTx.balanceAfterMinor != null && validTx.balanceAfterMinor > 0L) {
                            db.accountDao().setBalance(validTx.accountId, validTx.balanceAfterMinor)
                        } else {
                            db.accountDao().updateBalance(validTx.accountId, -validTx.amountMinor)
                        }
                    }
                }
                TransactionType.INCOME -> {
                    if (validTx.balanceAfterMinor != null && validTx.balanceAfterMinor > 0L) {
                        db.accountDao().setBalance(validTx.accountId, validTx.balanceAfterMinor)
                    } else {
                        db.accountDao().updateBalance(validTx.accountId, validTx.amountMinor)
                    }
                }
                TransactionType.TRANSFER -> {
                    // Handled specifically in transfer flows
                }
            }
        }
    }

    suspend fun confirmTransaction(tx: Transaction) {
        val confirmedTx = tx.copy(status = TransactionStatus.CONFIRMED)
        addTransaction(confirmedTx)
    }

    suspend fun confirmTransaction(id: String, categoryId: String?) {
        val txEntity = db.transactionDao().getTransactionById(id) ?: return
        if (txEntity.status == "PENDING") {
            val targetAccountId = ensureDefaultCashAccount(txEntity.accountId)
            if (txEntity.accountId != targetAccountId) {
                db.transactionDao().updateAccountId(id, targetAccountId)
            }
            val effectiveCatId = categoryId ?: txEntity.categoryId
            when (txEntity.type) {
                "INCOME" -> {
                    if (txEntity.balanceAfterMinor != null && txEntity.balanceAfterMinor > 0L) {
                        db.accountDao().setBalance(targetAccountId, txEntity.balanceAfterMinor)
                    } else {
                        db.accountDao().updateBalance(targetAccountId, txEntity.amountMinor)
                    }
                }
                "EXPENSE" -> {
                    val resolvedCreditCardId = txEntity.creditCardId ?: if (effectiveCatId == "cat_credit_pay") {
                        val text = "${txEntity.note} ${txEntity.counterparty.orEmpty()}".uppercase()
                        val creditOnly = db.creditCardDao().getCardsList().filter { it.cardType == "CREDIT" }
                        creditOnly.find { it.last4.isNotBlank() && text.contains(it.last4) }?.id
                            ?: creditOnly.find { it.nickname.isNotBlank() && text.contains(it.nickname.uppercase()) }?.id
                            ?: creditOnly.find { it.issuerBank.isNotBlank() && text.contains(it.issuerBank.uppercase()) }?.id
                            ?: creditOnly.firstOrNull()?.id
                    } else null

                    if (effectiveCatId == "cat_credit_pay" && resolvedCreditCardId != null) {
                        // Thanh toán thẻ tín dụng: Giảm dư nợ thẻ được chọn và trừ tiền tài khoản nguồn
                        db.creditCardDao().updateBalanceOwed(resolvedCreditCardId, -txEntity.amountMinor)
                        if (txEntity.balanceAfterMinor != null && txEntity.balanceAfterMinor > 0L) {
                            db.accountDao().setBalance(targetAccountId, txEntity.balanceAfterMinor)
                        } else {
                            db.accountDao().updateBalance(targetAccountId, -txEntity.amountMinor)
                        }
                        db.transactionDao().confirmTransactionWithCard(id, categoryId, resolvedCreditCardId)
                        return
                    } else if (txEntity.paymentSource == "CREDIT" && txEntity.creditCardId != null) {
                        // Chỉ tăng nợ thẻ tín dụng, không trừ tiền mặt
                        db.creditCardDao().updateBalanceOwed(txEntity.creditCardId, txEntity.amountMinor)
                    } else {
                        if (txEntity.balanceAfterMinor != null && txEntity.balanceAfterMinor > 0L) {
                            db.accountDao().setBalance(targetAccountId, txEntity.balanceAfterMinor)
                        } else {
                            db.accountDao().updateBalance(targetAccountId, -txEntity.amountMinor)
                        }
                    }
                }
            }
        }
        db.transactionDao().confirmTransaction(id, categoryId)
    }

    suspend fun confirmTransactions(ids: List<String>) {
        if (ids.isEmpty()) return
        for (id in ids) {
            confirmTransaction(id, null)
        }
    }

    suspend fun confirmTransactionsWithCategory(ids: List<String>, categoryId: String?) {
        if (ids.isEmpty()) return
        for (id in ids) {
            confirmTransaction(id, categoryId)
        }
    }

    suspend fun updateTransaction(tx: Transaction) {
        val oldEntity = db.transactionDao().getTransactionById(tx.id)
        if (oldEntity != null && oldEntity.status == "CONFIRMED") {
            // Revert old balance
            when (oldEntity.type) {
                "INCOME" -> db.accountDao().updateBalance(oldEntity.accountId, -oldEntity.amountMinor)
                "EXPENSE" -> {
                    if (oldEntity.categoryId == "cat_credit_pay" && oldEntity.creditCardId != null) {
                        // Hoàn tác thanh toán thẻ: Tăng lại nợ thẻ và cộng lại tiền mặt tài khoản
                        db.creditCardDao().updateBalanceOwed(oldEntity.creditCardId, oldEntity.amountMinor)
                        db.accountDao().updateBalance(oldEntity.accountId, oldEntity.amountMinor)
                    } else if (oldEntity.paymentSource == "CREDIT" && oldEntity.creditCardId != null) {
                        // Trừ lại dư nợ thẻ cũ
                        db.creditCardDao().updateBalanceOwed(oldEntity.creditCardId, -oldEntity.amountMinor)
                    } else {
                        // Hoàn lại tiền mặt cũ
                        db.accountDao().updateBalance(oldEntity.accountId, oldEntity.amountMinor)
                    }
                }
            }
        }
        // Save and apply new balance (tự động phân loại Debit/Credit chuẩn xác)
        addTransaction(tx)
    }

    suspend fun deleteTransaction(id: String) {
        val txEntity = db.transactionDao().getTransactionById(id)
        if (txEntity != null && txEntity.status == "CONFIRMED") {
            when (txEntity.type) {
                "INCOME" -> {
                    db.accountDao().updateBalance(txEntity.accountId, -txEntity.amountMinor)
                }
                "EXPENSE" -> {
                    if (txEntity.categoryId == "cat_credit_pay" && txEntity.creditCardId != null) {
                        // Hoàn tác thanh toán thẻ: Tăng lại nợ thẻ và cộng lại tiền mặt tài khoản
                        db.creditCardDao().updateBalanceOwed(txEntity.creditCardId, txEntity.amountMinor)
                        db.accountDao().updateBalance(txEntity.accountId, txEntity.amountMinor)
                    } else if (txEntity.paymentSource == "CREDIT" && txEntity.creditCardId != null) {
                        db.creditCardDao().updateBalanceOwed(txEntity.creditCardId, -txEntity.amountMinor)
                    } else {
                        db.accountDao().updateBalance(txEntity.accountId, txEntity.amountMinor)
                    }
                }
            }
        }
        db.transactionDao().deleteById(id)
    }

    suspend fun deleteTransactions(ids: List<String>) {
        if (ids.isEmpty()) return
        for (id in ids) {
            deleteTransaction(id)
        }
    }

    suspend fun convertCreditTxToInstallment(txId: String, loan: Loan) {
        val txEntity = db.transactionDao().getTransactionById(txId) ?: return
        // 1. Create the installment loan
        db.loanDao().insertLoan(LoanEntity.fromDomain(loan))
        
        // 2. Reduce the credit card balance owed (tiền mặt không bị trừ nên không hoàn vào account)
        val cardId = txEntity.creditCardId
        if (cardId != null) {
            db.creditCardDao().updateBalanceOwed(cardId, -txEntity.amountMinor)
        }
        
        // 3. Mark the transaction with note indicating conversion
        val updatedTx = txEntity.copy(
            note = "[Trả góp: ${loan.title}] ${txEntity.note}".trim()
        )
        db.transactionDao().insertTransaction(updatedTx)
    }

    suspend fun cleanSpuriousTransactions(): Int {
        return db.transactionDao().deleteSpuriousTransactions()
    }

    suspend fun updateCreditCard(card: CreditCard) {
        db.creditCardDao().updateCardSettings(
            id = card.id,
            nickname = card.nickname,
            limitMinor = card.creditLimitMinor,
            stmtDay = card.statementDay,
            dueDays = card.dueDaysAfterStatement,
            colorHex = card.colorHex
        )
        val acc = db.accountDao().getAccountById(card.accountId)
        if (acc != null) {
            db.accountDao().insertAccount(acc.copy(name = "Thẻ ${card.nickname}"))
        }
    }

    suspend fun addCreditCard(card: CreditCard, initialBalanceMinor: Long = 0L) {
        val accountType = when (card.cardType) {
            CardType.CREDIT -> AccountType.CREDIT_CARD
            CardType.DEBIT -> AccountType.BANK
            CardType.WALLET -> AccountType.EWALLET
            CardType.BANK_ACCOUNT -> AccountType.BANK
        }
        val initialBalance = when (card.cardType) {
            CardType.CREDIT -> -card.currentBalanceOwedMinor
            CardType.DEBIT, CardType.WALLET, CardType.BANK_ACCOUNT -> initialBalanceMinor
        }
        val accountName = when (card.cardType) {
            CardType.CREDIT -> "Thẻ ${card.nickname}"
            CardType.DEBIT -> "Thẻ Debit ${card.nickname}"
            CardType.WALLET -> "Ví ${card.nickname}"
            CardType.BANK_ACCOUNT -> "TK ${card.nickname}"
        }
        val account = Account(
            id = card.accountId,
            name = accountName,
            type = accountType,
            balanceMinor = initialBalance,
            bankCode = card.issuerBank,
            ownerMemberId = card.ownerMemberId
        )
        db.accountDao().insertAccount(AccountEntity.fromDomain(account))
        db.creditCardDao().insertCard(CreditCardEntity.fromDomain(card))
    }

    suspend fun recordCardPayment(cardId: String, fromAccountId: String, amountMinor: Long) {
        val now = System.currentTimeMillis()
        val card = db.creditCardDao().getCardById(cardId)?.toDomain() ?: return
        
        // 1. Reduce card balance owed
        db.creditCardDao().updateBalanceOwed(cardId, -amountMinor)
        
        // 2. Deduct from payment bank account
        db.accountDao().updateBalance(fromAccountId, -amountMinor)

        // 3. Create TRANSFER transaction
        val transferTx = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = fromAccountId,
            amountMinor = amountMinor,
            type = TransactionType.TRANSFER,
            categoryId = "cat_transfer",
            note = "Thanh toán thẻ tín dụng ${card.nickname} (••••${card.last4})",
            counterparty = card.issuerBank,
            status = TransactionStatus.CONFIRMED,
            occurredAt = now,
            source = "MANUAL",
            memberId = card.ownerMemberId
        )
        db.transactionDao().insertTransaction(TransactionEntity.fromDomain(transferTx))
    }

    suspend fun addOrUpdateAsset(asset: Asset) {
        db.assetDao().insertAsset(AssetEntity.fromDomain(asset))
    }

    suspend fun deleteAsset(
        id: String,
        refundAccountId: String? = null,
        refundAmountMinor: Long = 0L
    ) {
        if (!refundAccountId.isNullOrBlank() && refundAmountMinor > 0) {
            val asset = db.assetDao().getAssetById(id)?.toDomain()
            val symbol = asset?.symbol ?: "tài sản"
            db.accountDao().updateBalance(refundAccountId, refundAmountMinor)
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                accountId = refundAccountId,
                amountMinor = refundAmountMinor,
                type = TransactionType.INCOME,
                categoryId = "cat_investment",
                paymentSource = PaymentSourceType.DEBIT,
                note = "Hoàn tiền thu hồi khoản đầu tư $symbol",
                counterparty = "Thanh lý đầu tư",
                status = TransactionStatus.CONFIRMED,
                occurredAt = System.currentTimeMillis(),
                source = "MANUAL"
            )
            db.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
        }
        db.assetDao().deleteAssetById(id)
        db.investmentTransactionDao().deleteTransactionsForAsset(id)
    }

    suspend fun executeInvestmentTransaction(
        action: InvestmentAction,
        assetId: String?,
        symbol: String,
        name: String,
        assetType: AssetType,
        quantity: Double,
        priceMinor: Long,
        accountId: String?,
        note: String,
        occurredAt: Long = System.currentTimeMillis()
    ) {
        val now = occurredAt
        val cleanSymbol = symbol.trim().uppercase()

        when (action) {
            InvestmentAction.BUY -> {
                val existingEntity = if (assetId != null) db.assetDao().getAssetById(assetId) else null
                val totalCost = (quantity * priceMinor).toLong()
                val targetAssetId: String

                if (existingEntity != null) {
                    targetAssetId = existingEntity.id
                    val current = existingEntity.toDomain()
                    val newQuantity = current.quantity + quantity
                    val newCostBasis = com.dvt.moneylens.domain.calc.CostBasisCalculator.calculateBuyWeightedAverage(
                        currentQuantity = current.quantity,
                        currentCostBasisMinor = current.costBasisMinor,
                        buyQuantity = quantity,
                        buyPriceMinor = priceMinor
                    )
                    val updated = current.copy(
                        quantity = newQuantity,
                        costBasisMinor = newCostBasis,
                        currentPriceMinor = priceMinor,
                        notes = if (note.isNotBlank()) note else current.notes,
                        lastUpdated = now
                    )
                    db.assetDao().insertAsset(AssetEntity.fromDomain(updated))
                } else {
                    val newId = UUID.randomUUID().toString()
                    targetAssetId = newId
                    val newAsset = Asset(
                        id = newId,
                        type = assetType,
                        symbol = cleanSymbol,
                        name = name.ifBlank { cleanSymbol },
                        quantity = quantity,
                        currentPriceMinor = priceMinor,
                        costBasisMinor = priceMinor,
                        notes = note.ifBlank { null },
                        lastUpdated = now
                    )
                    db.assetDao().insertAsset(AssetEntity.fromDomain(newAsset))
                }

                // Ghi nhận lịch sử giao dịch MUA vào bảng investment_transactions
                val accountName = if (!accountId.isNullOrBlank()) db.accountDao().getAccountById(accountId)?.name else null
                val invTx = InvestmentTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    assetId = targetAssetId,
                    symbol = cleanSymbol,
                    action = InvestmentAction.BUY.name,
                    quantity = quantity,
                    priceMinor = priceMinor,
                    totalAmountMinor = totalCost,
                    accountId = accountId,
                    accountName = accountName,
                    note = note.ifBlank { if (existingEntity != null) "Mua thêm $cleanSymbol" else "Mua ban đầu $cleanSymbol" },
                    feeAndTaxMinor = 0L,
                    timestamp = now
                )
                db.investmentTransactionDao().insertTransaction(invTx)

                // Deduct cash from linked account if selected (Phương án C)
                if (!accountId.isNullOrBlank() && totalCost > 0) {
                    db.accountDao().updateBalance(accountId, -totalCost)
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        accountId = accountId,
                        amountMinor = totalCost,
                        type = TransactionType.EXPENSE,
                        categoryId = "cat_investment",
                        paymentSource = PaymentSourceType.DEBIT,
                        note = "Tích sản: Mua $quantity $cleanSymbol",
                        counterparty = "Thị trường tài chính",
                        status = TransactionStatus.CONFIRMED,
                        occurredAt = now,
                        source = "MANUAL"
                    )
                    db.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
                }
            }

            InvestmentAction.SELL -> {
                val existingEntity = if (assetId != null) db.assetDao().getAssetById(assetId) else null
                val totalProceeds = (quantity * priceMinor).toLong()
                val targetAssetId = assetId ?: existingEntity?.id ?: UUID.randomUUID().toString()

                if (existingEntity != null) {
                    val current = existingEntity.toDomain()
                    val remainingQuantity = (current.quantity - quantity).coerceAtLeast(0.0)
                    if (remainingQuantity <= 0.0001) {
                        db.assetDao().deleteAssetById(current.id)
                    } else {
                        val updated = current.copy(
                            quantity = remainingQuantity,
                            currentPriceMinor = priceMinor,
                            notes = if (note.isNotBlank()) note else current.notes,
                            lastUpdated = now
                        )
                        db.assetDao().insertAsset(AssetEntity.fromDomain(updated))
                    }
                }

                // Ghi nhận lịch sử giao dịch BÁN vào bảng investment_transactions
                val accountName = if (!accountId.isNullOrBlank()) db.accountDao().getAccountById(accountId)?.name else null
                val invTx = InvestmentTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    assetId = targetAssetId,
                    symbol = cleanSymbol,
                    action = InvestmentAction.SELL.name,
                    quantity = quantity,
                    priceMinor = priceMinor,
                    totalAmountMinor = totalProceeds,
                    accountId = accountId,
                    accountName = accountName,
                    note = note.ifBlank { "Bán $quantity $cleanSymbol" },
                    feeAndTaxMinor = 0L,
                    timestamp = now
                )
                db.investmentTransactionDao().insertTransaction(invTx)

                // Add proceeds to linked account
                if (!accountId.isNullOrBlank() && totalProceeds > 0) {
                    db.accountDao().updateBalance(accountId, totalProceeds)
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        accountId = accountId,
                        amountMinor = totalProceeds,
                        type = TransactionType.INCOME,
                        categoryId = "cat_dividend",
                        note = "Bán $quantity $cleanSymbol",
                        counterparty = "Thị trường tài chính",
                        status = TransactionStatus.CONFIRMED,
                        occurredAt = now,
                        source = "MANUAL"
                    )
                    db.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
                }
            }

            InvestmentAction.DIVIDEND -> {
                val existingEntity = if (assetId != null) db.assetDao().getAssetById(assetId) else null
                val targetAssetId = assetId ?: existingEntity?.id ?: UUID.randomUUID().toString()
                val accountName = if (!accountId.isNullOrBlank()) db.accountDao().getAccountById(accountId)?.name else null
                val invTx = InvestmentTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    assetId = targetAssetId,
                    symbol = cleanSymbol,
                    action = InvestmentAction.DIVIDEND.name,
                    quantity = 0.0,
                    priceMinor = priceMinor,
                    totalAmountMinor = priceMinor,
                    accountId = accountId,
                    accountName = accountName,
                    note = note.ifBlank { "Cổ tức $cleanSymbol" },
                    feeAndTaxMinor = 0L,
                    timestamp = now
                )
                db.investmentTransactionDao().insertTransaction(invTx)

                // Cash dividend directly received
                if (!accountId.isNullOrBlank() && priceMinor > 0) {
                    db.accountDao().updateBalance(accountId, priceMinor)
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        accountId = accountId,
                        amountMinor = priceMinor,
                        type = TransactionType.INCOME,
                        categoryId = "cat_dividend",
                        note = "Cổ tức $cleanSymbol: ${note.ifBlank { "Nhận cổ tức tiền mặt" }}",
                        counterparty = cleanSymbol,
                        status = TransactionStatus.CONFIRMED,
                        occurredAt = now,
                        source = "MANUAL"
                    )
                    db.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
                }
            }

            InvestmentAction.VALUATION -> {
                val existingEntity = if (assetId != null) db.assetDao().getAssetById(assetId) else null
                if (existingEntity != null) {
                    val current = existingEntity.toDomain()
                    val updated = current.copy(
                        currentPriceMinor = priceMinor,
                        notes = if (note.isNotBlank()) note else current.notes,
                        lastUpdated = now
                    )
                    db.assetDao().insertAsset(AssetEntity.fromDomain(updated))
                }
            }
        }
    }


    suspend fun refreshAllAssetPrices(isOfflineMode: Boolean): Int {
        if (isOfflineMode) return 0
        val currentEntities = db.assetDao().getAssetList()
        val assetList = currentEntities.map { it.toDomain() }
        if (assetList.isEmpty()) return 0

        val updatedMap = com.dvt.moneylens.data.network.MarketPriceService.fetchUpdatedPrices(assetList, isOfflineMode)
        val now = System.currentTimeMillis()

        var count = 0
        for ((assetId, newPrice) in updatedMap) {
            val entity = db.assetDao().getAssetById(assetId)
            if (entity != null) {
                val updated = entity.toDomain().copy(
                    currentPriceMinor = newPrice,
                    lastUpdated = now
                )
                db.assetDao().insertAsset(AssetEntity.fromDomain(updated))
                count++
            }
        }
        return count
    }

    suspend fun addOrUpdateLoan(loan: Loan) {
        db.loanDao().insertLoan(LoanEntity.fromDomain(loan))
    }

    suspend fun deleteLoan(loanId: String) {
        db.loanDao().deleteLoanById(loanId)
        db.loanPaymentDao().deletePaymentsByLoanId(loanId)
    }

    fun getPaymentsForLoan(loanId: String): Flow<List<LoanPayment>> {
        return db.loanPaymentDao().getPaymentsForLoan(loanId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun recordLoanPayment(
        loanId: String,
        amountMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        accountId: String?,
        note: String,
        paidAt: Long = System.currentTimeMillis()
    ) {
        val loanEntity = db.loanDao().getLoanById(loanId) ?: return
        val currentLoan = loanEntity.toDomain()
        val now = paidAt

        val newRemaining = (currentLoan.remainingPrincipalMinor - principalMinor).coerceAtLeast(0L)
        val newStatus = if (newRemaining <= 0L) LoanStatus.PAID_OFF else LoanStatus.ACTIVE

        db.loanDao().updateLoanBalance(loanId, newRemaining, newStatus.name)

        val defaultNote = if (currentLoan.type == LoanType.BORROWING) "Trả nợ: ${currentLoan.title}" else "Thu nợ: ${currentLoan.title}"
        val finalNote = note.ifBlank { defaultNote }

        val payment = LoanPayment(
            id = UUID.randomUUID().toString(),
            loanId = loanId,
            amountMinor = amountMinor,
            principalMinor = principalMinor,
            interestMinor = interestMinor,
            paidAt = now,
            note = finalNote
        )
        db.loanPaymentDao().insertPayment(LoanPaymentEntity.fromDomain(payment))

        if (!accountId.isNullOrBlank() && amountMinor > 0L) {
            when (currentLoan.type) {
                LoanType.BORROWING -> {
                    db.accountDao().updateBalance(accountId, -amountMinor)
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        accountId = accountId,
                        amountMinor = amountMinor,
                        type = TransactionType.EXPENSE,
                        categoryId = "cat_bills",
                        note = "Trả nợ: ${currentLoan.title} (${currentLoan.counterparty}) - $finalNote",
                        counterparty = currentLoan.counterparty,
                        status = TransactionStatus.CONFIRMED,
                        occurredAt = now,
                        source = "MANUAL"
                    )
                    db.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
                }
                LoanType.LENDING -> {
                    db.accountDao().updateBalance(accountId, amountMinor)
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        accountId = accountId,
                        amountMinor = amountMinor,
                        type = TransactionType.INCOME,
                        categoryId = "cat_other_income",
                        note = "Thu nợ: ${currentLoan.title} (${currentLoan.counterparty}) - $finalNote",
                        counterparty = currentLoan.counterparty,
                        status = TransactionStatus.CONFIRMED,
                        occurredAt = now,
                        source = "MANUAL"
                    )
                    db.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
                }
            }
        }
    }

    suspend fun deleteLoanPayment(paymentId: String, loanId: String) {
        val paymentEntity = db.loanPaymentDao().getPaymentById(paymentId) ?: return
        val loanEntity = db.loanDao().getLoanById(loanId) ?: return
        val currentLoan = loanEntity.toDomain()

        val restoredRemaining = (currentLoan.remainingPrincipalMinor + paymentEntity.principalMinor).coerceAtMost(currentLoan.originalAmountMinor)
        val restoredStatus = if (restoredRemaining > 0L) LoanStatus.ACTIVE else LoanStatus.PAID_OFF

        db.loanDao().updateLoanBalance(loanId, restoredRemaining, restoredStatus.name)
        db.loanPaymentDao().deletePaymentById(paymentId)
    }
}
