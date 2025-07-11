package com.csakitheone.onrail.data.sources

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.csakitheone.onrail.LatLng
import com.csakitheone.onrail.NetworkUtils
import com.csakitheone.onrail.data.model.MIArticle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

class MAVINFORM {
    enum class Territory(
        val id: Int,
        val displayName: String,
        val latLng: LatLng,
    ) {
        BUDAPEST(10868, "Budapest", LatLng(47.4979, 19.0402)),
        BALATON(10870, "Balaton", LatLng(46.9200, 17.8900)),
        BACS_KISKUN(10840, "Bács-Kiskun", LatLng(46.6000, 19.2500)),
        BARANYA(10841, "Baranya", LatLng(46.0667, 18.2333)),
        BEKES(10843, "Békés", LatLng(46.6800, 21.0500)),
        BORSOD_ABAÚJ_ZEMPLÉN(10844, "Borsod-Abaúj-Zemplén", LatLng(48.1000, 20.8000)),
        CSONGARAD_CSANAD(10846, "Csongrád-Csanád", LatLng(46.2500, 20.1500)),
        FEJER(10847, "Fejér", LatLng(47.2000, 18.4200)),
        GYOR_MOSON_SOPRON(10849, "Győr-Moson-Sopron", LatLng(47.6833, 17.6500)),
        HAJDU_BIHAR(10850, "Hajdú-Bihar", LatLng(47.5300, 21.6200)),
        HEVES(10852, "Heves", LatLng(47.9000, 20.3500)),
        JASZ_NAGYKUN_SZOLNOK(10853, "Jász-Nagykun-Szolnok", LatLng(47.2000, 20.2000)),
        KOMAROM_ESZTERGOM(10855, "Komárom-Esztergom", LatLng(47.5600, 18.3000)),
        NOGRAD(10856, "Nógrád", LatLng(48.0000, 19.6500)),
        PEST(10858, "Pest", LatLng(47.3000, 19.4000)),
        SOMOGY(11046, "Somogy", LatLng(46.4000, 17.7000)),
        SZABOLCS_SZATMAR_BEREG(10859, "Szabolcs-Szatmár-Bereg", LatLng(47.9500, 22.0000)),
        TOLNA(10861, "Tolna", LatLng(46.5000, 18.6000)),
        VAS(10862, "Vas", LatLng(47.2500, 16.7500)),
        VESZPREM(10864, "Veszprém", LatLng(47.1000, 17.9000)),
        ZALA(10865, "Zala", LatLng(46.8000, 16.8500));

        fun getUrl(): String {
            return "$mavinformTrainsUrl&field_territorial_scope_target_id%5B%5D=$id"
        }

        companion object {
            fun fromName(name: String?): Territory? {
                return entries.find { it.displayName.equals(name, ignoreCase = true) }
            }
        }
    }

