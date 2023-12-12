package com.tyro.payapi.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class RetrofitFactory {
    fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build()
    }
}
