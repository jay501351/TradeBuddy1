package com.project.tradebuddy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val companyName: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val logoUrl: String? = null
)
