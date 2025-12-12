package com.project.tradebuddy

data class StockSearchItem(
    var symbol: String = "",
    var instrument_name: String = "",
    var exchange: String = "",
    var country: String = "",
    var currency: String = ""
)
