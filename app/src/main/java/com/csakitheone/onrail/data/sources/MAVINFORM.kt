package com.csakitheone.onrail.data.sources

import android.util.Log
import com.csakitheone.onrail.LatLng
import com.csakitheone.onrail.data.model.MIArticle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

class MAVINFORM {
    enum class Territory(
        val id: Int,
        val displayName: String,
        val latLng: LatLng,
    ) {
        BUDAPEST(10868, "Budapest", LatLng(47.4979, 19.0402)),
        BALATON(10870, "Balaton", LatLng(46.9200, 17.8900)),
        BACS_KISKUN(10840, "Bács-Kiskun", LatLng(46.2500, 19.5000)),
        BARANYA(10841, "Baranya", LatLng(46.0667, 18.2333)),
        BEKES(10843, "Békés", LatLng(46.6667, 21.0833)),
        BORSOD_ABAÚJ_ZEMPLÉN(10844, "Borsod-Abaúj-Zemplén", LatLng(48.1000, 20.8000)),
        CSONGARAD_CSANAD(10846, "Csongrád-Csanád", LatLng(46.2500, 20.1500)),
        FEJER(10847, "Fejér", LatLng(47.2000, 18.4000)),
        GYOR_MOSON_SOPRON(10849, "Győr-Moson-Sopron", LatLng(47.6833, 17.6500)),
        HAJDU_BIHAR(10850, "Hajdú-Bihar", LatLng(47.5000, 21.6500)),
        HEVES(10852, "Heves", LatLng(47.9000, 20.1500)),
        JASZ_NAGYKUN_SZOLNOK(10853, "Jász-Nagykun-Szolnok", LatLng(47.2000, 20.2000)),
        KOMAROM_ESZTERGOM(10855, "Komárom-Esztergom", LatLng(47.7000, 18.4000)),
        NOGRAD(10856, "Nógrád", LatLng(47.9000, 19.5000)),
        PEST(10858, "Pest", LatLng(47.5000, 19.2500)),
        SOMOGY(11046, "Somogy", LatLng(46.5000, 17.0000)),
        SZABOLCS_SZATMAR_BEREG(10859, "Szabolcs-Szatmár-Bereg", LatLng(47.9500, 22.0000)),
        TOLNA(10861, "Tolna", LatLng(46.5000, 18.7000)),
        VAS(10862, "Vas", LatLng(47.2000, 16.6000)),
        VESZPREM(10864, "Veszprém", LatLng(47.1000, 17.9000)),
        ZALA(10865, "Zala", LatLng(46.5000, 16.8000)),
    }

    companion object {

        val baseUrl = "https://www.mavcsoport.hu"

        fun fetchRecentArticles(callback: (List<MIArticle>) -> Unit) {
            val url = "$baseUrl/mavinform?field_modalitas_value%5B%5D=vasut"
            fetchArticlesFromUrl(url, callback)
        }

        fun fetchRecentArticlesByTerritory(
            territory: Territory,
            callback: (List<MIArticle>) -> Unit,
        ) {
            val url =
                "$baseUrl/mavinform?field_modalitas_value%5B%5D=vasut&field_territorial_scope_target_id%5B%5D=${territory.id}"
            fetchArticlesFromUrl(url, callback)
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

                        MIArticle(
                            title = title,
                            link = link,
                            dateValidFrom = dateValidFrom,
                            dateLastUpdated = dateLastUpdated,
                        )
                    }

                callback(articles)
            }
        }

    }
}