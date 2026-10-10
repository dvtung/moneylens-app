package com.dvt.moneylens.domain.model

import java.util.Locale

/**
 * Chỉ số tài chính chi tiết của từng khoản đầu tư sau khi tính thuế và phí
 */
data class AssetFinancialMetrics(
    val assetId: String,
    val type: AssetType,
    val quantity: Double,
    val currentPriceMinor: Long,
    val costBasisPerUnitMinor: Long,
    val grossMarketValueMinor: Long,
    val totalCostBasisMinor: Long,
    val unrealizedPnlGrossMinor: Long,
    val unrealizedPnlGrossPercent: Double,
    val estimatedLiquidationFeeAndTaxMinor: Long,
    val netLiquidationValueMinor: Long,
    val netUnrealizedPnlMinor: Long,
    val netRoiPercent: Double,
    val breakEvenPricePerUnitMinor: Long,
    val feeAndTaxRatePercent: Double,
    val feeBreakdownDescription: String,
    val specialNotes: String? = null,
    val inflationRatePercent: Double = 4.0,
    val realLiquidationValueMinor: Long = netLiquidationValueMinor,
    val realUnrealizedPnlMinor: Long = netUnrealizedPnlMinor,
    val realRoiPercent: Double = netRoiPercent
)

object InvestmentCalculations {

