package com.dvt.moneylens.domain.model

import java.util.Locale

/**
 * Mốc thời gian và tỷ lệ phí chuộc lại (bán lại) chứng chỉ quỹ mở.
 * @param maxMonths Số tháng nắm giữ tối đa áp dụng mức phí này (ví dụ 6 = dưới 6 tháng, Int.MAX_VALUE = từ mốc trước trở lên).
 * @param feePercent Tỷ lệ % phí chuộc lại.
 */
data class FundExitFeeTier(
    val maxMonths: Int,
    val feePercent: Double
) {
    fun getLabel(): String {
        return if (maxMonths >= 1200) {
            "Nắm giữ từ 24 tháng trở lên"
        } else if (maxMonths == 12) {
            "Dưới 12 tháng (< 1 năm)"
        } else {
            "Dưới $maxMonths tháng"
        }
    }
}

/**
 * Biểu phí chuộc lại riêng biệt cho từng mã quỹ CCQ.
 */
data class FundExitFeeSchedule(
    val symbol: String,
    val fundName: String,
    val tiers: List<FundExitFeeTier>
) {
    /**
     * Tra cứu tỷ lệ % phí chuộc lại theo số tháng nắm giữ thực tế
     */
    fun getExitFeePercent(holdingMonths: Int): Double {
        if (tiers.isEmpty()) return 0.0
        val sortedTiers = tiers.sortedBy { it.maxMonths }
        for (tier in sortedTiers) {
            if (holdingMonths < tier.maxMonths) {
                return tier.feePercent
            }
        }
        return sortedTiers.last().feePercent
    }

    /**
     * Chuỗi tóm tắt các mốc phí để hiển thị trên UI
     */
    fun formatSummary(): String {
        if (tiers.isEmpty()) return "Miễn phí chuộc lại"
        val sorted = tiers.sortedBy { it.maxMonths }
        return sorted.joinToString(" · ") { tier ->
            val feeStr = if (tier.feePercent == tier.feePercent.toLong().toDouble()) {
                "${tier.feePercent.toLong()}%"
            } else {
                "${String.format(Locale.US, "%.1f", tier.feePercent)}%"
            }
            if (tier.maxMonths >= 1200) {
                "≥24th: $feeStr"
            } else {
                "<${tier.maxMonths}th: $feeStr"
            }
        }
    }

    companion object {
        fun getDefaultSchedule(symbol: String, name: String = symbol): FundExitFeeSchedule {
            return when (symbol.uppercase()) {
                "DCDS" -> FundExitFeeSchedule(
                    symbol = "DCDS",
                    fundName = "Quỹ Năng Động DCVFM",
                    tiers = listOf(
                        FundExitFeeTier(maxMonths = 6, feePercent = 2.0),
                        FundExitFeeTier(maxMonths = 12, feePercent = 1.5),
                        FundExitFeeTier(maxMonths = 24, feePercent = 0.5),
                        FundExitFeeTier(maxMonths = Int.MAX_VALUE, feePercent = 0.0)
                    )
                )
                "VESAF" -> FundExitFeeSchedule(
                    symbol = "VESAF",
                    fundName = "Quỹ Đầu Tư Cổ Phiếu Tiếp Cận Thị Trường VinaCapital",
                    tiers = listOf(
                        FundExitFeeTier(maxMonths = 12, feePercent = 1.5),
                        FundExitFeeTier(maxMonths = 24, feePercent = 0.5),
                        FundExitFeeTier(maxMonths = Int.MAX_VALUE, feePercent = 0.0)
                    )
                )
                "VEOF" -> FundExitFeeSchedule(
                    symbol = "VEOF",
                    fundName = "Quỹ Đầu Tư Cổ Phiếu Hưng Thịnh VinaCapital",
                    tiers = listOf(
                        FundExitFeeTier(maxMonths = 6, feePercent = 2.0),
                        FundExitFeeTier(maxMonths = 12, feePercent = 1.0),
                        FundExitFeeTier(maxMonths = 24, feePercent = 0.5),
                        FundExitFeeTier(maxMonths = Int.MAX_VALUE, feePercent = 0.0)
                    )
                )
                else -> FundExitFeeSchedule(
                    symbol = symbol.uppercase(),
                    fundName = name,
                    tiers = listOf(
                        FundExitFeeTier(maxMonths = 12, feePercent = 1.0),
                        FundExitFeeTier(maxMonths = 24, feePercent = 0.5),
                        FundExitFeeTier(maxMonths = Int.MAX_VALUE, feePercent = 0.0)
                    )
                )
            }
        }

        /**
         * Danh sách các quỹ mẫu phổ biến có sẵn tại Việt Nam
         */
        fun getPredefinedFundTemplates(): List<FundExitFeeSchedule> {
            return listOf(
                getDefaultSchedule("DCDS", "Quỹ Năng Động DCVFM"),
                getDefaultSchedule("VESAF", "Quỹ CP Tiếp Cận Thị Trường VinaCapital"),
                getDefaultSchedule("VEOF", "Quỹ CP Hưng Thịnh VinaCapital"),
                getDefaultSchedule("SSISCA", "Quỹ Đầu Tư Lợi Thế Cạnh Tranh SSI"),
                getDefaultSchedule("DCBC", "Quỹ Doanh Nghiệp Hàng Đầu DCVFM")
            )
        }
    }
}
