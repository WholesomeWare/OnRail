package com.csakitheone.onrail.data.sources

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class LocalSettings {
    companion object {

        fun load(context: Context) {
            val settingsFile = File(context.dataDir, "settings.json")
            if (!settingsFile.exists()) {
                return
            }
            val json = settingsFile.readText()
            isSendingLocationEnabled =
                json.substringAfter("\"isSendingLocationEnabled\": ")[0] == 't'
        }

        fun save(context: Context) {
            val settingsFile = File(context.dataDir, "settings.json")
            val json = """
                {
                    "isSendingLocationEnabled": $isSendingLocationEnabled
                }
            """
            settingsFile.writeText(json)
        }

        var isSendingLocationEnabled by mutableStateOf(false)

    }
}