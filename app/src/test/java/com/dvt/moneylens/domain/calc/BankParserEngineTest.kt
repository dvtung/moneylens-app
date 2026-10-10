package com.dvt.moneylens.domain.calc

import com.dvt.moneylens.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class BankParserEngineTest {

    @Test
    fun testParseVietcombankMessage() {
        val message = RawFinancialMessage(
            senderOrPackage = "Vietcombank",
            body = "SD TK 0071001234567 +5,000,000 VND luc 10:15 10/10/2026. So du 25,000,000 VND. ND: Chuyen tien luong thang 10"
        )
        val result = BankParserEngine.parse(message)

        assertNotNull(result)
        assertEquals("VCB", result?.bankCode)
        assertEquals(5_000_000L, result?.amountMinor)
        assertEquals(TransactionType.INCOME, result?.type)
        assertEquals(25_000_000L, result?.balanceAfterMinor)
    }

    @Test
    fun testParseTechcombankMessage() {
        val message = RawFinancialMessage(
            senderOrPackage = "TCB",
            body = "TK 19031234567890 tai Techcombank GD: -250,000 VND luc 14:30. So du: 12,500,000 VND. ND: Thanh toan tien dien"
        )
        val result = BankParserEngine.parse(message)

        assertNotNull(result)
        assertEquals("TCB", result?.bankCode)
        assertEquals(250_000L, result?.amountMinor)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertEquals(12_500_000L, result?.balanceAfterMinor)
    }

    @Test
    fun testParseMBBankMessage() {
        val message = RawFinancialMessage(
            senderOrPackage = "MBBank",
            body = "MBBank: TK 0987654321 +1,200,000VND vao 12/10/2026 09:00. So du 8,400,000VND. ND: Nhan tien hoan tra"
        )
        val result = BankParserEngine.parse(message)

        assertNotNull(result)
        assertEquals("MB", result?.bankCode)
        assertEquals(1_200_000L, result?.amountMinor)
        assertEquals(TransactionType.INCOME, result?.type)
    }

    @Test
    fun testParseVPBankCreditCardMessage() {
        val message = RawFinancialMessage(
            senderOrPackage = "VPBank",
            body = "The VISA credit xxxx1234 chi tieu 450,000 VND tai SHOPEE luc 11:20. Han muc kha dung: 15,000,000 VND."
        )
        val result = BankParserEngine.parse(message)

        assertNotNull(result)
        assertEquals("VPB", result?.bankCode)
        assertEquals(450_000L, result?.amountMinor)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertTrue(result?.isCreditCard == true)
        assertEquals("1234", result?.cardLast4)
    }

    @Test
    fun testParseSmallAmountIgnored() {
        val message = RawFinancialMessage(
            senderOrPackage = "Vietcombank",
            body = "SD TK 007100 +50 VND. So du 100,000 VND."
        )
        val result = BankParserEngine.parse(message)
        assertNull("Amount < 1000 VND should be ignored", result)
    }
}