    companion object {

        val baseUrl = "https://www.mavcsoport.hu"
        val mavinformTrainsUrl = "$baseUrl/mavinform?field_modalitas_value%5B%5D=vasut"
        var articles by mutableStateOf(emptyList<MIArticle>())
            private set

        fun MIArticle.isCached(context: Context): Boolean {
            val cacheFile = File(context.cacheDir, "mavinform/${this.title}.html")
            if (!cacheFile.exists()) {
                return false
            }
            return try {
                val content = cacheFile.readText()
                MIArticle.fromHtml(content).content.isNotBlank()
            } catch (e: Exception) {
                Log.e("MAVINFORM", "Error reading cached article: ${e.message}")
                false
            }
        }

        fun fetchArticles(context: Context, callback: (List<MIArticle>) -> Unit = {}) {
            if (!NetworkUtils.hasInternet(context)) {
                val cachedArticles = loadCachedArticles(context)
                articles = cachedArticles.sortedByDescending { it.dateValidFrom }
                callback(cachedArticles.sortedByDescending { it.dateValidFrom })
                return
            }
            else {
                val cacheDir = File(context.cacheDir, "mavinform")
                if (cacheDir.exists()) cacheDir.deleteRecursively()
            }

            val urls = listOf(mavinformTrainsUrl) + (1..5).map { "$mavinformTrainsUrl&page=$it" }
            val articles = mutableListOf<MIArticle>()
            var remainingCalls = urls.size

            urls.forEach { url ->
                fetchArticlesFromUrl(url) { newArticles ->
                    articles += newArticles

                    newArticles.forEachIndexed { index, article ->
                        val cacheFile = File(context.cacheDir, "mavinform/${article.title}.html")
                        cacheFile.parentFile?.mkdirs()
                        GlobalScope.launch {
                            delay( index * 100L)
                            cacheFile.writeText(article.toString())
                        }
                    }

                    remainingCalls--
                    if (remainingCalls == 0) {
                        this.articles = articles.sortedByDescending { it.dateValidFrom }
                        callback(articles.sortedByDescending { it.dateValidFrom })
                    }
                }
            }
        }

        fun fetchArticleContent(
            context: Context,
            article: MIArticle,
            callback: () -> Unit = {},
        ) {
            if (!NetworkUtils.hasInternet(context)) {
                return
            }

            GlobalScope.launch(Dispatchers.IO) {
                val html = URL("$baseUrl${article.link}").openConnection().inputStream.bufferedReader().readText()
                val htmlContent = html.substringAfter("<div class=\"field-body\">")
                    .substringBefore("<div class=\"social\">")
                    .trim()

                articles = articles.map {
                    if (it.link == article.link) {
                        val cacheFile = File(context.cacheDir, "mavinform/${it.title}.html")
                        cacheFile.parentFile?.mkdirs()
                        cacheFile.writeText(it.copy(content = htmlContent).toString())
                        it.copy(content = htmlContent)
                    } else {
                        it
                    }
                }
                callback()
            }
        }

        private fun loadCachedArticles(context: Context): List<MIArticle> {
            val cachedArticlesDir = File(context.cacheDir, "mavinform")
            if (!cachedArticlesDir.exists()) return emptyList()

            return cachedArticlesDir.listFiles()?.mapNotNull { file ->
                if (file.extension == "html") {
                    try {
                        MIArticle.fromHtml(file.readText())
                    } catch (e: Exception) {
                        Log.e("MAVINFORM", "Error reading cached article: ${e.message}")
                        null
                    }
                } else {
                    null
                }
            } ?: emptyList()
        }

        @OptIn(DelicateCoroutinesApi::class)
        private fun fetchArticlesFromUrl(
            url: String,
            callback: (List<MIArticle>) -> Unit,
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                val html = URL(url).openConnection().inputStream.bufferedReader().readText()
                val articles = html.substringAfter("custom-news-item")
                    .split("custom-news-item")
                    .filter { it.contains("news-title") }
                    .map {
                        val title = it
                            .substringAfter("news-title\">")
                            .substringAfter("href=\"")
                            .substringAfter("\">")
                            .substringBefore("</")
                            .trim()
                        val link = it
                            .substringAfter("news-title\">")
                            .substringAfter("href=\"")
                            .substringBefore("\">")
                        val dateValidFrom = it
                            .substringAfter("Érvényes:")
                            .substringAfter("date-display-single\">")
                            .substringBefore("</")
                            .trim()
                        val dateLastUpdated = it
                            .substringAfter("news-last-changed\">")
                            .substringAfter("field-content\">")
                            .substringBefore("</")
                            .trim()
                        val scopes = it
                            .substringAfter("field-territorial-scope\">")
                            .split("field-territorial-scope\">")
                            .map { scope -> scope.substringBefore("</").trim() }
                            .filter { scope -> scope.isNotEmpty() }
                        val isDrastic = it.contains("Rendkívüli változás")

                        MIArticle(
                            title = title,
                            link = link,
                            dateValidFrom = dateValidFrom,
                            dateLastUpdated = dateLastUpdated,
                            scopes = scopes,
                            isDrastic = isDrastic,
                        )
                    }

                callback(articles)
            }
        }

    }
}