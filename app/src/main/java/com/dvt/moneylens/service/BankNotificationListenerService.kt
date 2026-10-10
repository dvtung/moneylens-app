package com.dvt.moneylens.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dvt.moneylens.data.local.MoneyLensDatabase
import com.dvt.moneylens.data.repository.MoneyLensRepository
import com.dvt.moneylens.domain.calc.BankPackageRegistry
import com.dvt.moneylens.domain.calc.BankParserEngine
import com.dvt.moneylens.domain.calc.RawFinancialMessage
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionStatus
import com.dvt.moneylens.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: MoneyLensDatabase
    private lateinit var repository: MoneyLensRepository

    companion object {
        private const val TAG = "MoneyLensCapture"
        // Cache chống xử lý trùng lặp cùng một thông báo trong 60 giây
        private val recentNotificationHashes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }

    override fun onCreate() {
        super.onCreate()
        db = MoneyLensDatabase.getInstance(applicationContext)
        repository = MoneyLensRepository(db)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val sbnNonNull = sbn ?: return
        val packageName = sbnNonNull.packageName.orEmpty()

        // 1. CHẶN VÒNG LẶP TUYỆT ĐỐI: Bỏ qua 100% thông báo do chính app MoneyLens tạo ra
        if (packageName == applicationContext.packageName ||
            packageName == "com.dvt.moneylens" ||
            packageName.contains("moneylens") ||
            sbnNonNull.notification.channelId == "bank_movement_channel"
        ) {
            return
        }

        // 2. Bỏ qua thông báo ongoing (đang phát nhạc, download nền, pin...) hoặc không thể xóa
        if (sbnNonNull.isOngoing || !sbnNonNull.isClearable) {
            return
        }

        // 3. Bỏ qua các thông báo hệ điều hành Android / SystemUI
        if (packageName.startsWith("com.android.") || packageName == "android") {
            return
        }

        // 4. Kiểm tra công tắc người dùng trong Cài đặt (Notification Capture Toggle)
        val prefs = applicationContext.getSharedPreferences("moneylens_security_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notification_capture_enabled", true)) {
            return
        }

        val extras = sbnNonNull.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (text.isBlank() && title.isBlank()) return

        // 5. Chống trùng lặp (Deduplication trong 60 giây)
        val notifKey = "$packageName|$title|$text|${sbnNonNull.postTime / 10000}"
        val now = System.currentTimeMillis()
        recentNotificationHashes.entries.removeIf { now - it.value > 60_000L }
        if (recentNotificationHashes.putIfAbsent(notifKey, now) != null) {
            return // Thông báo đã được nhận diện gần đây
        }

        // 6. Kiểm tra nhanh định danh gói ứng dụng qua BankPackageRegistry
        val isKnownBank = BankPackageRegistry.isKnownBankPackage(packageName)
        val isSms = BankPackageRegistry.isSmsPackage(packageName)
        if (!isKnownBank && !isSms) {
            val quickText = (title + " " + text).lowercase()
            val hasFinancialTrigger = quickText.contains("biến động") || quickText.contains("số dư") ||
                    quickText.contains("gd:") || quickText.contains("tài khoản") || quickText.contains("thanh toán")
            if (!hasFinancialTrigger) {
                return // Không phải ngân hàng và không có nội dung tài chính -> Bỏ qua ngay
            }
        }

        serviceScope.launch {
            val rawMsg = RawFinancialMessage(
                senderOrPackage = packageName,
                title = title,
                body = text,
                timestamp = sbnNonNull.postTime
            )

            val enabledBankCodes = prefs.getStringSet("enabled_bank_codes", null)
            val parsed = BankParserEngine.parse(rawMsg, enabledBankCodes) ?: return@launch

            // Bảo vệ thêm: Tuyệt đối không lưu giao dịch có số tiền < 1.000 VNĐ
            if (parsed.amountMinor < 1000L) {
                return@launch
            }

            Log.d(TAG, "Parsed notification: ${parsed.bankCode} ${parsed.type} (amount: ${parsed.amountMinor}, confidence ${parsed.confidence})")

            val allCards = db.creditCardDao().getCardsList()
            val allAccounts = db.accountDao().getAccountsList()

            // 1. Khớp thẻ / tài khoản / ví theo accountHint hoặc cardLast4
            val hint = parsed.cardLast4 ?: parsed.accountHint
            var matchedCard = if (!hint.isNullOrBlank()) {
                allCards.find { card ->
                    card.last4.isNotBlank() && (
                        card.last4.takeLast(4) == hint.takeLast(4) ||
                        card.last4.endsWith(hint) ||
                        hint.endsWith(card.last4)
                    )
                }
            } else null

            // 2. Nếu chưa khớp theo số thẻ/số TK, tìm theo mã ngân hàng hoặc tên ví điện tử
            if (matchedCard == null && parsed.bankCode.isNotBlank()) {
                matchedCard = allCards.find { card ->
                    card.issuerBank.equals(parsed.bankCode, ignoreCase = true) ||
                    (card.cardType == "WALLET" && (
                        card.nickname.contains(parsed.bankCode, ignoreCase = true) ||
                        card.issuerBank.contains(parsed.bankCode, ignoreCase = true)
                    ))
                }
            }

            // 3. Khớp trực tiếp vào danh sách tài khoản
            val matchedAccount = if (matchedCard != null) {
                db.accountDao().getAccountById(matchedCard.accountId)
            } else {
                allAccounts.find { acc ->
                    acc.bankCode.equals(parsed.bankCode, ignoreCase = true) ||
                    acc.name.contains(parsed.bankCode, ignoreCase = true)
                } ?: allAccounts.firstOrNull { it.type != "CREDIT_CARD" } ?: allAccounts.firstOrNull()
            }

            val finalAccountId = matchedAccount?.id ?: matchedCard?.accountId ?: "acc_vcb"
            val isCardCredit = matchedCard?.cardType == "CREDIT" || (matchedCard == null && parsed.isCreditCard)

            // Kiểm tra biến động thu có khớp với ví hoặc tài khoản đang có trong hệ thống không
            val allCategories = db.categoryDao().getCategoriesList()
            val isIncomeMatched = parsed.type == TransactionType.INCOME && (
                (matchedCard != null && (matchedCard.cardType == "WALLET" || matchedCard.cardType == "BANK_ACCOUNT" || matchedCard.cardType == "DEBIT")) ||
                (matchedAccount != null && allCards.any { it.accountId == matchedAccount.id })
            )

            val txId = UUID.randomUUID().toString()
            val defaultIncomeCatId = allCategories.firstOrNull { it.type == "INCOME" }?.id
            val tx = Transaction(
                id = txId,
                accountId = finalAccountId,
                amountMinor = parsed.amountMinor,
                type = parsed.type,
                categoryId = if (isIncomeMatched) defaultIncomeCatId else null,
                note = parsed.description ?: title,
                counterparty = parsed.counterparty ?: parsed.bankCode,
                status = if (isIncomeMatched) TransactionStatus.CONFIRMED else TransactionStatus.PENDING,
                occurredAt = sbnNonNull.postTime,
                balanceAfterMinor = parsed.balanceAfterMinor,
                source = "NOTIFICATION",
                paymentSource = if (isCardCredit) com.dvt.moneylens.domain.model.PaymentSourceType.CREDIT else com.dvt.moneylens.domain.model.PaymentSourceType.DEBIT,
                creditCardId = if (isCardCredit) matchedCard?.id else (matchedCard?.id)
            )
            repository.addTransaction(tx)

            val displayBankOrWallet = matchedCard?.nickname ?: matchedAccount?.name ?: parsed.bankCode

            // Bắn thông báo mở rộng lên Notification Panel với Action Buttons
            BankMovementNotifier.notifyMovement(
                context = applicationContext,
                txId = txId,
                bank = displayBankOrWallet,
                amountMinor = parsed.amountMinor,
                type = parsed.type,
                note = parsed.description ?: title,
                categoryName = null,
                balanceAfterMinor = parsed.balanceAfterMinor,
                paymentSource = tx.paymentSource,
                cardLast4 = parsed.cardLast4 ?: matchedCard?.last4
            )
        }
    }
}
