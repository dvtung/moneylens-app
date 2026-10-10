package com.dvt.moneylens.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dvt.moneylens.MainActivity
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.domain.model.PaymentSourceType
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.receiver.QuickConfirmReceiver

/**
 * Quản lý thông báo biến động số dư trên Notification Panel.
 * Hỗ trợ xem chi tiết mở rộng (BigTextStyle) và hành động xác nhận nhanh / duyệt chi tiết.
 * Thông báo lưu cho đến khi người dùng xác nhận hoặc huỷ.
 */
object BankMovementNotifier {

    private const val CHANNEL_ID = "bank_movement_channel"
    private const val CHANNEL_NAME = "Biến động Số dư & Chi tiêu"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Thông báo biến động số dư và giao dịch thẻ ngân hàng"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Bắn thông báo biến động số dư lên thanh thông báo hệ thống kèm Action buttons
     */
    fun notifyMovement(
        context: Context,
        txId: String,
        bank: String,
        amountMinor: Long,
        type: TransactionType,
        note: String,
        categoryName: String? = null,
        balanceAfterMinor: Long? = null,
        paymentSource: PaymentSourceType = PaymentSourceType.DEBIT,
        cardLast4: String? = null
    ) {
        createNotificationChannel(context)

        val notificationId = (txId.hashCode() and 0x7FFFFFFF)

        // 1. Intent khi người dùng bấm vào thân thông báo -> Mở app vào modal duyệt chi tiết
        val openDetailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_REVIEW_TX_ID, txId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openDetailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Xác nhận nhanh (BroadcastReceiver)
        val quickConfirmIntent = Intent(context, QuickConfirmReceiver::class.java).apply {
            action = QuickConfirmReceiver.ACTION_QUICK_CONFIRM
            putExtra(QuickConfirmReceiver.EXTRA_TX_ID, txId)
            putExtra(QuickConfirmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val quickConfirmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            quickConfirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Mở modal duyệt chi tiết & phân loại
        val reviewDetailPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 2,
            openDetailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Action: Xóa nhanh giao dịch khỏi danh sách
        val quickDeleteIntent = Intent(context, QuickConfirmReceiver::class.java).apply {
            action = QuickConfirmReceiver.ACTION_QUICK_DELETE
            putExtra(QuickConfirmReceiver.EXTRA_TX_ID, txId)
            putExtra(QuickConfirmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val quickDeletePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 3,
            quickDeleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isIncome = type == TransactionType.INCOME
        val isCredit = paymentSource == PaymentSourceType.CREDIT || cardLast4 != null
        val sign = if (isIncome) "+" else "-"
        val formattedAmount = Money(amountMinor).formatVnd()
        val title = when {
            isIncome -> "💰 Tiền vào tài khoản · $bank"
            isCredit -> "💳 Chi tiêu thẻ tín dụng · $bank ${if (!cardLast4.isNullOrBlank()) "(••••$cardLast4)" else ""}".trim()
            else -> "💸 Biến động chi tiêu · $bank"
        }
        val categorySuffix = if (!categoryName.isNullOrBlank()) " · $categoryName" else ""
        val contentText = "$sign$formattedAmount$categorySuffix"

        val bigText = buildString {
            append("• Số tiền: $sign$formattedAmount\n")
            append("• Nguồn: ${if (isCredit) "Credit (Thẻ tín dụng)" else "Debit (Tài khoản/Ví)"}\n")
            append("• Nội dung: $note")
            if (balanceAfterMinor != null && balanceAfterMinor > 0) {
                append("\n• Số dư sau GD: ${Money(balanceAfterMinor).formatVnd()}")
            }
            append("\n\n👉 Bấm 'Xác nhận' để ghi sổ, 'Chi tiết' để phân loại, hoặc 'Xóa' để bỏ qua.")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isIncome) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Xác nhận", quickConfirmPendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, "🔍 Chi tiết", reviewDetailPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "🗑️ Xóa", quickDeletePendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
