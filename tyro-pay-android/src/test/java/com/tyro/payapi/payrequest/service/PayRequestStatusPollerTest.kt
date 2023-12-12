package com.tyro.payapi.payrequest.service

import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.repository.DefaultPayRequestRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify

class PayRequestStatusPollerTest {
    private lateinit var payRequestStatusPoller: PayRequestStatusPoller
    private lateinit var payRequestRepository: DefaultPayRequestRepository

    @Before
    fun setUp() {
        payRequestRepository = Mockito.mock()
        payRequestStatusPoller = PayRequestStatusPoller(PayRequestPoller(payRequestRepository))
    }

    @Test
    fun `pollForInitialStatusUpdate() should return the pay request when status is success`() = runBlocking {
        val finalStatusResult = createResponse(PayRequestResponse.PayRequestStatus.SUCCESS)
        Mockito.`when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
            .thenReturn(finalStatusResult)
        val result = payRequestStatusPoller.pollForInitialStatusUpdate("paySecret123")
        verify(payRequestRepository, Mockito.times(2)).fetchPayRequest("paySecret123")
        assertEquals(finalStatusResult, result)
    }

    @Test
    fun `pollForInitialStatusUpdate() should return the pay request when status is failed`() = runBlocking {
        val finalStatusResult = createResponse(PayRequestResponse.PayRequestStatus.FAILED)
        Mockito.`when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
            .thenReturn(finalStatusResult)
        val result = payRequestStatusPoller.pollForInitialStatusUpdate("paySecret123", 1)
        verify(payRequestRepository, Mockito.times(3)).fetchPayRequest("paySecret123")
        assertEquals(finalStatusResult, result)
    }

    @Test
    fun `pollForInitialStatusUpdate() should return the pay request when status is awaiting auth`() = runBlocking {
        val finalStatusResult = createResponse(PayRequestResponse.PayRequestStatus.AWAITING_AUTHENTICATION)
        Mockito.`when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(finalStatusResult)
        val result = payRequestStatusPoller.pollForInitialStatusUpdate("paySecret123", 1)
        verify(payRequestRepository, Mockito.times(4)).fetchPayRequest("paySecret123")
        assertEquals(finalStatusResult, result)
    }

    @Test
    fun `pollForInitialStatusUpdate() should timeout and return the latest pay request when expected status never returned`() = runBlocking {
        val finalStatusResult = createResponse(PayRequestResponse.PayRequestStatus.PROCESSING)
        Mockito.`when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
            .thenReturn(finalStatusResult)
        val result = payRequestStatusPoller.pollForInitialStatusUpdate("paySecret123", 1)
        verify(payRequestRepository, Mockito.times(60)).fetchPayRequest("paySecret123")
        assertEquals(finalStatusResult, result)
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
