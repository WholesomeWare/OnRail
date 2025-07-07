package com.csakitheone.onrail.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MIArticle(
    val title: String,
    val link: String,
    val dateValidFrom: String,
    val dateLastUpdated: String,
) : Parcelable