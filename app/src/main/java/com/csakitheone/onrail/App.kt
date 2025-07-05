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
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isUnmetered =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        val hasInternet =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (hasInternet && isUnmetered) {
            RTDB.clearOldMessages()
        }
    }

}