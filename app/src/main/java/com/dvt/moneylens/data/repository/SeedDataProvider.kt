package com.dvt.moneylens.data.repository

import com.dvt.moneylens.domain.model.*
import java.util.UUID

object SeedDataProvider {

    fun getDefaultCategories(): List<Category> = listOf(
        Category("cat_food", "Ăn uống", "restaurant", "#E65100", TransactionType.EXPENSE),
        Category("cat_coffee", "Cà phê & Trà", "local_cafe", "#6D4C41", TransactionType.EXPENSE),
        Category("cat_transport", "Di chuyển & Xăng", "directions_car", "#1E88E5", TransactionType.EXPENSE),
        Category("cat_shopping", "Mua sắm & Gia dụng", "shopping_bag", "#8E24AA", TransactionType.EXPENSE),
        Category("cat_grocery", "Đi chợ & Siêu thị", "shopping_cart", "#43A047", TransactionType.EXPENSE),
        Category("cat_bills", "Hóa đơn & Tiện ích", "receipt_long", "#F4511E", TransactionType.EXPENSE),
        Category("cat_health", "Y tế & Sức khỏe", "medical_services", "#E53935", TransactionType.EXPENSE),
        Category("cat_transfer_out", "Chuyển tiền", "swap_horiz", "#5B6B8C", TransactionType.EXPENSE),
        Category("cat_credit_pay", "Thanh toán Credit", "credit_card", "#3949AB", TransactionType.EXPENSE),
        Category("cat_salary", "Tiền lương", "payments", "#12794F", TransactionType.INCOME),
        Category("cat_transfer_in", "Chuyển tiền", "swap_horiz", "#5B6B8C", TransactionType.INCOME),
        Category("cat_bonus", "Thưởng & Khác", "redeem", "#00897B", TransactionType.INCOME),
        Category("cat_dividend", "Cổ tức & Thu nhập đầu tư", "trending_up", "#0A6B5C", TransactionType.INCOME),
        Category("cat_investment", "Tích sản & Đầu tư", "account_balance", "#00897B", TransactionType.EXPENSE),
        Category("cat_transfer", "Chuyển khoản nội bộ", "swap_horiz", "#5B6B8C", TransactionType.TRANSFER)
    )

    fun getDefaultMembers(): List<Member> = listOf(
        Member("mem_self", "Tôi", MemberRole.SELF, "#0A6B5C"),
        Member("mem_spouse", "Vợ / Chồng", MemberRole.SPOUSE, "#8E24AA"),
        Member("mem_child", "Con cái", MemberRole.CHILD, "#1E88E5")
    )

    fun getDefaultAccounts(): List<Account> = listOf(
        Account(
            id = "acc_vcb",
            name = "Vietcombank Digibank",
            type = AccountType.BANK,
            balanceMinor = 24_580_000L,
            bankCode = "VCB",
            ownerMemberId = "mem_self"
        ),
        Account(
            id = "acc_tcb",
            name = "Techcombank",
            type = AccountType.BANK,
            balanceMinor = 8_250_000L,
            bankCode = "TCB",
            ownerMemberId = "mem_self"
        ),
        Account(
            id = "acc_cash",
            name = "Tiền mặt trong ví",
            type = AccountType.CASH,
            balanceMinor = 1_850_000L,
            ownerMemberId = "mem_self"
        ),
        Account(
            id = "acc_momo",
            name = "Ví điện tử MoMo",
            type = AccountType.EWALLET,
            balanceMinor = 920_000L,
            bankCode = "MOMO",
            ownerMemberId = "mem_self"
        ),
        Account(
            id = "acc_vpbank_card",
            name = "Thẻ VPBank Mastercard",
            type = AccountType.CREDIT_CARD,
            balanceMinor = -9_800_000L,
            bankCode = "VPB",
            ownerMemberId = "mem_self"
        ),
        Account(
            id = "acc_tcb_card",
            name = "Thẻ Techcombank Visa",
            type = AccountType.CREDIT_CARD,
            balanceMinor = -8_650_000L,
            bankCode = "TCB",
            ownerMemberId = "mem_self"
        )
    )

