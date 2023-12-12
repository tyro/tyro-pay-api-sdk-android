package com.tyro.payapi.payrequest.repository

import com.tyro.payapi.payrequest.client.PayRequestClient
import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.retrofit.RetrofitFactory
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import retrofit2.Response
import retrofit2.Retrofit

class DefaultPayRequestRepositoryTest {
    private lateinit var payRequestRepo: DefaultPayRequestRepository
    private lateinit var payRequestClient: PayRequestClient
    private val payRequestResponse = PayRequestResponse(
        PayRequestResponse.PayRequestOrigin("order123", "ref123", "name123"),
        PayRequestResponse.PayRequestStatus.SUCCESS,
        PayRequestResponse.Capture(PayRequestResponse.CaptureMethod.AUTOMATIC, null),
        PayRequestResponse.AmountWithCurrency(100L, "AUD"),
        PayRequestResponse.ThreeDSecure(PayRequestResponse.ThreeDSecureStatus.SUCCESS, null, null),
        null,
        null,
        null,
    )

    @Before
    fun setUp() {
        val retrofitFactory: RetrofitFactory = Mockito.mock(RetrofitFactory::class.java)
        val retrofit: Retrofit = Mockito.mock(Retrofit::class.java)
        payRequestClient = Mockito.mock(PayRequestClient::class.java)
        `when`(retrofitFactory.createRetrofit("https://api.tyro.com/")).thenReturn(retrofit)
        `when`(retrofit.create(PayRequestClient::class.java)).thenReturn(payRequestClient)
        payRequestRepo = DefaultPayRequestRepository(retrofitFactory)
    }

    @Test
    fun `fetchPayRequest() should return body when success response`() = runBlocking {
        `when`(payRequestClient.getPayRequest("paySecret123")).thenReturn(
            Response.success(payRequestResponse),
        )
        val responseBody = payRequestRepo.fetchPayRequest("paySecret123")
        assertEquals(payRequestResponse, responseBody)
    }

    @Test
    fun `fetchPayRequest() should return null when request fails`() = runBlocking {
        `when`(payRequestClient.getPayRequest("paySecret123")).thenReturn(
            Response.error(403, ResponseBody.create(MediaType.get("application/json"), "test")),
        )
        val responseBody = payRequestRepo.fetchPayRequest("paySecret123")
        assertEquals(null, responseBody)
    }
}
