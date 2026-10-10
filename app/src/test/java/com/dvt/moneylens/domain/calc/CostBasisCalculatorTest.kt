package com.dvt.moneylens.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class CostBasisCalculatorTest {

    @Test
    fun testCalculateBuyWeightedAverage() {
        // Mua 1: 2 cổ phiếu @ 100,000đ = 200,000đ
        // Mua 2: 3 cổ phiếu @ 150,000đ = 450,000đ
        // Tổng tiền = 650,000đ, Tổng số lượng = 5 cổ phiếu
        // Giá trung bình = 650,000 / 5 = 130,000đ
        val avgCost = CostBasisCalculator.calculateBuyWeightedAverage(
            currentQuantity = 2.0,
            currentCostBasisMinor = 100_000L,
            buyQuantity = 3.0,
            buyPriceMinor = 150_000L
        )
        assertEquals(130_000L, avgCost)
    }

    @Test
    fun testCalculateSellRealizedPnL() {
        // Giá vốn bình quân: 100,000đ
        // Bán 1.5 cổ phiếu @ 140,000đ
        // Lãi mỗi cổ phiếu = 40,000đ
        // Tổng lãi = 40,000 * 1.5 = 60,000đ
        val pnl = CostBasisCalculator.calculateSellRealizedPnL(
            currentCostBasisMinor = 100_000L,
            sellQuantity = 1.5,
            sellPriceMinor = 140_000L
        )
        assertEquals(60_000L, pnl)
    }

    @Test
    fun testCalculatePnlPercentage() {
        // Giá vốn: 100,000đ, Giá hiện tại: 125,000đ -> +25%
        val pct = CostBasisCalculator.calculatePnlPercentage(
            currentPriceMinor = 125_000L,
            costBasisMinor = 100_000L
        )
        assertEquals(25.0, pct, 0.001)
    }
}
