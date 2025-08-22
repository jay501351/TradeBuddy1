package com.project.tradebuddy

data class TimeSeriesResponse(
    val values: List<StockPoint>
)

data class StockPoint(
    val datetime: String,
    val close:String
)
