package com.dvt.moneylens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CreditCardEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        AssetEntity::class,
        MemberEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        InvestmentTransactionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class MoneyLensDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun assetDao(): AssetDao
    abstract fun memberDao(): MemberDao
    abstract fun loanDao(): LoanDao
    abstract fun loanPaymentDao(): LoanPaymentDao
    abstract fun investmentTransactionDao(): InvestmentTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: MoneyLensDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `investment_transactions` (
                        `id` TEXT NOT NULL,
                        `assetId` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `priceMinor` INTEGER NOT NULL,
                        `totalAmountMinor` INTEGER NOT NULL,
                        `accountId` TEXT,
                        `accountName` TEXT,
                        `note` TEXT NOT NULL,
                        `feeAndTaxMinor` INTEGER NOT NULL DEFAULT 0,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): MoneyLensDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneyLensDatabase::class.java,
                    "moneylens.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

