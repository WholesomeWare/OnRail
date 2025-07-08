package com.csakitheone.onrail.data.model

import android.os.Parcelable
import com.csakitheone.onrail.data.sources.MAVINFORM
import kotlinx.parcelize.Parcelize

@Parcelize
data class MIArticle(
    val title: String,
    val link: String,
    val dateValidFrom: String,
    val dateLastUpdated: String,
    val scopes: List<String>,
    val isDrastic: Boolean = false,
) : Parcelable {
    val territoryScopes: List<MAVINFORM.Territory>
        get() = scopes.mapNotNull { MAVINFORM.Territory.fromName(it) }
}
