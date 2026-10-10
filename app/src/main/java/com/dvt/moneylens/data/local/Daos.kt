package com.dvt.moneylens.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0")
    suspend fun getAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsForBackup(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET balanceMinor = balanceMinor + :deltaMinor WHERE id = :id")
    suspend fun updateBalance(id: String, deltaMinor: Long)

    @Query("UPDATE accounts SET balanceMinor = :balanceMinor WHERE id = :id")
    suspend fun setBalance(id: String, balanceMinor: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards")
    fun getAllCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards")
    suspend fun getCardsList(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: String): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE last4 = :last4 LIMIT 1")
    suspend fun getCardByLast4(last4: String): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE accountId = :accountId LIMIT 1")
    suspend fun getCardByAccountId(accountId: String): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CreditCardEntity>)

    @Query("UPDATE credit_cards SET currentBalanceOwedMinor = currentBalanceOwedMinor + :deltaMinor WHERE id = :id")
    suspend fun updateBalanceOwed(id: String, deltaMinor: Long)

    @Query("UPDATE credit_cards SET nickname = :nickname, creditLimitMinor = :limitMinor, statementDay = :stmtDay, dueDaysAfterStatement = :dueDays, colorHex = :colorHex WHERE id = :id")
    suspend fun updateCardSettings(id: String, nickname: String, limitMinor: Long, stmtDay: Int, dueDays: Int, colorHex: String)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("UPDATE transactions SET accountId = :accountId WHERE id = :id")
    suspend fun updateAccountId(id: String, accountId: String)

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY occurredAt DESC")
    fun getPendingTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<String>): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("UPDATE transactions SET status = 'CONFIRMED', categoryId = :categoryId WHERE id = :id")
    suspend fun confirmTransaction(id: String, categoryId: String?)

    @Query("UPDATE transactions SET status = 'CONFIRMED', categoryId = :categoryId, creditCardId = :creditCardId WHERE id = :id")
    suspend fun confirmTransactionWithCard(id: String, categoryId: String?, creditCardId: String?)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE transactions SET status = 'CONFIRMED' WHERE id IN (:ids)")
    suspend fun confirmTransactions(ids: List<String>)

    @Query("UPDATE transactions SET status = 'CONFIRMED', categoryId = :categoryId WHERE id IN (:ids)")
    suspend fun confirmTransactionsWithCategory(ids: List<String>, categoryId: String)

    @Query("DELETE FROM transactions WHERE amountMinor < 1000 OR (source = 'NOTIFICATION' AND counterparty = 'BANK')")
    suspend fun deleteSpuriousTransactions(): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getCategoriesList(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY currentPriceMinor * quantity DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets")
    suspend fun getAssetList(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<AssetEntity>)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members")
    suspend fun getAllMembersList(): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)

    @Query("DELETE FROM members")
    suspend fun deleteAll()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansList(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: String): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<LoanEntity>)

    @Query("UPDATE loans SET remainingPrincipalMinor = :newRemaining, status = :newStatus WHERE id = :id")
    suspend fun updateLoanBalance(id: String, newRemaining: Long, newStatus: String)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: String)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()
}

@Dao
interface LoanPaymentDao {
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paidAt DESC")
    fun getPaymentsForLoan(loanId: String): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments ORDER BY paidAt DESC")
    fun getAllPayments(): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments")
    suspend fun getAllPaymentsList(): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments WHERE id = :id")
    suspend fun getPaymentById(id: String): LoanPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LoanPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<LoanPaymentEntity>)

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deletePaymentsByLoanId(loanId: String)

    @Query("DELETE FROM loan_payments")
    suspend fun deleteAll()
}

@Dao
interface InvestmentTransactionDao {
    @Query("SELECT * FROM investment_transactions WHERE assetId = :assetId ORDER BY timestamp DESC")
    fun getTransactionsForAsset(assetId: String): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE assetId = :assetId ORDER BY timestamp DESC")
    suspend fun getTransactionsListForAsset(assetId: String): List<InvestmentTransactionEntity>

    @Query("SELECT * FROM investment_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<InvestmentTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: InvestmentTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txList: List<InvestmentTransactionEntity>)

    @Query("DELETE FROM investment_transactions WHERE assetId = :assetId")
    suspend fun deleteTransactionsForAsset(assetId: String)

    @Query("DELETE FROM investment_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM investment_transactions")
    suspend fun deleteAll()
}