    /**
     * Tính toán toàn diện chỉ số tài chính của tài sản theo loại tài sản và biểu thuế/phí
     */
    fun calculateAssetMetrics(
        asset: Asset,
        config: InvestmentTaxConfig,
        fundFeeSchedules: Map<String, FundExitFeeSchedule> = emptyMap()
    ): AssetFinancialMetrics {
        val qty = asset.quantity
        val curPrice = asset.currentPriceMinor
        val costBasis = asset.costBasisMinor

        val grossMarketValue = (qty * curPrice).toLong()
        val totalCostBasis = (qty * costBasis).toLong()
        val pnlGross = grossMarketValue - totalCostBasis
        val pnlGrossPercent = if (totalCostBasis > 0) (pnlGross.toDouble() / totalCostBasis) * 100.0 else 0.0

        val inflationRate = config.getInflationRateFor(asset.type)
        val holdingDurationMs = (System.currentTimeMillis() - asset.lastUpdated).coerceAtLeast(0L)
        val holdingYears = if (holdingDurationMs >= 86_400_000L) {
            holdingDurationMs.toDouble() / (365.25 * 86_400_000L)
        } else {
            1.0 // Mặc định quy chiếu 1 năm khi mới khởi tạo để dự phóng sức mua thực tế
        }
        val inflationFactor = if (inflationRate > 0.0) {
            Math.pow(1.0 + (inflationRate / 100.0), holdingYears).coerceAtLeast(1.0)
        } else {
            1.0
        }

        fun calcRealMetrics(netLiquidation: Long): Triple<Long, Long, Double> {
            val realLiquidation = (netLiquidation / inflationFactor).toLong()
            val realPnl = realLiquidation - totalCostBasis
            val realRoi = if (totalCostBasis > 0) (realPnl.toDouble() / totalCostBasis) * 100.0 else 0.0
            return Triple(realLiquidation, realPnl, realRoi)
        }

        return when (asset.type) {
            AssetType.STOCK -> {
                // Chứng khoán:
                // Thuế bán = 0.1% * Doanh thu
                // Phí môi giới bán = 0.2% * Doanh thu (hoặc theo cấu hình)
                val totalFeeTaxRate = config.stockBrokerageFeePercent + config.stockSellingTaxPercent
                val feeTaxFraction = totalFeeTaxRate / 100.0
                val estFeesAndTax = (grossMarketValue * feeTaxFraction).toLong()
                val netLiquidation = grossMarketValue - estFeesAndTax
                val netPnl = netLiquidation - totalCostBasis
                val netRoi = if (totalCostBasis > 0) (netPnl.toDouble() / totalCostBasis) * 100.0 else 0.0
                val (realLiquidation, realPnl, realRoi) = calcRealMetrics(netLiquidation)

                // Giá hòa vốn = Giá vốn / (1 - tỷ lệ thuế phí bán)
                val breakEven = if (feeTaxFraction < 1.0) {
                    (costBasis / (1.0 - feeTaxFraction)).toLong()
                } else costBasis

                AssetFinancialMetrics(
                    assetId = asset.id,
                    type = asset.type,
                    quantity = qty,
                    currentPriceMinor = curPrice,
                    costBasisPerUnitMinor = costBasis,
                    grossMarketValueMinor = grossMarketValue,
                    totalCostBasisMinor = totalCostBasis,
                    unrealizedPnlGrossMinor = pnlGross,
                    unrealizedPnlGrossPercent = pnlGrossPercent,
                    estimatedLiquidationFeeAndTaxMinor = estFeesAndTax,
                    netLiquidationValueMinor = netLiquidation,
                    netUnrealizedPnlMinor = netPnl,
                    netRoiPercent = netRoi,
                    breakEvenPricePerUnitMinor = breakEven,
                    feeAndTaxRatePercent = totalFeeTaxRate,
                    feeBreakdownDescription = "Phí GD bán: ${formatPercent(config.stockBrokerageFeePercent)}% · Thuế TNCN bán: ${formatPercent(config.stockSellingTaxPercent)}%",
                    specialNotes = "Tính theo giá vốn BQ gia quyền. Khấu trừ thuế 0.1% và phí môi giới ${formatPercent(config.stockBrokerageFeePercent)}% khi thanh lý.",
                    inflationRatePercent = inflationRate,
                    realLiquidationValueMinor = realLiquidation,
                    realUnrealizedPnlMinor = realPnl,
                    realRoiPercent = realRoi
                )
            }

            AssetType.FUND -> {
                // Chứng chỉ quỹ mở (CCQ):
                // Tra cứu xem có biểu phí chuộc lại riêng theo mã quỹ hay không
                val customSchedule = fundFeeSchedules[asset.symbol.uppercase()]
                val exitFeePercent = if (customSchedule != null && customSchedule.tiers.isNotEmpty()) {
                    // Lấy mức phí của mốc nắm giữ ngắn hạn nhất (tier đầu tiên) hoặc tính theo số tháng
                    customSchedule.tiers.first().feePercent
                } else {
                    config.fundExitFeeTier1Percent
                }

                val totalFeeTaxRate = exitFeePercent + config.fundSellingTaxPercent
                val feeTaxFraction = totalFeeTaxRate / 100.0
                val estFeesAndTax = (grossMarketValue * feeTaxFraction).toLong()
                val netLiquidation = grossMarketValue - estFeesAndTax
                val netPnl = netLiquidation - totalCostBasis
                val netRoi = if (totalCostBasis > 0) (netPnl.toDouble() / totalCostBasis) * 100.0 else 0.0
                val (realLiquidation, realPnl, realRoi) = calcRealMetrics(netLiquidation)

                val breakEven = if (feeTaxFraction < 1.0) {
                    (costBasis / (1.0 - feeTaxFraction)).toLong()
                } else costBasis

                val scheduleDesc = if (customSchedule != null) {
                    "Biểu phí riêng ${customSchedule.symbol}: ${customSchedule.formatSummary()} · Thuế bán: ${formatPercent(config.fundSellingTaxPercent)}%"
                } else {
                    "Thuế bán: ${formatPercent(config.fundSellingTaxPercent)}% + Phí bán lại CCQ theo thời gian (<12th: ${formatPercent(config.fundExitFeeTier1Percent)}%, 12-24th: ${formatPercent(config.fundExitFeeTier2Percent)}%, ≥24th: ${formatPercent(config.fundExitFeeTier3Percent)}%)"
                }

                AssetFinancialMetrics(
                    assetId = asset.id,
                    type = asset.type,
                    quantity = qty,
                    currentPriceMinor = curPrice,
                    costBasisPerUnitMinor = costBasis,
                    grossMarketValueMinor = grossMarketValue,
                    totalCostBasisMinor = totalCostBasis,
                    unrealizedPnlGrossMinor = pnlGross,
                    unrealizedPnlGrossPercent = pnlGrossPercent,
                    estimatedLiquidationFeeAndTaxMinor = estFeesAndTax,
                    netLiquidationValueMinor = netLiquidation,
                    netUnrealizedPnlMinor = netPnl,
                    netRoiPercent = netRoi,
                    breakEvenPricePerUnitMinor = breakEven,
                    feeAndTaxRatePercent = totalFeeTaxRate,
                    feeBreakdownDescription = scheduleDesc,
                    specialNotes = if (customSchedule != null) "Áp dụng biểu phí riêng của quỹ ${customSchedule.symbol} theo bản cáo bạch." else "Cơ chế quản lý theo lô FIFO. Miễn phí rút vốn khi nắm giữ từ 24 tháng trở lên.",
                    inflationRatePercent = inflationRate,
                    realLiquidationValueMinor = realLiquidation,
                    realUnrealizedPnlMinor = realPnl,
                    realRoiPercent = realRoi
                )
            }

            AssetType.GOLD -> {
                // Vàng:
                // Bắt buộc định giá theo GIÁ MUA VÀO CỦA TIỆM (Bid price) để phản ánh đúng số tiền thu về thực tế.
                // Miễn thuế TNCN chuyển nhượng vàng vật chất theo quy định hiện hành.
                // Phí thanh lý = 0 (vì giá thị trường ghi nhận đã là giá tiệm mua vào).
                val netLiquidation = grossMarketValue
                val netPnl = pnlGross
                val netRoi = pnlGrossPercent
                val breakEven = costBasis // Giá hòa vốn chính là giá vốn đã mua
                val (realLiquidation, realPnl, realRoi) = calcRealMetrics(netLiquidation)

                AssetFinancialMetrics(
                    assetId = asset.id,
                    type = asset.type,
                    quantity = qty,
                    currentPriceMinor = curPrice,
                    costBasisPerUnitMinor = costBasis,
                    grossMarketValueMinor = grossMarketValue,
                    totalCostBasisMinor = totalCostBasis,
                    unrealizedPnlGrossMinor = pnlGross,
                    unrealizedPnlGrossPercent = pnlGrossPercent,
                    estimatedLiquidationFeeAndTaxMinor = 0L,
                    netLiquidationValueMinor = netLiquidation,
                    netUnrealizedPnlMinor = netPnl,
                    netRoiPercent = netRoi,
                    breakEvenPricePerUnitMinor = breakEven,
                    feeAndTaxRatePercent = 0.0,
                    feeBreakdownDescription = "Miễn thuế TNCN bán vàng. Định giá chuẩn theo giá tiệm mua vào (Bid Price)",
                    specialNotes = "Định giá theo giá tiệm thu mua vào, loại trừ hoàn toàn rủi ro chênh lệch spread Mua - Bán.",
                    inflationRatePercent = inflationRate,
                    realLiquidationValueMinor = realLiquidation,
                    realUnrealizedPnlMinor = realPnl,
                    realRoiPercent = realRoi
                )
            }

            AssetType.REAL_ESTATE -> {
                // Bất động sản (BĐS):
                // Thuế TNCN khi bán: 2.0% giá chuyển nhượng
                // Phí hoa hồng môi giới: 1.0% (hoặc theo cấu hình)
                val totalFeeTaxRate = config.realEstateTransferTaxPercent + config.realEstateBrokerageFeePercent
                val feeTaxFraction = totalFeeTaxRate / 100.0
                val estFeesAndTax = (grossMarketValue * feeTaxFraction).toLong()
                val netLiquidation = grossMarketValue - estFeesAndTax
                val netPnl = netLiquidation - totalCostBasis
                val netRoi = if (totalCostBasis > 0) (netPnl.toDouble() / totalCostBasis) * 100.0 else 0.0
                val (realLiquidation, realPnl, realRoi) = calcRealMetrics(netLiquidation)

                val breakEven = if (feeTaxFraction < 1.0) {
                    (costBasis / (1.0 - feeTaxFraction)).toLong()
                } else costBasis

                AssetFinancialMetrics(
                    assetId = asset.id,
                    type = asset.type,
                    quantity = qty,
                    currentPriceMinor = curPrice,
                    costBasisPerUnitMinor = costBasis,
                    grossMarketValueMinor = grossMarketValue,
                    totalCostBasisMinor = totalCostBasis,
                    unrealizedPnlGrossMinor = pnlGross,
                    unrealizedPnlGrossPercent = pnlGrossPercent,
                    estimatedLiquidationFeeAndTaxMinor = estFeesAndTax,
                    netLiquidationValueMinor = netLiquidation,
                    netUnrealizedPnlMinor = netPnl,
                    netRoiPercent = netRoi,
                    breakEvenPricePerUnitMinor = breakEven,
                    feeAndTaxRatePercent = totalFeeTaxRate,
                    feeBreakdownDescription = "Thuế chuyển nhượng: ${formatPercent(config.realEstateTransferTaxPercent)}% · Phí môi giới: ${formatPercent(config.realEstateBrokerageFeePercent)}% (Tổng: ${formatPercent(totalFeeTaxRate)}%)",
                    specialNotes = "Chi phí thanh lý gồm 2% thuế TNCN và 1% môi giới. Giá vốn bao gồm giá mua, cải tạo và thuế trước bạ.",
                    inflationRatePercent = inflationRate,
                    realLiquidationValueMinor = realLiquidation,
                    realUnrealizedPnlMinor = realPnl,
                    realRoiPercent = realRoi
                )
            }

            AssetType.CRYPTO -> {
                // Tiền số / Crypto:
                // Phí sàn giao dịch ước tính 0.1%
                val feeRate = 0.1
                val feeFraction = feeRate / 100.0
                val estFeesAndTax = (grossMarketValue * feeFraction).toLong()
                val netLiquidation = grossMarketValue - estFeesAndTax
                val netPnl = netLiquidation - totalCostBasis
                val netRoi = if (totalCostBasis > 0) (netPnl.toDouble() / totalCostBasis) * 100.0 else 0.0
                val (realLiquidation, realPnl, realRoi) = calcRealMetrics(netLiquidation)

                AssetFinancialMetrics(
                    assetId = asset.id,
                    type = asset.type,
                    quantity = qty,
                    currentPriceMinor = curPrice,
                    costBasisPerUnitMinor = costBasis,
                    grossMarketValueMinor = grossMarketValue,
                    totalCostBasisMinor = totalCostBasis,
                    unrealizedPnlGrossMinor = pnlGross,
                    unrealizedPnlGrossPercent = pnlGrossPercent,
                    estimatedLiquidationFeeAndTaxMinor = estFeesAndTax,
                    netLiquidationValueMinor = netLiquidation,
                    netUnrealizedPnlMinor = netPnl,
                    netRoiPercent = netRoi,
                    breakEvenPricePerUnitMinor = costBasis,
                    feeAndTaxRatePercent = feeRate,
                    feeBreakdownDescription = "Phí sàn giao dịch ước tính: 0.1%",
                    specialNotes = "Tài sản kỹ thuật số lưu trữ on-chain.",
                    inflationRatePercent = inflationRate,
                    realLiquidationValueMinor = realLiquidation,
                    realUnrealizedPnlMinor = realPnl,
                    realRoiPercent = realRoi
                )
            }
        }
    }