    fun getDefaultCreditCards(): List<CreditCard> = listOf(
        CreditCard(
            id = "card_vpb_stepup",
            accountId = "acc_vpbank_card",
            nickname = "VPBank StepUp",
            issuerBank = "VPBank",
            network = CardNetwork.MASTERCARD,
            last4 = "5678",
            creditLimitMinor = 60_000_000L,
            statementDay = 15,
            dueDaysAfterStatement = 20, // Due on 5th next month
            minPaymentPercent = 0.05,
            minPaymentFloorMinor = 200_000L,
            currentBalanceOwedMinor = 9_800_000L,
            annualFeeMinor = 499_000L,
            colorHex = "#E8A317",
            ownerMemberId = "mem_self"
        ),
        CreditCard(
            id = "card_tcb_signature",
            accountId = "acc_tcb_card",
            nickname = "Techcombank Visa Signature",
            issuerBank = "Techcombank",
            network = CardNetwork.VISA,
            last4 = "1234",
            creditLimitMinor = 100_000_000L,
            statementDay = 25,
            dueDaysAfterStatement = 20,
            minPaymentPercent = 0.05,
            minPaymentFloorMinor = 250_000L,
            currentBalanceOwedMinor = 8_650_000L,
            annualFeeMinor = 899_000L,
            colorHex = "#0A6B5C",
            ownerMemberId = "mem_self"
        )
    )

    fun getDefaultBudgets(): List<Budget> = listOf(
        Budget("b_food", "cat_food", 6_000_000L, "2026-10"),
        Budget("b_coffee", "cat_coffee", 1_500_000L, "2026-10"),
        Budget("b_transport", "cat_transport", 2_000_000L, "2026-10"),
        Budget("b_grocery", "cat_grocery", 4_500_000L, "2026-10"),
        Budget("b_shopping", "cat_shopping", 2_500_000L, "2026-10")
    )

    fun getDefaultAssets(): List<Asset> = listOf(
        Asset(
            id = "ast_fpt",
            type = AssetType.STOCK,
            symbol = "FPT",
            name = "Công ty CP FPT",
            quantity = 500.0,
            currentPriceMinor = 135_000L,
            costBasisMinor = 118_000L,
            notes = "Cổ phiếu công nghệ tăng trưởng"
        ),
        Asset(
            id = "ast_hpg",
            type = AssetType.STOCK,
            symbol = "HPG",
            name = "Tập đoàn Hòa Phát",
            quantity = 1500.0,
            currentPriceMinor = 28_500L,
            costBasisMinor = 26_200L,
            notes = "Cổ phiếu thép & vật liệu xây dựng"
        ),
        Asset(
            id = "ast_sjc",
            type = AssetType.GOLD,
            symbol = "SJC",
            name = "Vàng miếng SJC 9999",
            quantity = 2.0, // 2 lượng
            currentPriceMinor = 84_500_000L,
            costBasisMinor = 79_000_000L,
            notes = "Tích sản vàng phòng thủ"
        ),
        Asset(
            id = "ast_btc",
            type = AssetType.CRYPTO,
            symbol = "BTC",
            name = "Bitcoin",
            quantity = 0.05,
            currentPriceMinor = 1_680_000_000L,
            costBasisMinor = 1_520_000_000L,
            notes = "Lưu trữ trên ví lạnh cá nhân"
        ),
        Asset(
            id = "ast_dcvfm",
            type = AssetType.FUND,
            symbol = "DCDS",
            name = "Quỹ Năng Động DCVFM",
            quantity = 1200.0,
            currentPriceMinor = 78_400L,
            costBasisMinor = 71_000L,
            notes = "Kế hoạch đầu tư định kỳ SIP hàng tháng"
        ),
        Asset(
            id = "ast_bds_canho",
            type = AssetType.REAL_ESTATE,
            symbol = "BĐS-CANHO",
            name = "Căn hộ Vinhomes Smart City",
            quantity = 1.0,
            currentPriceMinor = 2_850_000_000L,
            costBasisMinor = 2_600_000_000L,
            notes = "Căn 2PN+1 đang cho thuê 11tr/tháng"
        )
    )

