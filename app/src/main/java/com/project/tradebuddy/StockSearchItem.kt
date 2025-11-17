package com.project.tradebuddy

data class StockSearchItem(
    val symbol: String,
    val instrument_name: String,
    val exchange: String,
    val country: String,
    val currency: String
)
