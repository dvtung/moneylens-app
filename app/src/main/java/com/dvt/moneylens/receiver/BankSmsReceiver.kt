package com.dvt.moneylens.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.dvt.moneylens.data.local.MoneyLensDatabase
import com.dvt.moneylens.data.repository.MoneyLensRepository
import com.dvt.moneylens.domain.calc.BankParserEngine
import com.dvt.moneylens.domain.calc.RawFinancialMessage
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBody = StringBuilder()
        var sender = ""
        var timestamp = System.currentTimeMillis()

        for (sms in messages) {
            sender = sms.displayOriginatingAddress.orEmpty()
            fullBody.append(sms.displayMessageBody)
            timestamp = sms.timestampMillis
        }

        val db = MoneyLensDatabase.getInstance(context)
        val repository = MoneyLensRepository(db)

        val prefs = context.getSharedPreferences("moneylens_security_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notification_capture_enabled", true)) return

        CoroutineScope(Dispatchers.IO).launch {
            val rawMsg = RawFinancialMessage(
                senderOrPackage = sender,
                title = sender,
                body = fullBody.toString(),
                timestamp = timestamp
            )

            val enabledBankCodes = prefs.getStringSet("enabled_bank_codes", null)
            val parsed = BankParserEngine.parse(rawMsg, enabledBankCodes) ?: return@launch
            if (parsed.amountMinor < 1000L) return@launch
            Log.d("MoneyLensSms", "Parsed SMS: ${parsed.bankCode} amount=${parsed.amountMinor}")

            val matchedCard = if (parsed.cardLast4 != null) {
                db.creditCardDao().getCardByLast4(parsed.cardLast4)
            } else null
            val isCardCredit = matchedCard?.cardType == "CREDIT" || (matchedCard == null && parsed.isCreditCard)

            val txId = UUID.randomUUID().toString()
            val tx = Transaction(
                id = txId,
                accountId = matchedCard?.accountId ?: "acc_vcb",
                amountMinor = parsed.amountMinor,
                type = parsed.type,
                categoryId = null,
                note = parsed.description ?: "SMS từ $sender",
                counterparty = parsed.counterparty ?: parsed.bankCode,
                status = TransactionStatus.PENDING,
                occurredAt = timestamp,
                balanceAfterMinor = parsed.balanceAfterMinor,
                source = "SMS",
                paymentSource = if (isCardCredit) com.dvt.moneylens.domain.model.PaymentSourceType.CREDIT else com.dvt.moneylens.domain.model.PaymentSourceType.DEBIT,
                creditCardId = matchedCard?.id
            )
            repository.addTransaction(tx)

            // Bắn thông báo mở rộng lên Notification Panel với Action Buttons
            com.dvt.moneylens.service.BankMovementNotifier.notifyMovement(
                context = context,
                txId = txId,
                bank = parsed.bankCode,
                amountMinor = parsed.amountMinor,
                type = parsed.type,
                note = parsed.description ?: "SMS từ $sender",
                categoryName = null,
                balanceAfterMinor = parsed.balanceAfterMinor,
                paymentSource = tx.paymentSource,
                cardLast4 = parsed.cardLast4 ?: matchedCard?.last4
            )
        }
    }
}