    fun getDefaultInvestmentTransactions(): List<InvestmentTransaction> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            InvestmentTransaction(
                id = "inv_tx_fpt_1",
                assetId = "ast_fpt",
                symbol = "FPT",
                action = InvestmentAction.BUY,
                quantity = 300.0,
                priceMinor = 115_000L,
                totalAmountMinor = 34_500_000L,
                accountId = "acc_vcb",
                accountName = "Vietcombank Priority",
                note = "Mua gom đợt 1 tích sản dài hạn",
                timestamp = now - 45 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_fpt_2",
                assetId = "ast_fpt",
                symbol = "FPT",
                action = InvestmentAction.BUY,
                quantity = 200.0,
                priceMinor = 122_500L,
                totalAmountMinor = 24_500_000L,
                accountId = "acc_vcb",
                accountName = "Vietcombank Priority",
                note = "Mua thêm khi giá điều chỉnh",
                timestamp = now - 15 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_hpg_1",
                assetId = "ast_hpg",
                symbol = "HPG",
                action = InvestmentAction.BUY,
                quantity = 1000.0,
                priceMinor = 25_500L,
                totalAmountMinor = 25_500_000L,
                accountId = "acc_tcb",
                accountName = "Techcombank Lương",
                note = "Mua tích lũy vùng hỗ trợ",
                timestamp = now - 60 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_hpg_2",
                assetId = "ast_hpg",
                symbol = "HPG",
                action = InvestmentAction.BUY,
                quantity = 500.0,
                priceMinor = 27_600L,
                totalAmountMinor = 13_800_000L,
                accountId = "acc_tcb",
                accountName = "Techcombank Lương",
                note = "Mua thêm quý 3/2026",
                timestamp = now - 20 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_sjc_1",
                assetId = "ast_sjc",
                symbol = "SJC",
                action = InvestmentAction.BUY,
                quantity = 2.0,
                priceMinor = 79_000_000L,
                totalAmountMinor = 158_000_000L,
                accountId = "acc_cash",
                accountName = "Tiền mặt trong ví",
                note = "Mua 2 lượng vàng miếng tích trữ",
                timestamp = now - 90 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_btc_1",
                assetId = "ast_btc",
                symbol = "BTC",
                action = InvestmentAction.BUY,
                quantity = 0.05,
                priceMinor = 1_520_000_000L,
                totalAmountMinor = 76_000_000L,
                accountId = "acc_vcb",
                accountName = "Vietcombank Priority",
                note = "DCA Bitcoin định kỳ",
                timestamp = now - 40 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_dcds_1",
                assetId = "ast_dcvfm",
                symbol = "DCDS",
                action = InvestmentAction.BUY,
                quantity = 600.0,
                priceMinor = 70_000L,
                totalAmountMinor = 42_000_000L,
                accountId = "acc_tcb",
                accountName = "Techcombank Lương",
                note = "Đầu tư SIP định kỳ kỳ 1",
                timestamp = now - 60 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_dcds_2",
                assetId = "ast_dcvfm",
                symbol = "DCDS",
                action = InvestmentAction.BUY,
                quantity = 600.0,
                priceMinor = 72_000L,
                totalAmountMinor = 43_200_000L,
                accountId = "acc_tcb",
                accountName = "Techcombank Lương",
                note = "Đầu tư SIP định kỳ kỳ 2",
                timestamp = now - 30 * day
            ),
            InvestmentTransaction(
                id = "inv_tx_bds_1",
                assetId = "ast_bds_canho",
                symbol = "BĐS-CANHO",
                action = InvestmentAction.BUY,
                quantity = 1.0,
                priceMinor = 2_600_000_000L,
                totalAmountMinor = 2_600_000_000L,
                accountId = "acc_vcb",
                accountName = "Vietcombank Priority",
                note = "Mua nhận bàn giao căn hộ 2PN+1",
                timestamp = now - 180 * day
            )
        )
    }

    fun getDefaultTransactions(): List<Transaction> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            // Confirmed Recent Transactions (Hôm nay & các ngày trước)
            Transaction(
                id = "tx_1",
                accountId = "acc_vcb",
                amountMinor = 65_000L,
                type = TransactionType.EXPENSE,
                categoryId = "cat_coffee",
                note = "Cà phê Highlands Landmark",
                counterparty = "Highlands Coffee",
                status = TransactionStatus.CONFIRMED,
                occurredAt = now - 5 * 3600 * 1000L,
                memberId = "mem_self"
            ),
            Transaction(
                id = "tx_2",
                accountId = "acc_tcb",
                amountMinor = 480_000L,
                type = TransactionType.EXPENSE,
                categoryId = "cat_grocery",
                note = "Mua thực phẩm tuần",
                counterparty = "WinMart+ Vincom",
                status = TransactionStatus.CONFIRMED,
                occurredAt = now - 1 * day,
                memberId = "mem_self"
            ),
            Transaction(
                id = "tx_3",
                accountId = "acc_vcb",
                amountMinor = 35_000_000L,
                type = TransactionType.INCOME,
                categoryId = "cat_salary",
                note = "Lương tháng 09/2026",
                counterparty = "CTY CP CONG NGHE FINTECH",
                status = TransactionStatus.CONFIRMED,
                occurredAt = now - 4 * day,
                balanceAfterMinor = 38_500_000L,
                memberId = "mem_self"
            ),
            Transaction(
                id = "tx_4",
                accountId = "acc_vpbank_card",
                amountMinor = 1_250_000L,
                type = TransactionType.EXPENSE,
                categoryId = "cat_shopping",
                note = "Quần áo Uniqlo",
                counterparty = "UNIQLO VIETNAM",
                status = TransactionStatus.CONFIRMED,
                occurredAt = now - 3 * day,
                memberId = "mem_self"
            ),
            Transaction(
                id = "tx_5",
                accountId = "acc_vcb",
                amountMinor = 5_000_000L,
                type = TransactionType.TRANSFER,
                categoryId = "cat_transfer",
                note = "Chuyển tiền vào tài khoản Techcombank dự phòng",
                counterparty = "TK Techcombank",
                status = TransactionStatus.CONFIRMED,
                occurredAt = now - 5 * day,
                memberId = "mem_self"
            ),
            Transaction(
                id = "tx_6",
                accountId = "acc_tcb",
                amountMinor = 1_450_000L,
                type = TransactionType.EXPENSE,
                categoryId = "cat_bills",
                note = "Tiền điện EVN T9",
                counterparty = "EVN HCMC",
                status = TransactionStatus.CONFIRMED,
                occurredAt = now - 6 * day,
                memberId = "mem_self"
            ),

            // Dữ liệu lịch sử các tháng trước của năm 2026
            // Tháng 9
            Transaction("tx_h_9_inc", "acc_vcb", 35_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 08/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 35 * day, memberId = "mem_self"),
            Transaction("tx_h_9_exp", "acc_vcb", 18_200_000L, TransactionType.EXPENSE, "cat_bills", "Tổng chi tiêu sinh hoạt T9", "Chi tiêu gia đình", TransactionStatus.CONFIRMED, now - 34 * day, memberId = "mem_self"),

            // Tháng 8
            Transaction("tx_h_8_inc", "acc_vcb", 32_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 07/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 65 * day, memberId = "mem_self"),
            Transaction("tx_h_8_exp", "acc_vcb", 20_500_000L, TransactionType.EXPENSE, "cat_shopping", "Mua sắm & du lịch hè T8", "Du lịch hè", TransactionStatus.CONFIRMED, now - 64 * day, memberId = "mem_self"),

            // Tháng 7
            Transaction("tx_h_7_inc", "acc_vcb", 30_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 06/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 95 * day, memberId = "mem_self"),
            Transaction("tx_h_7_exp", "acc_vcb", 17_800_000L, TransactionType.EXPENSE, "cat_bills", "Chi tiêu sinh hoạt T7", "Sinh hoạt", TransactionStatus.CONFIRMED, now - 94 * day, memberId = "mem_self"),

            // Tháng 6
            Transaction("tx_h_6_inc", "acc_vcb", 30_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 05/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 125 * day, memberId = "mem_self"),
            Transaction("tx_h_6_exp", "acc_vcb", 19_200_000L, TransactionType.EXPENSE, "cat_food", "Chi tiêu ăn uống & nhà cửa T6", "Gia đình", TransactionStatus.CONFIRMED, now - 124 * day, memberId = "mem_self"),

            // Tháng 5
            Transaction("tx_h_5_inc", "acc_vcb", 28_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 04/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 155 * day, memberId = "mem_self"),
            Transaction("tx_h_5_exp", "acc_vcb", 16_500_000L, TransactionType.EXPENSE, "cat_bills", "Chi tiêu sinh hoạt T5", "Sinh hoạt", TransactionStatus.CONFIRMED, now - 154 * day, memberId = "mem_self"),

            // Tháng 4
            Transaction("tx_h_4_inc", "acc_vcb", 28_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 03/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 185 * day, memberId = "mem_self"),
            Transaction("tx_h_4_exp", "acc_vcb", 15_800_000L, TransactionType.EXPENSE, "cat_grocery", "Thực phẩm & tiêu dùng T4", "Siêu thị", TransactionStatus.CONFIRMED, now - 184 * day, memberId = "mem_self"),

            // Tháng 3
            Transaction("tx_h_3_inc", "acc_vcb", 28_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 02/2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 215 * day, memberId = "mem_self"),
            Transaction("tx_h_3_exp", "acc_vcb", 16_200_000L, TransactionType.EXPENSE, "cat_bills", "Sinh hoạt phí T3", "Sinh hoạt", TransactionStatus.CONFIRMED, now - 214 * day, memberId = "mem_self"),

            // Tháng 2
            Transaction("tx_h_2_inc", "acc_vcb", 45_000_000L, TransactionType.INCOME, "cat_salary", "Lương & Thưởng Tết 2026", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 245 * day, memberId = "mem_self"),
            Transaction("tx_h_2_exp", "acc_vcb", 26_000_000L, TransactionType.EXPENSE, "cat_shopping", "Chi tiêu sắm Tết & biếu tặng T2", "Tết 2026", TransactionStatus.CONFIRMED, now - 244 * day, memberId = "mem_self"),

            // Tháng 1
            Transaction("tx_h_1_inc", "acc_vcb", 28_000_000L, TransactionType.INCOME, "cat_salary", "Lương tháng 12/2025", "CTY CP FINTECH", TransactionStatus.CONFIRMED, now - 275 * day, memberId = "mem_self"),
            Transaction("tx_h_1_exp", "acc_vcb", 15_000_000L, TransactionType.EXPENSE, "cat_bills", "Sinh hoạt phí T1", "Sinh hoạt", TransactionStatus.CONFIRMED, now - 274 * day, memberId = "mem_self")
        )
    }

    fun getDefaultLoans(): List<Loan> = listOf(
        Loan(
            id = "loan_car_vcb",
            title = "Vay mua ô tô Mazda 3",
            counterparty = "Vietcombank",
            type = LoanType.BORROWING,
            originalAmountMinor = 350_000_000L,
            remainingPrincipalMinor = 210_000_000L,
            interestRate = 8.9,
            interestType = InterestType.PERCENT_YEAR,
            termMonths = 36,
            monthlyPaymentMinor = 11_300_000L,
            startDate = System.currentTimeMillis() - 14 * 30 * 86_400_000L,
            dueDayOfMonth = 10,
            status = LoanStatus.ACTIVE,
            notes = "Hợp đồng tín dụng số VCB-AUTO-2024",
            linkedAccountId = "acc_vcb",
            colorHex = "#0A6B5C"
        ),
        Loan(
            id = "loan_iphone_zero",
            title = "Trả góp iPhone 16 Pro Max 256GB",
            counterparty = "Home Credit (0% Lãi)",
            type = LoanType.BORROWING,
            originalAmountMinor = 34_990_000L,
            remainingPrincipalMinor = 17_495_000L,
            interestRate = 0.0,
            interestType = InterestType.ZERO_PERCENT,
            termMonths = 12,
            monthlyPaymentMinor = 2_915_000L,
            startDate = System.currentTimeMillis() - 6 * 30 * 86_400_000L,
            dueDayOfMonth = 20,
            status = LoanStatus.ACTIVE,
            notes = "Trả góp 0% kỳ hạn 12 tháng qua thẻ Techcombank",
            linkedAccountId = "acc_tcb_card",
            colorHex = "#E8A317",
            conversionFeeMinor = 1_200_000L,
            conversionFeeSchedule = com.dvt.moneylens.domain.model.ConversionFeeSchedule.FIRST_MONTH,
            conversionFeePercent = 3.43
        ),
        Loan(
            id = "loan_lend_friend",
            title = "Cho anh Nam mượn vốn kinh doanh",
            counterparty = "Nguyễn Văn Nam (Bạn thân)",
            type = LoanType.LENDING,
            originalAmountMinor = 50_000_000L,
            remainingPrincipalMinor = 30_000_000L,
            interestRate = 0.0,
            interestType = InterestType.NO_INTEREST,
            termMonths = 6,
            monthlyPaymentMinor = 10_000_000L,
            startDate = System.currentTimeMillis() - 2 * 30 * 86_400_000L,
            dueDayOfMonth = 28,
            status = LoanStatus.ACTIVE,
            notes = "Hẹn trả 10tr/tháng vào ngày 28",
            linkedAccountId = "acc_vcb",
            colorHex = "#1E88E5"
        )
    )

    fun getDefaultLoanPayments(): List<LoanPayment> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            LoanPayment(
                id = "lp_car_1",
                loanId = "loan_car_vcb",
                amountMinor = 11_300_000L,
                principalMinor = 9_700_000L,
                interestMinor = 1_600_000L,
                paidAt = now - 60 * day,
                note = "Trả nợ kỳ tháng 8/2026 qua VCB Digibank"
            ),
            LoanPayment(
                id = "lp_car_2",
                loanId = "loan_car_vcb",
                amountMinor = 11_300_000L,
                principalMinor = 9_800_000L,
                interestMinor = 1_500_000L,
                paidAt = now - 30 * day,
                note = "Trả nợ kỳ tháng 9/2026 qua VCB Digibank"
            ),
            LoanPayment(
                id = "lp_iphone_1",
                loanId = "loan_iphone_zero",
                amountMinor = 4_115_000L,
                principalMinor = 2_915_000L,
                interestMinor = 0L,
                paidAt = now - 90 * day,
                note = "Kỳ 1 kèm phí chuyển đổi trả góp 1.200.000đ"
            ),
            LoanPayment(
                id = "lp_iphone_2",
                loanId = "loan_iphone_zero",
                amountMinor = 2_915_000L,
                principalMinor = 2_915_000L,
                interestMinor = 0L,
                paidAt = now - 60 * day,
                note = "Kỳ 2 trả góp 0% qua Techcombank"
            ),
            LoanPayment(
                id = "lp_iphone_3",
                loanId = "loan_iphone_zero",
                amountMinor = 2_915_000L,
                principalMinor = 2_915_000L,
                interestMinor = 0L,
                paidAt = now - 30 * day,
                note = "Kỳ 3 trả góp 0% qua Techcombank"
            ),
            LoanPayment(
                id = "lp_lend_1",
                loanId = "loan_lend_friend",
                amountMinor = 10_000_000L,
                principalMinor = 10_000_000L,
                interestMinor = 0L,
                paidAt = now - 30 * day,
                note = "Anh Nam chuyển khoản trả đợt 1 vào VCB"
            ),
            LoanPayment(
                id = "lp_lend_2",
                loanId = "loan_lend_friend",
                amountMinor = 10_000_000L,
                principalMinor = 10_000_000L,
                interestMinor = 0L,
                paidAt = now - 2 * day,
                note = "Anh Nam chuyển khoản trả đợt 2 vào VCB"
            )
        )
    }
}
