package com.project.tradebuddy.network

data class TimeSeriesResponse(
    val meta: Meta,
    val values: List<StockValue>
)

data class Meta(
    val symbol:String,
    val interval:String
)

data class StockValue(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String
)
