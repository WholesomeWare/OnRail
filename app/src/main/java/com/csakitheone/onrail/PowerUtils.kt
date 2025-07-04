package com.csakitheone.onrail

import android.content.Context
import android.os.PowerManager

class PowerUtils {
    companion object {

        val isPowerSaveMode: Boolean
            get() = powerManager?.isPowerSaveMode ?: false

        private var powerManager: PowerManager? = null

        fun init(context: Context) {
            powerManager = context.getSystemService(PowerManager::class.java)
        }

    }
}