package com.project.tradebuddy.api

import com.project.tradebuddy.Stock
import com.project.tradebuddy.StockSearchResponse

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TwelveDataService {

    // 🔍 Symbol Search Endpoint
    // Example: https://api.twelvedata.com/symbol_search?symbol=apple&apikey=YOUR_API_KEY
    @GET("symbol_search")
    suspend fun searchStocks(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Response<StockSearchResponse>
}
