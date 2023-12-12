package com.tyro.payapi.payrequest.client

import com.tyro.payapi.payrequest.model.PayRequestResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface PayRequestClient {
    @GET("/connect/pay/client/requests")
    suspend fun getPayRequest(@Header("Pay-Secret") paySecret: String): Response<PayRequestResponse>
}
