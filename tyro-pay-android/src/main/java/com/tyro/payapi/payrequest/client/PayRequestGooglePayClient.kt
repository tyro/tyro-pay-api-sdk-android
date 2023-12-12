package com.tyro.payapi.payrequest.client

import com.tyro.payapi.payrequest.model.GooglePayPayRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH

interface PayRequestGooglePayClient {
    @PATCH("/connect/pay/client/requests")
    suspend fun submitPayRequest(@Header("Pay-Secret") paySecret: String, @Body googlePayPayRequest: GooglePayPayRequest): Response<Void>
}