    /**
     * Tính lãi/lỗ đã thực hiện khi bán cổ phiếu theo đúng quy chuẩn:
     * Doanh thu = SL bán * Giá bán
     * Thuế phí = Doanh thu * (Phí bán % + Thuế TNCN bán %)
     * Giá vốn xuất bán = SL bán * Giá vốn bình quân
     * Lãi/Lỗ ròng = Doanh thu - Giá vốn xuất bán - Thuế phí
     */
    fun calculateRealizedStockPnl(
        soldQuantity: Double,
        sellPriceMinor: Long,
        avgCostMinor: Long,
        config: InvestmentTaxConfig
    ): Long {
        val grossRevenue = (soldQuantity * sellPriceMinor).toLong()
        val feeTaxPercent = (config.stockBrokerageFeePercent + config.stockSellingTaxPercent) / 100.0
        val feesAndTax = (grossRevenue * feeTaxPercent).toLong()
        val costBasisSold = (soldQuantity * avgCostMinor).toLong()
        return grossRevenue - costBasisSold - feesAndTax
    }

    /**
     * Mô phỏng giá trị thanh lý CCQ theo số tháng nắm giữ cụ thể
     */
    fun calculateFundLiquidationByHoldingMonths(
        quantity: Double,
        navMinor: Long,
        costBasisMinor: Long,
        holdingMonths: Int,
        config: InvestmentTaxConfig
    ): Pair<Long, Long> {
        val grossRevenue = (quantity * navMinor).toLong()
        val exitFeeRate = when {
            holdingMonths < 12 -> config.fundExitFeeTier1Percent
            holdingMonths < 24 -> config.fundExitFeeTier2Percent
            else -> config.fundExitFeeTier3Percent
        }
        val totalFeeTaxRate = (exitFeeRate + config.fundSellingTaxPercent) / 100.0
        val estFeesAndTax = (grossRevenue * totalFeeTaxRate).toLong()
        val netProceeds = grossRevenue - estFeesAndTax
        val netPnl = netProceeds - (quantity * costBasisMinor).toLong()
        return Pair(netProceeds, netPnl)
    }

    private fun formatPercent(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }
}
