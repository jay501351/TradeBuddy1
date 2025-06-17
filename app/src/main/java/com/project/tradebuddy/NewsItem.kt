package com.project.tradebuddy

import com.google.gson.annotations.SerializedName

data class NewsItem(
    val title: String?,
    val link: String?,
    val pubDate: String?,
    val description: String?,
    @SerializedName("image_url") val image_url:String?
)
