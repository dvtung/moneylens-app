package com.dvt.moneylens.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.ui.theme.Spacing

enum class InvestmentGuideTab(val title: String, val icon: ImageVector) {
    STOCK("Cổ phiếu", Icons.Rounded.ShowChart),
    FUND("Quỹ mở (CCQ)", Icons.Rounded.PieChart),
    REAL_ESTATE("Bất động sản", Icons.Rounded.HomeWork),
    GOLD("Vàng & Kim loại", Icons.Rounded.Stars),
    CRYPTO("Crypto & Khác", Icons.Rounded.CurrencyBitcoin),
    OVERVIEW("Tổng quan & Thuế Phí", Icons.Rounded.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentGuideScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var selectedTab by remember { mutableStateOf(InvestmentGuideTab.STOCK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Phương pháp tính đầu tư",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Công thức toán học & Ví dụ minh họa theo chuẩn VN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row phân loại theo loại tài sản
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = Spacing.md,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                InvestmentGuideTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // Nội dung theo Tab đã chọn
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                when (selectedTab) {
                    InvestmentGuideTab.STOCK -> stockGuideContent()
                    InvestmentGuideTab.FUND -> fundGuideContent()
                    InvestmentGuideTab.REAL_ESTATE -> realEstateGuideContent()
                    InvestmentGuideTab.GOLD -> goldGuideContent()
                    InvestmentGuideTab.CRYPTO -> cryptoGuideContent()
                    InvestmentGuideTab.OVERVIEW -> overviewGuideContent()
                }

                // Disclaimer
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.md, bottom = Spacing.xxl)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "Lưu ý pháp lý & Bản quyền:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tài liệu mang tính tham khảo kỹ thuật quản lý tài chính cá nhân. Hãy đối chiếu biểu phí hiện hành của CTCK, Công ty quản lý quỹ và ngân hàng tại thời điểm giao dịch.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 1. CỔ PHIẾU ====================
private fun androidx.compose.foundation.lazy.LazyListScope.stockGuideContent() {
    item {
        GuideIntroHeader(
            title = "Cổ phiếu & Chứng khoán niêm yết",
            subtitle = "Sở hữu cổ phần doanh nghiệp trên sàn HOSE, HNX, UPCoM",
            icon = Icons.Rounded.ShowChart,
            color = Color(0xFF1E88E5)
        )
    }

    item {
        GuideSectionCard(title = "1. Khái niệm & Cơ chế Hòa vốn", icon = Icons.Rounded.Info) {
            BulletPoint("Giá vốn bình quân gia quyền (Cost Basis): Tổng số tiền mua (gồm phí mua) chia cho tổng số lượng cổ phiếu đang nắm giữ.")
            BulletPoint("Điểm hòa vốn (Break-even Price): Mức giá thị trường cần đạt để sau khi trừ thuế TNCN bán (0.1%) và phí môi giới bán (0.2%), bạn thu về đúng bằng giá vốn ban đầu.")
            FormulaBox("Giá_hòa_vốn = Giá_vốn_BQ ÷ (1 − (Phí_môi_giới% + Thuế_TNCN%))")
            BulletPoint("Cổ tức tiền mặt: Nhận về = Số CP × Cổ tức/CP × 95% (đã trừ 5% thuế TNCN tại nguồn). Tiền về ví không làm giảm giá vốn.")
            BulletPoint("Cổ tức bằng cổ phiếu / CP thưởng: Tăng số lượng cổ phiếu, giá vốn BQ tự động điều chỉnh giảm tương ứng.")
        }
    }

    item {
        GuideSectionCard(title = "2. Công thức toán học Chi tiết", icon = Icons.Rounded.Calculate) {
            FormulaBox(
                "1. Tổng giá vốn (Total Cost) = Số_lượng × Giá_vốn_BQ\n\n" +
                "2. Doanh thu thị trường (Gross) = Số_lượng × Giá_thị_trường\n\n" +
                "3. Thuế & Phí bán = Gross × (Phí_môi_giới% + Thuế_TNCN%)\n" +
                "   • Phí môi giới bán mặc định: 0.2%\n" +
                "   • Thuế TNCN bán cổ phiếu: 0.1%\n\n" +
                "4. Giá trị thanh lý ròng (V_net) = Gross − Thuế_Phí_bán\n\n" +
                "5. Lãi/lỗ ròng = V_net − Tổng_giá_vốn\n\n" +
                "6. Giá trị thực sau lạm phát (V_real) = V_net ÷ (1 + i)^t\n" +
                "   • i: Tỷ lệ lạm phát Cổ phiếu (%/năm)\n" +
                "   • t: Thời gian nắm giữ (năm)"
            )
        }
    }

    item {
        GuideSectionCard(title = "3. Ví dụ thực tế minh họa", icon = Icons.Rounded.ReceiptLong) {
            ExampleCard(
                scenario = "Mua 1.000 cổ phiếu HPG giá 25.000đ/cp. Sau 2 năm giá tăng lên 30.000đ/cp. Tỷ lệ lạm phát 4%/năm.",
                steps = listOf(
                    "Tổng vốn đầu tư ban đầu = 1.000 × 25.000đ = 25.000.000đ",
                    "Doanh thu thanh lý (Gross) = 1.000 × 30.000đ = 30.000.000đ",
                    "Phí môi giới bán (0.2%) = 30.000.000đ × 0.2% = 60.000đ",
                    "Thuế TNCN bán (0.1%) = 30.000.000đ × 0.1% = 30.000đ",
                    "Giá trị ròng thực nhận (V_net) = 30.000.000đ − 90.000đ = 29.910.000đ",
                    "Lợi nhuận ròng danh nghĩa = 29.910.000đ − 25.000.000đ = +4.910.000đ (+19,64%)",
                    "Hệ số lạm phát 2 năm = (1 + 0.04)² = 1.0816",
                    "Giá trị thực sau lạm phát (V_real) = 29.910.000đ ÷ 1.0816 = 27.653.476đ",
                    "Lợi nhuận thực tế sau lạm phát = 27.653.476đ − 25.000.000đ = +2.653.476đ (+10.61%)"
                )
            )
        }
    }
}

// ==================== 2. QUỸ MỞ (CCQ) ====================
private fun androidx.compose.foundation.lazy.LazyListScope.fundGuideContent() {
    item {
        GuideIntroHeader(
            title = "Chứng chỉ quỹ mở (CCQ)",
            subtitle = "Đầu tư qua các quỹ mở hàng đầu (DCDS, VESAF, VEOF, SSISCA...)",
            icon = Icons.Rounded.PieChart,
            color = Color(0xFF0A6B5C)
        )
    }

    item {
        GuideSectionCard(title = "1. Cơ chế Hoạt động & Biểu phí Lũy thoái", icon = Icons.Rounded.Info) {
            RuleCallout("Nguyên tắc FIFO: Khi bán/chuộc lại CCQ, hệ thống tự động khớp theo từng lô mua trước xuất trước. Phí bán giảm dần theo thời gian nắm giữ của từng lô.")
            BulletPoint("Phí mua ban đầu: Thường từ 0.0% - 0.5% tùy chương trình công ty quản lý quỹ.")
            BulletPoint("Biểu phí bán lũy thoái theo thời gian:")
            BulletPoint("  • Nắm giữ < 12 tháng: Phí Tier 1 (mặc định 1.0%)")
            BulletPoint("  • Nắm giữ 12 - 24 tháng: Phí Tier 2 (mặc định 0.5%)")
            BulletPoint("  • Nắm giữ ≥ 24 tháng: Phí Tier 3 (0.0% - Miễn phí bán)")
            BulletPoint("Thuế TNCN chuyển nhượng CCQ: 0.1% doanh thu bán.")
        }
    }

    item {
        GuideSectionCard(title = "2. Công thức toán học Chi tiết", icon = Icons.Rounded.Calculate) {
            FormulaBox(
                "1. Tổng giá vốn = Số_lượng_CCQ × NAV_mua_bình_quân\n\n" +
                "2. Giá trị thị trường hiện tại (NAV) = Số_lượng_CCQ × NAV_hiện_tại\n\n" +
                "3. Phí phạt bán sớm = NAV_bán × Tỷ_lệ_phí_theo_tháng(t)\n\n" +
                "4. Thuế TNCN bán CCQ = NAV_bán × 0.1%\n\n" +
                "5. Giá trị ròng thực nhận (V_net) = NAV_bán − Phí_phạt − Thuế_TNCN\n\n" +
                "6. Giá trị thực sau lạm phát (V_real) = V_net ÷ (1 + i_fund)^t"
            )
        }
    }

    item {
        GuideSectionCard(title = "3. Ví dụ thực tế minh họa", icon = Icons.Rounded.ReceiptLong) {
            ExampleCard(
                scenario = "Mua 5.000 CCQ DCDS giá NAV 60.000đ/CCQ (Vốn 300 triệu). Sau 18 tháng NAV đạt 72.000đ/CCQ. Lạm phát quỹ 4%/năm.",
                steps = listOf(
                    "Doanh thu bán lại (Gross NAV) = 5.000 × 72.000đ = 360.000.000đ",
                    "Thời gian nắm giữ 18 tháng (mốc Tier 2: 12-24 tháng) -> Phí chuộc lại 0.5%",
                    "Phí bán CCQ = 360.000.000đ × 0.5% = 1.800.000đ",
                    "Thuế TNCN bán (0.1%) = 360.000.000đ × 0.1% = 360.000đ",
                    "Giá trị ròng thực nhận (V_net) = 360.000.000đ − 2.160.000đ = 357.840.000đ",
                    "Lợi nhuận ròng = 357.840.000đ − 300.000.000đ = +57.840.000đ (+19,28%)",
                    "Hệ số lạm phát 1.5 năm = (1 + 0.04)^1.5 = 1.0606",
                    "Giá trị thực sau lạm phát (V_real) = 357.840.000đ ÷ 1.0606 = 337.394.000đ",
                    "Lợi nhuận thực tế sau lạm phát = +37.394.000đ (+12,46%)"
                )
            )
        }
    }
}

// ==================== 3. BẤT ĐỘNG SẢN ====================
private fun androidx.compose.foundation.lazy.LazyListScope.realEstateGuideContent() {
    item {
        GuideIntroHeader(
            title = "Bất động sản (BĐS)",
            subtitle = "Căn hộ, đất nền, nhà phố và BĐS cho thuê",
            icon = Icons.Rounded.HomeWork,
            color = Color(0xFFFF7043)
        )
    }

    item {
        GuideSectionCard(title = "1. Cấu trúc Vốn & Chi phí Thanh lý BĐS", icon = Icons.Rounded.Info) {
            BulletPoint("Giá vốn ban đầu = Giá mua hợp đồng + Thuế trước bạ (0.5%) + Chi phí sửa chữa nội thất + Phí thủ tục pháp lý.")
            BulletPoint("Chi phí thanh lý bắt buộc khi chuyển nhượng:")
            BulletPoint("  • Thuế TNCN chuyển nhượng BĐS: 2.0% giá trị giao dịch")
            BulletPoint("  • Phí hoa hồng môi giới BĐS: Mặc định 1.0% (thường 1% - 2%)")
            BulletPoint("Dòng tiền thu nhập định kỳ: Tiền thuê nhà hàng tháng sau trừ phí quản lý & bảo trì.")
            BulletPoint("Tỷ suất lợi nhuận vốn tự có ROE: Đo lường hiệu quả sinh lời khi dùng đòn bẩy vay ngân hàng.")
        }
    }

    item {
        GuideSectionCard(title = "2. Công thức toán học Chi tiết", icon = Icons.Rounded.Calculate) {
            FormulaBox(
                "1. Tổng giá vốn (Total Cost) = Giá_mua + Lệ_phí + Cải_tạo\n\n" +
                "2. Thuế & Phí bán (3.0%) = Giá_thị_trường × (2.0% Thuế + 1.0% Môi_giới)\n\n" +
                "3. Giá trị ròng thực nhận (V_net) = Giá_thị_trường × 97%\n\n" +
                "4. Lãi/lỗ ròng chưa đòn bẩy = V_net − Tổng_giá_vốn\n\n" +
                "5. Giá trị thực sau lạm phát (V_real) = V_net ÷ (1 + i_bđs)^t\n\n" +
                "6. ROE đòn bẩy (%) = (Lợi_nhuận_ròng − Lãi_vay_đã_trả) ÷ Vốn_tự_có × 100%"
            )
        }
    }

    item {
        GuideSectionCard(title = "3. Ví dụ thực tế minh họa", icon = Icons.Rounded.ReceiptLong) {
            ExampleCard(
                scenario = "Mua căn hộ 3.0 tỷ, sửa chữa 200 triệu (Vốn 3.2 tỷ). Sau 3 năm định giá thị trường 4.0 tỷ. Lạm phát BĐS 4%/năm.",
                steps = listOf(
                    "Tổng vốn đầu tư ban đầu = 3.0 tỷ + 200tr = 3.200.000.000đ",
                    "Doanh thu chuyển nhượng (Gross) = 4.000.000.000đ",
                    "Thuế TNCN chuyển nhượng (2.0%) = 4.000.000.000đ × 2.0% = 80.000.000đ",
                    "Phí hoa hồng môi giới (1.0%) = 4.000.000.000đ × 1.0% = 40.000.000đ",
                    "Giá trị ròng thực nhận (V_net) = 4.0 tỷ − 120 triệu = 3.880.000.000đ",
                    "Lợi nhuận ròng danh nghĩa = 3.880.000.000đ − 3.200.000.000đ = +680.000.000đ (+21.25%)",
                    "Hệ số lạm phát 3 năm = (1 + 0.04)³ = 1.12486",
                    "Giá trị thực sau lạm phát (V_real) = 3.880.000.000đ ÷ 1.12486 = 3.449.318.000đ",
                    "Lợi nhuận thực tế sau lạm phát = +249.318.000đ (+7.79%)"
                )
            )
        }
    }
}

// ==================== 4. VÀNG & KIM LOẠI ====================
private fun androidx.compose.foundation.lazy.LazyListScope.goldGuideContent() {
    item {
        GuideIntroHeader(
            title = "Vàng & Kim loại quý",
            subtitle = "Vàng nhẫn 9999, Vàng miếng SJC và Vàng tích trữ",
            icon = Icons.Rounded.Stars,
            color = Color(0xFFE8A317)
        )
    }

    item {
        GuideSectionCard(title = "1. Nguyên tắc Định giá Mua vào tiệm (Bid Price)", icon = Icons.Rounded.Info) {
            RuleCallout("Cơ chế Bid-Ask Spread: Tiệm vàng niêm yết Giá Bán ra (giá bạn mua vào) và Giá Mua vào (giá tiệm mua lại từ bạn). MoneyLens BẮT BUỘC định giá tài sản vàng theo GIÁ MUA VÀO CỦA TIỆM.")
            BulletPoint("Loại trừ hoàn toàn ảo tưởng lợi nhuận do chênh lệch giá niêm yết.")
            BulletPoint("Miễn thuế TNCN: Theo quy định pháp luật Việt Nam hiện hành, cá nhân mua bán vàng vật chất không chịu thuế TNCN chuyển nhượng.")
            BulletPoint("Phí thanh lý = 0đ (Vì giá thu mua của tiệm đã phản ánh chi phí của tiệm).")
            BulletPoint("Đơn vị chuẩn: 1 lượng (cây) = 10 chỉ = 37.5 gram.")
        }
    }

    item {
        GuideSectionCard(title = "2. Công thức toán học Chi tiết", icon = Icons.Rounded.Calculate) {
            FormulaBox(
                "1. Tổng giá vốn = Số_chỉ × Giá_tiệm_bán_ra_lúc_mua + Phí_gia_công\n\n" +
                "2. Giá trị ròng thực nhận (V_net) = Số_chỉ × Giá_tiệm_MUA_VÀO_hiện_tại\n\n" +
                "3. Lãi/lỗ ròng = V_net − Tổng_giá_vốn\n\n" +
                "4. Giá trị thực sau lạm phát (V_real) = V_net ÷ (1 + i_gold)^t"
            )
        }
    }

    item {
        GuideSectionCard(title = "3. Ví dụ thực tế minh họa", icon = Icons.Rounded.ReceiptLong) {
            ExampleCard(
                scenario = "Mua 10 chỉ (1 lượng) vàng nhẫn giá 7.500.000đ/chỉ (Giá vốn 75 triệu). Sau 1 năm tiệm niêm yết: Bán ra 8.200.000đ - Mua vào 8.000.000đ.",
                steps = listOf(
                    "Vốn đầu tư ban đầu = 10 chỉ × 7.500.000đ = 75.000.000đ",
                    "Định giá danh mục dùng GIÁ MUA VÀO CỦA TIỆM = 8.000.000đ/chỉ",
                    "Giá trị ròng thực nhận (V_net) = 10 chỉ × 8.000.000đ = 80.000.000đ",
                    "Thuế & Phí thanh lý = 0đ",
                    "Lợi nhuận ròng danh nghĩa = 80.000.000đ − 75.000.000đ = +5.000.000đ (+6.67%)",
                    "Lạm phát vàng 4%/năm -> Hệ số = 1.04",
                    "Giá trị thực sau lạm phát (V_real) = 80.000.000đ ÷ 1.04 = 76.923.000đ",
                    "Lợi nhuận thực tế sau lạm phát = +1.923.000đ (+2,56%)"
                )
            )
        }
    }
}

// ==================== 5. CRYPTO & KHÁC ====================
private fun androidx.compose.foundation.lazy.LazyListScope.cryptoGuideContent() {
    item {
        GuideIntroHeader(
            title = "Tiền mã hóa & Tài sản khác",
            subtitle = "Bitcoin, Ethereum và các tài sản kỹ thuật số",
            icon = Icons.Rounded.CurrencyBitcoin,
            color = Color(0xFF8E24AA)
        )
    }

    item {
        GuideSectionCard(title = "1. Quy tắc Tính toán & Phí giao dịch", icon = Icons.Rounded.Info) {
            BulletPoint("Phí sàn giao dịch ước tính: Mặc định 0.1% cho các đợt khớp lệnh bán.")
            BulletPoint("Tính biến động cao: Áp dụng tỷ lệ lạm phát mục tiêu hoặc tỷ lệ trượt giá riêng để theo dõi sức mua quy đổi về VND.")
            BulletPoint("Tự động chuyển đổi tỷ giá USD/VND theo tỷ giá thị trường thời gian thực.")
        }
    }

    item {
        GuideSectionCard(title = "2. Công thức toán học Chi tiết", icon = Icons.Rounded.Calculate) {
            FormulaBox(
                "1. Tổng giá vốn (VND) = Số_lượng × Giá_vốn_unit × Tỷ_giá_lúc_mua\n\n" +
                "2. Doanh thu thị trường (Gross) = Số_lượng × Giá_hiện_tại × Tỷ_giá_hiện_tại\n\n" +
                "3. Phí giao dịch sàn (0.1%) = Gross × 0.1%\n\n" +
                "4. Giá trị ròng thực nhận (V_net) = Gross × 99.9%\n\n" +
                "5. Giá trị thực sau lạm phát (V_real) = V_net ÷ (1 + i_crypto)^t"
            )
        }
    }

    item {
        GuideSectionCard(title = "3. Ví dụ thực tế minh họa", icon = Icons.Rounded.ReceiptLong) {
            ExampleCard(
                scenario = "Mua 0.1 BTC giá 60.000 USD (Tỷ giá 25.000 VND/USD -> Vốn 150 triệu). Sau 1 năm BTC đạt 70.000 USD (Tỷ giá 25.400 VND/USD).",
                steps = listOf(
                    "Tổng giá vốn ban đầu = 0.1 × 60.000 × 25.000 = 150.000.000đ",
                    "Doanh thu thị trường (Gross) = 0.1 × 70.000 × 25.400 = 177.800.000đ",
                    "Phí sàn giao dịch (0.1%) = 177.800đ",
                    "Giá trị ròng thực nhận (V_net) = 177.622.200đ",
                    "Lợi nhuận ròng = 177.622.200đ − 150.000.000đ = +27.622.200đ (+18.41%)",
                    "Giá trị thực sau lạm phát (4%) = 177.622.200đ ÷ 1.04 = 170.790.576đ",
                    "Lợi nhuận thực tế sau lạm phát = +20.790.576đ (+13.86%)"
                )
            )
        }
    }
}

// ==================== 6. TỔNG QUAN & THUẾ PHÍ ====================
private fun androidx.compose.foundation.lazy.LazyListScope.overviewGuideContent() {
    item {
        GuideIntroHeader(
            title = "Tổng quan Danh mục & Bảng Thuế Phí Mặc định",
            subtitle = "Chuẩn hóa định giá danh mục đầu tư toàn diện",
            icon = Icons.Rounded.Analytics,
            color = Color(0xFF673AB7)
        )
    }

    item {
        GuideSectionCard(title = "1. Bảng Cấu hình Thuế & Phí Mặc định", icon = Icons.Rounded.Tune) {
            GuideTable(
                headers = listOf("Loại tài sản", "Phí / Thuế", "Mặc định", "Ghi chú áp dụng"),
                rows = listOf(
                    listOf("Cổ phiếu", "Phí môi giới GD bán", "0.2%", "Trừ trên doanh thu bán"),
                    listOf("Cổ phiếu", "Thuế TNCN bán", "0.1%", "Khấu trừ bắt buộc trên doanh thu"),
                    listOf("Cổ phiếu", "Thuế cổ tức tiền mặt", "5.0%", "Khấu trừ tại nguồn khi nhận"),
                    listOf("Quỹ CCQ", "Phí mua CCQ", "0.5%", "Tính vào giá vốn mua"),
                    listOf("Quỹ CCQ", "Thuế TNCN bán", "0.1%", "Trừ khi bán/chuộc lại"),
                    listOf("Quỹ CCQ", "Phí phạt rút <12th", "1.0%", "Mốc Tier 1"),
                    listOf("Quỹ CCQ", "Phí phạt rút 12-24th", "0.5%", "Mốc Tier 2"),
                    listOf("Quỹ CCQ", "Phí phạt rút ≥24th", "0.0%", "Miễn phí chuộc lại"),
                    listOf("Bất động sản", "Thuế TNCN chuyển nhượng", "2.0%", "Khấu trừ chuyển nhượng BĐS"),
                    listOf("Bất động sản", "Phí hoa hồng môi giới", "1.0%", "Phí môi giới khi bán"),
                    listOf("Tất cả", "Tỷ lệ Lạm phát", "4.0%/năm", "Cơ sở tính Giá trị thực V_real")
                )
            )
        }
    }

    item {
        GuideSectionCard(title = "2. Tổng hợp Danh mục & Chỉ số XIRR", icon = Icons.Rounded.DonutLarge) {
            FormulaBox(
                "Tổng giá trị ròng danh mục (Total V_net) = Σ V_net (Tất cả tài sản)\n\n" +
                "Tổng giá trị thực sau lạm phát (Total V_real) = Σ V_real (Tất cả tài sản)\n\n" +
                "Tỷ suất sinh lời ròng danh mục (%) = (Total V_net − Total Cost) ÷ Total Cost × 100%\n\n" +
                "XIRR Danh mục: Chuẩn hóa toàn bộ chuỗi dòng tiền nạp/rút theo thời gian thực."
            )
        }
    }
}

// ==================== HELPER COMPOSABLES ====================
@Composable
private fun GuideIntroHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            content()
        }
    }
}

@Composable
private fun ExampleCard(
    scenario: String,
    steps: List<String>
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "📌 Tình huống thực tế:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scenario,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "🧮 Các bước tính chi tiết:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}. ",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormulaBox(formula: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = formula,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.md)
        )
    }
}

@Composable
private fun RuleCallout(message: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GuideTable(
    headers: List<String>,
    rows: List<List<String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            headers.forEachIndexed { idx, header ->
                val weight = if (idx == 0) 1.0f else if (idx == 1) 1.2f else 0.8f
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(weight)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Table Rows
        rows.forEachIndexed { rIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (rIdx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                    .padding(vertical = 5.dp, horizontal = 8.dp)
            ) {
                row.forEachIndexed { cIdx, cell ->
                    val weight = if (cIdx == 0) 1.0f else if (cIdx == 1) 1.2f else 0.8f
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (cIdx == 0) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
    }
}
