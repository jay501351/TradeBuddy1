package com.project.tradebuddy

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("api/1/news")
    suspend fun getFinancialNews(
        @Query("apikey") apiKey: String,
        @Query("category") category: String = "business",
        @Query("language") language: String = "en"
    ): Response<NewsResponse>
}