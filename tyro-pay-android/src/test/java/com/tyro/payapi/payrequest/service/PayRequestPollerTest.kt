package com.tyro.payapi.payrequest.service

import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.repository.DefaultPayRequestRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify

class PayRequestPollerTest {
    private lateinit var poller: PayRequestPoller
    private lateinit var payRequestRepository: DefaultPayRequestRepository

    @Before
    fun setUp() {
        payRequestRepository = Mockito.mock()
        poller = PayRequestPoller(payRequestRepository)
    }

    @Test
    fun `pollForResult() should return statusResult when condition is met after 2 tries`() = runBlocking {
        val finalStatusResult = createResponse(PayRequestResponse.PayRequestStatus.SUCCESS)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
            .thenReturn(finalStatusResult)
        val res = poller.pollForResult(
            "paySecret123",
            { statusResult -> statusResult.status == PayRequestResponse.PayRequestStatus.SUCCESS },
            10,
            2,
        )
        verify(payRequestRepository, times(2)).fetchPayRequest("paySecret123")
        Assert.assertEquals(finalStatusResult, res)
    }

    @Test
    fun `pollForResult() should return the latest statusResult when expected result never matches`() = runBlocking {
        val latestResult = createResponse(PayRequestResponse.PayRequestStatus.PROCESSING)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(latestResult)
        val res = poller.pollForResult(
            "paySecret123",
            { statusResult -> statusResult.status == PayRequestResponse.PayRequestStatus.SUCCESS },
            10,
            10,
        )
        verify(payRequestRepository, times(10)).fetchPayRequest("paySecret123")
        Assert.assertEquals(latestResult, res)
    }

    @Test
    fun `pollForResult() should return a null statusResult when fetching pay request returns null`() = runBlocking {
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(null)
        val res = poller.pollForResult(
            "paySecret123",
            { statusResult -> statusResult.status == PayRequestResponse.PayRequestStatus.SUCCESS },
            10,
            10,
        )
        verify(payRequestRepository, times(1)).fetchPayRequest("paySecret123")
        Assert.assertEquals(null, res)
    }

    private fun createResponse(status: PayRequestResponse.PayRequestStatus): PayRequestResponse {
        return PayRequestResponse(
            PayRequestResponse.PayRequestOrigin("order123", "ref123", "name123"),
            status,
            PayRequestResponse.Capture(PayRequestResponse.CaptureMethod.AUTOMATIC, null),
            PayRequestResponse.AmountWithCurrency(100L, "AUD"),
            PayRequestResponse.ThreeDSecure(PayRequestResponse.ThreeDSecureStatus.SUCCESS, null, null),
            null,
            null,
            null,
        )
    }
}
