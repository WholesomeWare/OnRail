package com.csakitheone.onrail.data.model

import android.os.Parcelable
import com.csakitheone.onrail.data.sources.MAVINFORM
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.LocalDateTime

@Parcelize
data class MIArticle(
    val title: String,
    val link: String,
    val dateValidFrom: String,
    val dateLastUpdated: String,
    val scopes: List<String>,
    val isDrastic: Boolean = false,
    val content: String = "",
) : Parcelable {
    val territoryScopes: List<MAVINFORM.Territory>
        get() = scopes.mapNotNull { MAVINFORM.Territory.fromName(it) }

    val readableDateValidFrom: String
        get() {
            val dateTime =
                LocalDateTime.parse(dateValidFrom.replace(". ", "T").replace(".", "-"))
            val now = LocalDateTime.now()
            val days = Duration.between(dateTime, now).toDays()
            if (days > 0) {
                return "$days napja"
            }
            val hours = Duration.between(dateTime, now).toHours()
            if (hours > 0) {
                return "$hours órája"
            }
            val minutes = Duration.between(dateTime, now).toMinutes()
            if (minutes > 0) {
                return "$minutes perce"
            }
            return "most"
        }

    val readableDateLastUpdated: String
        get() {
            val dateTime =
                LocalDateTime.parse(dateLastUpdated.replace(". ", "T").replace(".", "-"))
            val now = LocalDateTime.now()
            val days = Duration.between(dateTime, now).toDays()
            if (days > 0) {
                return "$days napja"
            }
            val hours = Duration.between(dateTime, now).toHours()
            if (hours > 0) {
                return "$hours órája"
            }
            val minutes = Duration.between(dateTime, now).toMinutes()
            if (minutes > 0) {
                return "$minutes perce"
            }
            return "most"
        }

    /**
     * Returns an html representation of the article.
     */
    override fun toString(): String {
        return """
            <div id="mi-article-metadata" style="display: none;">
                <h2 class="mi-article-title">$title</h2>
                <a class="mi-article-link" href="$link">Link</a>
                <p class="mi-article-date-valid-from">$dateValidFrom</p>
                <p class="mi-article-date-last-updated">$dateLastUpdated</p>
                <p class="mi-article-scopes">${scopes.joinToString(", ")}</p>
                <p class="mi-article-is-drastic">${if (isDrastic) "Rendkívüli változás" else "Normál változás"}</p>
            </div>
            <!-- Article content begins -->
            $content
        """.trimIndent()
    }

    companion object {
        fun fromHtml(html: String): MIArticle {
            val (metadataHtml, contentHtml) = html.split(
                "<!-- Article content begins -->",
                limit = 2
            )
            val title = metadataHtml.substringAfter("class=\"mi-article-title\">")
                .substringBefore("</")
            val link = metadataHtml.substringAfter("class=\"mi-article-link\" href=\"")
                .substringBefore("\">")
            val dateValidFrom = metadataHtml.substringAfter("class=\"mi-article-date-valid-from\">")
                .substringBefore("</")
            val dateLastUpdated =
                metadataHtml.substringAfter("class=\"mi-article-date-last-updated\">")
                    .substringBefore("</")
            val scopes = metadataHtml.substringAfter("class=\"mi-article-scopes\">")
                .substringBefore("</")
                .split(",")
                .map { it.trim() }
            val isDrastic = metadataHtml.contains("Rendkívüli")
            val content = contentHtml.trim()

            return MIArticle(
                title = title,
                link = link,
                dateValidFrom = dateValidFrom,
                dateLastUpdated = dateLastUpdated,
                scopes = scopes,
                isDrastic = isDrastic,
                content = content
            )
        }
    }
}
