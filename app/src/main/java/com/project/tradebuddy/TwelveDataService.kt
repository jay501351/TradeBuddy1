package com.project.tradebuddy.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Map value for each symbol in batch /quote response (kept for typed mapping if needed)
data class QuoteData(
    val symbol: String? = null,
    val price: String? = null,
    val previous_close: String? = null
)

interface TwelveDataService {
    // Symbol Search (existing)
    @GET("symbol_search")
    suspend fun searchStocks(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Response<com.project.tradebuddy.StockSearchResponse>

    // Batch typed quote endpoint (kept for compatibility)
    @GET("quote")
    suspend fun getQuotes(
        @Query("symbol") symbols: String,
        @Query("apikey") apiKey: String
    ): Response<Map<String, QuoteData>>

    // NEW: raw response body version — more robust when API returns single-object / map / error
    @GET("quote")
    suspend fun getQuotesRaw(
        @Query("symbol") symbols: String,
        @Query("apikey") apiKey: String
    ): Response<ResponseBody>
}
