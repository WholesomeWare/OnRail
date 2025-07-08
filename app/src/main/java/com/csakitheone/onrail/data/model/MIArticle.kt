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
}
