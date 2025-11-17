package com.project.tradebuddy

data class StockItem(
    val logoResId: Int,
    val symbol: String,
    val company: String,
    val price: String,
    val change: String,
    val isPositive: Boolean
)

data class Watchlist(
    val name: String,
    val stocks: MutableList<Stock> = mutableListOf()
)