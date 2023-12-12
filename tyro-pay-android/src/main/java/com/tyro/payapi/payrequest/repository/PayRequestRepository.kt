package com.tyro.payapi.payrequest.repository

import com.tyro.payapi.payrequest.client.PayRequestClient
import com.tyro.payapi.payrequest.constants.BaseURLConstants
import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.retrofit.RetrofitFactory
import timber.log.Timber

interface PayRequestRepository {
    suspend fun fetchPayRequest(
        paySecret: String,
    ): PayRequestResponse?
}

internal class DefaultPayRequestRepository(
    retrofitFactory: RetrofitFactory,
) : PayRequestRepository {
    private val payRequestClient: PayRequestClient = retrofitFactory
        .createRetrofit(BaseURLConstants.PAY_API_BASE_URL)
        .create(PayRequestClient::class.java)

    override suspend fun fetchPayRequest(paySecret: String): PayRequestResponse? {
        Timber.d("fetchPayRequest()")
        val response = payRequestClient.getPayRequest(paySecret)
        Timber.d("fetchPayRequest(): pay request returned")
        Timber.d(response.body().toString())
        if (!response.isSuccessful) {
            Timber.e("Could not fetch Pay Request")
            Timber.e(response.raw().toString())
            return null
        }
        return response.body()
    }
}
