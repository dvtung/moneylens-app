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

object BudgetAlertNotifier {

    private const val CHANNEL_ID = "budget_alerts_channel"
    private const val CHANNEL_NAME = "Cảnh báo Hạn mức Ngân sách"
    private const val NOTIFICATION_ID_WARNING = 8001
    private const val NOTIFICATION_ID_CRITICAL = 8002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Thông báo cảnh báo khi chi tiêu chạm ngưỡng 80% hoặc vượt 100% ngân sách tháng"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendBudgetAlert(
        context: Context,
        percent: Int,
        spentVnd: String,
        limitVnd: String,
        isCritical: Boolean
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isCritical) 100 else 80,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isCritical) {
            "🚨 BÁO ĐỘNG: Đã vượt 100% ngân sách tháng!"
        } else {
            "⚠️ CẢNH BÁO: Đã chạm ${percent}% hạn mức ngân sách!"
        }

        val message = if (isCritical) {
            "Tổng chi tiêu tháng này đã vượt hạn mức: $spentVnd / $limitVnd. Hãy cân nhắc điều chỉnh chi tiêu."
        } else {
            "Bạn đã chi tiêu $spentVnd / $limitVnd (${percent}% ngân sách tháng). Hãy kiểm soát chi tiêu các ngày tới."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isCritical) android.R.drawable.stat_notify_error else android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(
                if (isCritical) NOTIFICATION_ID_CRITICAL else NOTIFICATION_ID_WARNING,
                notification
            )
        }
    }

    fun cancelAlerts(context: Context) {
        runCatching {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(NOTIFICATION_ID_WARNING)
            notificationManager.cancel(NOTIFICATION_ID_CRITICAL)
        }
    }
}
