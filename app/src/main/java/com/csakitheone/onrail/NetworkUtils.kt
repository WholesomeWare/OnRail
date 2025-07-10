package com.csakitheone.onrail

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkUtils {
    companion object {

        private var connectivityManager: ConnectivityManager? = null
        private var networkCallback = ConnectivityManager.NetworkCallback()

        fun hasInternet(context: Context): Boolean {
            if (connectivityManager == null) {
                connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            }
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }

        fun isUnmetered(context: Context): Boolean {
            if (connectivityManager == null) {
                connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            }
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        }

        fun listen(context: Context, listener: (Boolean) -> Unit) {
            if (connectivityManager == null) {
                connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            }
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    listener(true)
                }

                override fun onLost(network: Network) {
                    listener(false)
                }
            }
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        }

        fun stopListening() {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }

    }
}