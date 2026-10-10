package com.dvt.moneylens.domain.model

/**
 * Cấu hình thuế và phí áp dụng cho việc tính toán lãi/lỗ và giá trị thanh lý ròng
 * của các loại tài sản đầu tư (Cổ phiếu, Quỹ mở CCQ, Vàng, Bất động sản).
 */
data class InvestmentTaxConfig(
    // Chứng khoán (Cổ phiếu)
    val stockBrokerageFeePercent: Double = 0.2,   // Phí môi giới mua/bán cổ phiếu (%)
    val stockSellingTaxPercent: Double = 0.1,      // Thuế TNCN khi bán cổ phiếu (%)
    val cashDividendTaxPercent: Double = 5.0,      // Thuế TNCN khi nhận cổ tức tiền mặt (%)

    // Chứng chỉ quỹ mở (CCQ)
    val fundBuyingFeePercent: Double = 0.5,        // Phí mua CCQ ban đầu (%)
    val fundSellingTaxPercent: Double = 0.1,       // Thuế TNCN khi bán/chuộc lại CCQ (%)
    val fundExitFeeTier1Percent: Double = 1.0,     // Phí bán lại CCQ nắm giữ dưới 12 tháng (%)
    val fundExitFeeTier2Percent: Double = 0.5,     // Phí bán lại CCQ nắm giữ từ 12 đến 24 tháng (%)
    val fundExitFeeTier3Percent: Double = 0.0,     // Phí bán lại CCQ nắm giữ từ 24 tháng trở lên (%)

    // Bất động sản (BĐS)
    val realEstateTransferTaxPercent: Double = 2.0, // Thuế TNCN khi chuyển nhượng BĐS (%)
    val realEstateBrokerageFeePercent: Double = 1.0, // Phí hoa hồng môi giới khi bán BĐS (%)

    // Chỉ số Lạm phát & Trượt giá hàng năm (%)
    val generalInflationRatePercent: Double = 4.0,   // Tỷ lệ lạm phát mục tiêu chung (%/năm)
    val stockInflationRatePercent: Double = 4.0,     // Lạm phát áp dụng cho Cổ phiếu (%/năm)
    val realEstateInflationRatePercent: Double = 4.0,// Lạm phát áp dụng cho Bất động sản (%/năm)
    val goldInflationRatePercent: Double = 4.0,      // Lạm phát áp dụng cho Vàng (%/năm)
    val cryptoInflationRatePercent: Double = 4.0,    // Lạm phát áp dụng cho Tiền mã hóa (%/năm)
    val fundInflationRatePercent: Double = 4.0       // Lạm phát áp dụng cho Quỹ CCQ (%/năm)
) {
    fun getInflationRateFor(type: AssetType): Double {
        return when (type) {
            AssetType.STOCK -> stockInflationRatePercent
            AssetType.REAL_ESTATE -> realEstateInflationRatePercent
            AssetType.GOLD -> goldInflationRatePercent
            AssetType.CRYPTO -> cryptoInflationRatePercent
            AssetType.FUND -> fundInflationRatePercent
        }
    }
}
