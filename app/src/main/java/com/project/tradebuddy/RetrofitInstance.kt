package com.project.tradebuddy

import com.project.tradebuddy.api.TwelveDataService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api.twelvedata.com/"


    val api: TwelveDataService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwelveDataService::class.java)
    }
}