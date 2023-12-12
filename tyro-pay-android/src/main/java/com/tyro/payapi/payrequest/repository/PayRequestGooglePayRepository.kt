package com.tyro.payapi.payrequest.repository

import com.tyro.payapi.payrequest.client.PayRequestGooglePayClient
import com.tyro.payapi.payrequest.constants.BaseURLConstants
import com.tyro.payapi.payrequest.model.GooglePayPayRequest
import com.tyro.payapi.retrofit.RetrofitFactory
import timber.log.Timber

interface PayRequestGooglePayRepository {
    suspend fun submitPayRequest(
        paySecret: String,
        googlePayPayRequest: GooglePayPayRequest,
    ): Boolean
}

internal class DefaultPayRequestGooglePayRepository(
    private val payRequestGooglePayClient: PayRequestGooglePayClient,
) : PayRequestGooglePayRepository {

    override suspend fun submitPayRequest(paySecret: String, googlePayPayRequest: GooglePayPayRequest): Boolean {
        Timber.d("submitPayRequest()")
        val response = payRequestGooglePayClient.submitPayRequest(paySecret, googlePayPayRequest)
        Timber.d("submitPayRequest(): returned")
        Timber.d(response.raw().toString())
        if (!response.isSuccessful) {
            Timber.e("Error submitting Pay Request")
            Timber.e(response.raw().toString())
            return false
        }
        return true
    }

    internal class Factory {
        fun createSandboxRepository(retrofitFactory: RetrofitFactory): DefaultPayRequestGooglePayRepository {
            return DefaultPayRequestGooglePayRepository(
                retrofitFactory.createRetrofit(BaseURLConstants.PAY_API_SANDBOX_GOOGLE_PAY_INBOUND_BASE_URL)
                    .create(PayRequestGooglePayClient::class.java),
            )
        }
        fun createLiveRepository(retrofitFactory: RetrofitFactory): DefaultPayRequestGooglePayRepository {
            return DefaultPayRequestGooglePayRepository(
                retrofitFactory.createRetrofit(BaseURLConstants.PAY_API_LIVE_GOOGLE_PAY_INBOUND_BASE_URL)
                    .create(PayRequestGooglePayClient::class.java),
            )
        }
    }
}
