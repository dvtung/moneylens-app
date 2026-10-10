package com.dvt.moneylens.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dvt.moneylens.data.local.MoneyLensDatabase
import com.dvt.moneylens.data.repository.MoneyLensRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickConfirmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val txId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val db = MoneyLensDatabase.getInstance(context)
        val repository = MoneyLensRepository(db)

        val action = intent.action
        CoroutineScope(Dispatchers.IO).launch {
            if (action == ACTION_QUICK_DELETE) {
                repository.deleteTransaction(txId)
            } else {
                repository.confirmTransaction(txId, null)
            }
            if (notifId != -1) {
                runCatching {
                    NotificationManagerCompat.from(context).cancel(notifId)
                }
            }
        }
    }

    companion object {
        const val ACTION_QUICK_CONFIRM = "com.dvt.moneylens.ACTION_QUICK_CONFIRM"
        const val ACTION_QUICK_DELETE = "com.dvt.moneylens.ACTION_QUICK_DELETE"
        const val EXTRA_TX_ID = "extra_tx_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
