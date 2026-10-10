package com.dvt.moneylens.data.backup

import com.dvt.moneylens.data.local.AccountEntity
import com.dvt.moneylens.data.local.InvestmentTransactionEntity
import org.junit.Assert.*
import org.junit.Test

class DataBackupManagerTest {

    @Test
    fun testToJsonAndFromJsonIntegrity() {
        val dto = DataBackupDto(
            schemaVersion = 6,
            appVersion = "1.0.0",
            appName = "MoneyLens",
            exportedAt = 1700000000000L,
            deviceModel = "Test Device",
            accounts = listOf(
                AccountEntity(
                    id = "acc_1",
                    name = "Ví Tiền Mặt",
                    type = "CASH",
                    balanceMinor = 1_000_000L,
                    bankCode = null,
                    currency = "VND",
                    ownerMemberId = null,
                    isArchived = false
                )
            ),
            creditCards = emptyList(),
            categories = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            assets = emptyList(),
            members = emptyList(),
            loans = emptyList(),
            loanPayments = emptyList(),
            investmentTransactions = listOf(
                InvestmentTransactionEntity(
                    id = "itx_1",
                    assetId = "asset_btc",
                    symbol = "BTC",
                    action = "BUY",
                    quantity = 0.5,
                    priceMinor = 1_500_000_000L,
                    totalAmountMinor = 750_000_000L,
                    accountId = "acc_1",
                    accountName = "Ví Tiền Mặt",
                    note = "Mua BTC dip",
                    feeAndTaxMinor = 100_000L,
                    timestamp = 1700000000000L
                )
            )
        )

        val jsonString = DataBackupManager.toJson(dto)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("acc_1"))
        assertTrue(jsonString.contains("asset_btc"))

        val summary = DataBackupManager.extractSummary(jsonString)
        assertNotNull(summary)
        assertEquals(6, summary?.schemaVersion)
        assertEquals(1, summary?.accountCount)
        assertEquals(1, summary?.investmentTransactionCount)
        assertEquals(2, summary?.totalRecordCount)

        val restoredDto = DataBackupManager.fromJson(jsonString)
        assertNotNull(restoredDto)
        assertEquals(1, restoredDto?.accounts?.size)
        assertEquals("Ví Tiền Mặt", restoredDto?.accounts?.first()?.name)
        assertEquals(1, restoredDto?.investmentTransactions?.size)
        assertEquals("BTC", restoredDto?.investmentTransactions?.first()?.symbol)
    }
}
