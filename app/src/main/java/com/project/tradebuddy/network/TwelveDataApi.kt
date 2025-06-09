package com.project.tradebuddy.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.sql.Time

interface TwelveDataApi{
    @GET("time_series")
    suspend fun getTimeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("apikey") apiKey: String
    ): Response<TimeSeriesResponse>
}