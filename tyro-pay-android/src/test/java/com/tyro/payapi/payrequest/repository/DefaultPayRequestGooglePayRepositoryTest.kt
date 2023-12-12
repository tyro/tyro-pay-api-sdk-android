package com.tyro.payapi.payrequest.repository

import com.tyro.payapi.payrequest.client.PayRequestGooglePayClient
import com.tyro.payapi.payrequest.model.GooglePayPayRequest
import com.tyro.payapi.retrofit.RetrofitFactory
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import retrofit2.Response
import retrofit2.Retrofit

class DefaultPayRequestGooglePayRepositoryTest {
    private lateinit var payRequestGooglePayRepository: DefaultPayRequestGooglePayRepository
    private lateinit var factory: DefaultPayRequestGooglePayRepository.Factory
    private lateinit var payRequestGooglePayClient: PayRequestGooglePayClient
    private val googlePayRequest = GooglePayPayRequest(
        GooglePayPayRequest.GooglePayPayload(
            GooglePayPayRequest.Token(
                "sig",
                GooglePayPayRequest.SigningKey("signedKey", listOf("1", "2")),
                "1",
                "signedMsg",
            ),
        ),
    )

    @Before
    fun setUp() {
        val retrofitFactory: RetrofitFactory = Mockito.mock(RetrofitFactory::class.java)
        val retrofit: Retrofit = Mockito.mock(Retrofit::class.java)
        payRequestGooglePayClient = Mockito.mock(PayRequestGooglePayClient::class.java)
        Mockito.`when`(retrofitFactory.createRetrofit("https://pay.inbound.sandbox.googlepay.connect.tyro.com/")).thenReturn(retrofit)
        Mockito.`when`(retrofit.create(PayRequestGooglePayClient::class.java)).thenReturn(payRequestGooglePayClient)
        factory = DefaultPayRequestGooglePayRepository.Factory()
        payRequestGooglePayRepository = factory.createSandboxRepository(retrofitFactory)
    }

    @Test
    fun `submitPayRequest() should return true when submit success`() = runBlocking {
        Mockito.`when`(payRequestGooglePayClient.submitPayRequest("paySecret123", googlePayRequest)).thenReturn(
            Response.success(null),
        )
        val success = payRequestGooglePayRepository.submitPayRequest("paySecret123", googlePayRequest)
        TestCase.assertEquals(true, success)
    }

    @Test
    fun `submitPayRequest() should return false when submit fails`() = runBlocking {
        Mockito.`when`(payRequestGooglePayClient.submitPayRequest("paySecret123", googlePayRequest)).thenReturn(
            Response.error(403, ResponseBody.create(MediaType.get("application/json"), "test")),
        )
        val success = payRequestGooglePayRepository.submitPayRequest("paySecret123", googlePayRequest)
        TestCase.assertEquals(false, success)
    }

    @Test
    fun `createSandboxRepository() should create with correct baseUrl and not throw NPE`() {
        val retrofitFactory: RetrofitFactory = Mockito.mock(RetrofitFactory::class.java)
        val retrofit: Retrofit = Mockito.mock(Retrofit::class.java)
        payRequestGooglePayClient = Mockito.mock(PayRequestGooglePayClient::class.java)
        Mockito.`when`(retrofitFactory.createRetrofit("https://pay.inbound.sandbox.googlepay.connect.tyro.com/")).thenReturn(retrofit)
        Mockito.`when`(retrofit.create(PayRequestGooglePayClient::class.java)).thenReturn(payRequestGooglePayClient)
        val repo = DefaultPayRequestGooglePayRepository.Factory().createSandboxRepository(retrofitFactory)
        TestCase.assertNotNull(repo)
    }

    @Test
    fun `createLiveRepository() should create with correct baseUrl and not throw NPE`() {
        val retrofitFactory: RetrofitFactory = Mockito.mock(RetrofitFactory::class.java)
        val retrofit: Retrofit = Mockito.mock(Retrofit::class.java)
        payRequestGooglePayClient = Mockito.mock(PayRequestGooglePayClient::class.java)
        Mockito.`when`(retrofitFactory.createRetrofit("https://pay.inbound.googlepay.connect.tyro.com/")).thenReturn(retrofit)
        Mockito.`when`(retrofit.create(PayRequestGooglePayClient::class.java)).thenReturn(payRequestGooglePayClient)
        val repo = DefaultPayRequestGooglePayRepository.Factory().createLiveRepository(retrofitFactory)
        TestCase.assertNotNull(repo)
    }
}
