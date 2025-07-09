package com.csakitheone.onrail.data.sources

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class LocalSettings {
    companion object {

        var savedTrainTripNames by mutableStateOf(emptySet<String>())

        fun load(context: Context) {
            //TODO: Remove this after a few releases
            val oldSettingsFile = File(context.dataDir, "settings.json")
            if (oldSettingsFile.exists()) {
                val json = oldSettingsFile.readText()
                if (json.contains("\"savedTrainTripNames\":")) {
                    savedTrainTripNames = json.substringAfter("\"savedTrainTripNames\": [")
                        .substringBefore("]").split(",").map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() }.toSet()
                }
                save(context)
                oldSettingsFile.delete()
                return
            }

            val settingsFile = File(context.dataDir, "settings.ini")
            if (!settingsFile.exists()) {
                return
            }
            val settings = settingsFile.readLines().associate { line ->
                val (key, value) = line.split("=", limit = 2).map { it.trim() }
                key to value
            }
            savedTrainTripNames = settings["savedTrainTripNames"]?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
        }

        fun save(context: Context) {
            val settingsFile = File(context.dataDir, "settings.ini")
            val ini = """
                savedTrainTripNames= ${savedTrainTripNames.joinToString(", ")}
            """
            settingsFile.writeText(ini)
        }

    }
}