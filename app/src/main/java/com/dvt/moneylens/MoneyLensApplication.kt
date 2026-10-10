package com.dvt.moneylens

import android.app.Application
import com.dvt.moneylens.data.local.MoneyLensDatabase

class MoneyLensApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize local Room database
        MoneyLensDatabase.getInstance(this)
    }
}
