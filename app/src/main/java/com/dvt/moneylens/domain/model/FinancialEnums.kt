package com.dvt.moneylens.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

enum class TransactionStatus {
    PENDING,
    CONFIRMED
}

enum class AccountType {
    CASH,
    BANK,
    EWALLET,
    CREDIT_CARD,
    BROKERAGE
}

enum class CardNetwork(val label: String) {
    VISA("VISA"),
    MASTERCARD("MASTER"),
    JCB("JCB"),
    AMEX("AMEX"),
    NAPAS("NAPAS"),
    OTHER("KHÁC")
}

enum class CardType(val label: String) {
    CREDIT("Thẻ tín dụng"),
    DEBIT("Thẻ Debit"),
    WALLET("Ví điện tử"),
    BANK_ACCOUNT("Tài khoản")
}

enum class StatementStatus {
    OPEN,
    PAID_MIN,
    PAID_FULL,
    OVERDUE
}

enum class AssetType {
    STOCK,
    CRYPTO,
    GOLD,
    FUND,
    REAL_ESTATE
}

enum class MemberRole {
    SELF,
    SPOUSE,
    CHILD,
    OTHER
}

enum class InvestmentAction {
    BUY,
    SELL,
    DIVIDEND,
    VALUATION
}

enum class LoanType {
    BORROWING, // Đi vay / Mua trả góp (Phải trả)
    LENDING    // Cho vay (Phải thu hồi)
}

enum class InterestType {
    PERCENT_YEAR,   // Lãi suất %/năm
    ZERO_PERCENT,   // Trả góp 0%
    FIXED_INTEREST, // Lãi cố định trọn gói
    NO_INTEREST     // Không tính lãi (Vay mượn hỗ trợ)
}

enum class LoanStatus {
    ACTIVE,   // Đang vay/Cho vay
    PAID_OFF, // Đã tất toán hoàn toàn
    OVERDUE   // Quá hạn thanh toán
}

enum class ConversionFeeSchedule(val label: String) {
    FIRST_MONTH("Thu 1 lần vào tháng đầu"),
    SPREAD_MONTHLY("Chia đều các tháng")
}

enum class PaymentSourceType(val label: String) {
    DEBIT("Debit (Tài khoản / Tiền mặt)"),
    CREDIT("Credit (Thẻ tín dụng)")
}


