package com.dvt.moneylens.domain.calc

object CostBasisCalculator {

    /**
     * Tính toán giá vốn bình quân gia quyền mới khi mua thêm tài sản.
     * Formula: (Số lượng cũ * Giá vốn cũ + Số lượng mua * Giá mua) / (Số lượng cũ + Số lượng mua)
     */
    fun calculateBuyWeightedAverage(
        currentQuantity: Double,
        currentCostBasisMinor: Long,
        buyQuantity: Double,
        buyPriceMinor: Long
    ): Long {
        val totalQuantity = currentQuantity + buyQuantity
        if (totalQuantity <= 0.0) return buyPriceMinor

        val currentTotalCost = currentQuantity * currentCostBasisMinor
        val buyTotalCost = buyQuantity * buyPriceMinor
        val newTotalCost = currentTotalCost + buyTotalCost

        return (newTotalCost / totalQuantity).toLong()
    }

    /**
     * Tính lãi/lỗ đã thực hiện (Realized PnL) khi bán một phần hoặc toàn bộ tài sản.
     * Formula: (Giá bán - Giá vốn bình quân) * Số lượng bán
     */
    fun calculateSellRealizedPnL(
        currentCostBasisMinor: Long,
        sellQuantity: Double,
        sellPriceMinor: Long
    ): Long {
        val gainPerUnit = sellPriceMinor - currentCostBasisMinor
        return (gainPerUnit * sellQuantity).toLong()
    }

    /**
     * Tính tỷ suất sinh lời (%).
     */
    fun calculatePnlPercentage(
        currentPriceMinor: Long,
        costBasisMinor: Long
    ): Double {
        if (costBasisMinor <= 0L) return 0.0
        return ((currentPriceMinor - costBasisMinor).toDouble() / costBasisMinor) * 100.0
    }
}
