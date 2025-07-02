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
            if (json.contains("\"isSendingLocationEnabled\": ")) {
                isSendingLocationEnabled =
                    json.substringAfter("\"isSendingLocationEnabled\": ").first() == 't'
            }
            if (json.contains("\"savedTrainTripNames\":")) {
                savedTrainTripNames = json.substringAfter("\"savedTrainTripNames\": [")
                    .substringBefore("]").split(",").map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }.toSet()
            }
        }

        fun save(context: Context) {
            val settingsFile = File(context.dataDir, "settings.json")
            val json = """
                {
                    "isSendingLocationEnabled": $isSendingLocationEnabled,
                    "savedTrainTripNames": ${
                savedTrainTripNames.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { "\"$it\"" }
            }
                }
            """
            settingsFile.writeText(json)
        }

        var isSendingLocationEnabled by mutableStateOf(false)
        var savedTrainTripNames by mutableStateOf(emptySet<String>())

    }
}