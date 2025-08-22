package com.project.tradebuddy

import retrofit2.http.GET
import retrofit2.http.Query

interface StockApi {
    @GET("Time_series")
    suspend fun getStockData(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("apikey") apiKey: String): TimeSeriesResponse
}