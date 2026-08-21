package com.csakitheone.onrail.data

import com.csakitheone.onrail.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Gemini {
    companion object {
        private var generativeModel: GenerativeModel? = null

        private fun init() {
            if (generativeModel != null) return
            generativeModel = GenerativeModel(
                modelName = "gemini-3.5-flash-lite",
                apiKey = BuildConfig.GEMINI_API_KEY,
                systemInstruction = content {
                    text("Te egy magyar vasúti információs asszisztens vagy. Foglald össze a megadott MÁVINFORM cikkeket egyetlen, lényegre törő magyar nyelvű mondatban.")
                }
            )
        }

        suspend fun summarizeArticle(title: String, content: String): String = withContext(Dispatchers.IO) {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) return@withContext "Hiba: Hiányzó API kulcs a build konfigurációban."

            init()

            return@withContext try {
                val response = generativeModel?.generateContent("Cím: $title\n\nTartalom: $content")
                response?.text ?: "Hiba: Nem sikerült összefoglalót készíteni."
            } catch (e: Exception) {
                "Hiba: ${e.message}"
            }
        }
    }
}
