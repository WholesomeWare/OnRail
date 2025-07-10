package com.csakitheone.onrail

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.csakitheone.onrail.data.sources.LocalSettings
import com.csakitheone.onrail.data.sources.RTDB

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        PowerUtils.init(this)
        LocalSettings.load(this)

        clearOldMessagesWhenNotMetered()
    }

    fun clearOldMessagesWhenNotMetered() {
        if (NetworkUtils.isUnmetered(this)) {
            RTDB.clearOldMessages()
        }
    }

}