package com.tyro.payapi.googlepayclient.viewmodel

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.gson.Gson
import com.tyro.payapi.googlepayclient.TyroGooglePayClient
import com.tyro.payapi.googlepayclient.constants.GooglePayCardNetwork
import com.tyro.payapi.googlepayclient.googlepay.DefaultGooglePayRepository
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRequestFactory
import com.tyro.payapi.googlepayclient.view.TyroGooglePayActivityContract
import com.tyro.payapi.payrequest.constants.ErrorCode
import com.tyro.payapi.payrequest.constants.TyroPayRequestErrorType
import com.tyro.payapi.payrequest.model.GooglePayPayRequest
import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.model.TyroPayRequestError
import com.tyro.payapi.payrequest.repository.DefaultPayRequestRepository
import com.tyro.payapi.payrequest.repository.PayRequestGooglePayRepository
import com.tyro.payapi.payrequest.service.PayRequestPoller
import com.tyro.payapi.payrequest.service.PayRequestStatusPoller
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
class TyroGooglePayViewModelTest {
    private lateinit var viewModel: TyroGooglePayViewModel

    @Mock
    private lateinit var paymentsClient: PaymentsClient

    @Mock
    private lateinit var payRequestRepository: DefaultPayRequestRepository

    @Mock
    private lateinit var payRequestGooglePayRepo: PayRequestGooglePayRepository

    @Mock
    private lateinit var googlePayRepository: DefaultGooglePayRepository

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle

    @Mock
    private lateinit var paymentData: PaymentData

    @Mock
    private lateinit var observer: Observer<TyroGooglePayClient.Result>
    private val gson = Gson()
    private val paymentDataStr = """
            {
                "paymentMethodData":{
                    "tokenizationData":{
                        "token":{
                            "signature":"MEYCIQDtf9T3uyyVToB9nIuk1B588gZVHW1/PH6JxUhWJR1buwIhAN+RWnMk6D/JZ0pjH8RDN2so9tTE4svUTOgCTO1c7JTN",
                            "intermediateSigningKey":{
                                "signedKey":"{\"keyValue\":\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGFiF/L5cpwK+Tx7wPZmO7HuaXvdn3Rq1ifgAX6CoKqp+vSHieo+DKpv4jIWQkwrIXNH4VkYbGakv3ci0UwqORQ\\u003d\\u003d\",\"keyExpiration\":\"1689143240647\"}",
                                "signatures":[
                                    "MEYCIQCtQctDXiYjpko0mkjigQNuS5oBq1XMAH3AweiR6C29JAIhAJgbNBnQ+rHi8sC+YWeuYVvSlBSpX9a7PJlrkoY0RZ73"
                                ]
                            },
                            "protocolVersion":"ECv2",
                            "signedMessage":"{\"encryptedMessage\":\"oJKPRT4xHjfjyGBDyoDLzBvww990sFk2/oaB+iIWXj2/9S1+dwjEguwkWw5DbYqxDcRu5MGZm2EwpmBdIRge9UU6YFBt8K/SlMR0r/YrBc+/0+QoU2oUnt7KKjscgNfqMhY8aDlFbQJ4xLAENx7wRmtZlIwjyyJFs/e5ikPEZGemQVH279pOEUoVUHkacLsM73j1jpT29dUXYmcDpNYBBqWcBj0A4WMgNzo+nbXcamq9S4dTkz1HloHvDV1Onn2DJVlkjwAhKppUpWbrhQd3BiSmTJM4pxLVTFpzLtrCKGajK7k9YlltuKb6D7FlLELyyqp+HfpST4hQaCl4at8ccm/u0H1OrzLvYKzFPg4lBRq4wOxK7/F7NffcJfa2ZkyGGYodROo0jzpghop7p2JFhrrDEMru1ofHIXsj4QONLRgblRofJNuDb9ti9bxua1pYxAM769e/qOK2NKduQjCx7qIKDYgauZziRgeG2kVc3j1veWvBnj5La4gvEBkmerO0vKFHfYvDjcc+ejfwj/05oEzuSTHdMWsSe3qZ3thplDuoTVgBiRvQo/l5Zxj9c3WRQENEvQni7tBnTsJkSRX9eaRlXcu05uxLleiuDgdQdrvsKrI5cfWjiAUY6nwr91v6rzKNu1uRUYYInrIS4XICW+nHu71Cy4A\\u003d\",\"ephemeralPublicKey\":\"BMHDsGizQGkNQL7sfF7rcxOl5wyKzUuLMQlGdP5A/MqHgo4vCIL5cop+2tJN3g2LfyuMu1xeIT+d8NG3/h2JT6I\\u003d\",\"tag\":\"I5lICOlsJlaVGX/EeAUYbtJdoN6fMi6IwpI3BCwFlmM\\u003d\"}"
                        }
                    }
                }
            }
    """.trimIndent()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        viewModel = TyroGooglePayViewModel(
            paymentsClient,
            TyroGooglePayActivityContract.PayRequestParams("paySecret123", TyroGooglePayClient.Config(false, "someMerchant", listOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD))),
            payRequestRepository,
            payRequestGooglePayRepo,
            googlePayRepository,
            GooglePayRequestFactory(),
            savedStateHandle,
            PayRequestStatusPoller(PayRequestPoller(payRequestRepository)),
            gson,
        )
    }

    @Test
    fun `loadGooglePayPaymentData() should throw exception when google pay is not available`() = runBlocking {
        `when`(googlePayRepository.isGooglePayReady(listOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD))).thenReturn(
            flowOf(false),
        )
        try {
            viewModel.loadGooglePayPaymentData()
            fail()
        } catch (e: Exception) {
            assertEquals(IllegalStateException::class.java, e.javaClass)
            assertEquals("Google Pay is not available", e.message)
        }
    }

    @Test
    fun `loadGooglePayPaymentData() should throw when pay request cannot be fetched`() = runBlocking {
        `when`(googlePayRepository.isGooglePayReady(listOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD))).thenReturn(
            flowOf(true),
        )
        `when`(payRequestRepository.fetchPayRequest("paySecret123")).thenReturn(null)
        try {
            viewModel.loadGooglePayPaymentData()
            fail()
        } catch (e: Exception) {
            assertEquals(IllegalArgumentException::class.java, e.javaClass)
            assertEquals("Could not fetch Pay Request.", e.message)
        }
    }

    @Test
    fun `loadGooglePayPaymentData() should throw when pay request cannot be submitted due to status`() = runBlocking {
        `when`(googlePayRepository.isGooglePayReady(listOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD))).thenReturn(
            flowOf(true),
        )
        `when`(payRequestRepository.fetchPayRequest("paySecret123")).thenReturn(createResponse(PayRequestResponse.PayRequestStatus.SUCCESS))
        try {
            viewModel.loadGooglePayPaymentData()
            fail()
        } catch (e: Exception) {
            assertEquals(IllegalStateException::class.java, e.javaClass)
            assertEquals("Pay Request cannot be submitted when status is SUCCESS", e.message)
        }
    }

    @Test
    fun `loadGooglePayPaymentData() should succeed and call paymentsClient with correct params`() = runBlocking {
        `when`(googlePayRepository.isGooglePayReady(listOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD))).thenReturn(
            flowOf(true),
        )
        `when`(payRequestRepository.fetchPayRequest("paySecret123")).thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
        val requestCaptor: ArgumentCaptor<PaymentDataRequest> = ArgumentCaptor.forClass(PaymentDataRequest::class.java)
        `when`(
            paymentsClient.loadPaymentData(
                requestCaptor.capture(),
            ),
        ).thenReturn(FakeTestTask())
        assertTrue(viewModel.loadGooglePayPaymentData().isSuccessful)
        assertEquals(
            """{"apiVersion":2,"apiVersionMinor":0,"allowedPaymentMethods":[{"type":"CARD","parameters":{"allowedAuthMethods":["CRYPTOGRAM_3DS","PAN_ONLY"],"allowedCardNetworks":["VISA","MASTERCARD"]},"tokenizationSpecification":{"type":"PAYMENT_GATEWAY","parameters":{"gateway":"verygoodsecurity","gatewayMerchantId":"ACnqw45PLq1aBztBuxDKkZXJ"}}}],"transactionInfo":{"totalPrice":"1.00","totalPriceStatus":"FINAL","countryCode":"AU","currencyCode":"AUD"},"merchantInfo":{"merchantName":"someMerchant"},"shippingAddressRequired":false}""",
            requestCaptor.value.toJson(),
        )
    }

    @Test
    fun `loadGooglePayPaymentData() should fail`() = runBlocking {
        `when`(googlePayRepository.isGooglePayReady(listOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD))).thenReturn(
            flowOf(true),
        )
        `when`(payRequestRepository.fetchPayRequest("paySecret123")).thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT))
        `when`(
            paymentsClient.loadPaymentData(
                any(),
            ),
        ).thenReturn(FakeTestTask(shouldThrow = true))
        assertFalse(viewModel.loadGooglePayPaymentData().isSuccessful)
    }

    @Test
    fun `handleGooglePayResult() should update result to failed when submit fails`() = runBlocking {
        val requestCaptor: ArgumentCaptor<GooglePayPayRequest> = ArgumentCaptor.forClass(
            GooglePayPayRequest::class.java,
        )
        `when`(payRequestGooglePayRepo.submitPayRequest(eq("paySecret123"), capture(requestCaptor))).thenReturn(false)
        `when`(paymentData.toJson()).thenReturn(paymentDataStr)
        viewModel.result.observeForever(observer)
        viewModel.handleGooglePayResult(paymentData)
        assertEquals(
            """GooglePayPayRequest(google_pay_payload=GooglePayPayload(token=Token(signature=MEYCIQDtf9T3uyyVToB9nIuk1B588gZVHW1/PH6JxUhWJR1buwIhAN+RWnMk6D/JZ0pjH8RDN2so9tTE4svUTOgCTO1c7JTN, intermediateSigningKey=SigningKey(signedKey={"keyValue":"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGFiF/L5cpwK+Tx7wPZmO7HuaXvdn3Rq1ifgAX6CoKqp+vSHieo+DKpv4jIWQkwrIXNH4VkYbGakv3ci0UwqORQ\u003d\u003d","keyExpiration":"1689143240647"}, signatures=[MEYCIQCtQctDXiYjpko0mkjigQNuS5oBq1XMAH3AweiR6C29JAIhAJgbNBnQ+rHi8sC+YWeuYVvSlBSpX9a7PJlrkoY0RZ73]), protocolVersion=ECv2, signedMessage={"encryptedMessage":"oJKPRT4xHjfjyGBDyoDLzBvww990sFk2/oaB+iIWXj2/9S1+dwjEguwkWw5DbYqxDcRu5MGZm2EwpmBdIRge9UU6YFBt8K/SlMR0r/YrBc+/0+QoU2oUnt7KKjscgNfqMhY8aDlFbQJ4xLAENx7wRmtZlIwjyyJFs/e5ikPEZGemQVH279pOEUoVUHkacLsM73j1jpT29dUXYmcDpNYBBqWcBj0A4WMgNzo+nbXcamq9S4dTkz1HloHvDV1Onn2DJVlkjwAhKppUpWbrhQd3BiSmTJM4pxLVTFpzLtrCKGajK7k9YlltuKb6D7FlLELyyqp+HfpST4hQaCl4at8ccm/u0H1OrzLvYKzFPg4lBRq4wOxK7/F7NffcJfa2ZkyGGYodROo0jzpghop7p2JFhrrDEMru1ofHIXsj4QONLRgblRofJNuDb9ti9bxua1pYxAM769e/qOK2NKduQjCx7qIKDYgauZziRgeG2kVc3j1veWvBnj5La4gvEBkmerO0vKFHfYvDjcc+ejfwj/05oEzuSTHdMWsSe3qZ3thplDuoTVgBiRvQo/l5Zxj9c3WRQENEvQni7tBnTsJkSRX9eaRlXcu05uxLleiuDgdQdrvsKrI5cfWjiAUY6nwr91v6rzKNu1uRUYYInrIS4XICW+nHu71Cy4A\u003d","ephemeralPublicKey":"BMHDsGizQGkNQL7sfF7rcxOl5wyKzUuLMQlGdP5A/MqHgo4vCIL5cop+2tJN3g2LfyuMu1xeIT+d8NG3/h2JT6I\u003d","tag":"I5lICOlsJlaVGX/EeAUYbtJdoN6fMi6IwpI3BCwFlmM\u003d"})), paymentType=GOOGLE_PAY)""",
            requestCaptor.value.toString(),
        )
        verify(savedStateHandle)["has_started"] = false
        verify(savedStateHandle)["has_started"] = true
        verifyNoMoreInteractions(savedStateHandle)

        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    "Problem submitting pay request",
                    TyroPayRequestErrorType.SERVER_VALIDATION_ERROR,
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handleGooglePayResult() should update result to success when submit & polling succeeds`() = runBlocking {
        val requestCaptor: ArgumentCaptor<GooglePayPayRequest> = ArgumentCaptor.forClass(
            GooglePayPayRequest::class.java,
        )
        `when`(payRequestGooglePayRepo.submitPayRequest(eq("paySecret123"), capture(requestCaptor))).thenReturn(true)
        `when`(paymentData.toJson()).thenReturn(paymentDataStr)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.SUCCESS))
        viewModel.result.observeForever(observer)

        viewModel.handleGooglePayResult(paymentData)

        verify(observer).onChanged(TyroGooglePayClient.Result.Success)
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handleGooglePayResult() should update result to failed when submit & polling fails`() = runBlocking {
        val requestCaptor: ArgumentCaptor<GooglePayPayRequest> = ArgumentCaptor.forClass(
            GooglePayPayRequest::class.java,
        )
        `when`(payRequestGooglePayRepo.submitPayRequest(eq("paySecret123"), capture(requestCaptor))).thenReturn(true)
        `when`(paymentData.toJson()).thenReturn(paymentDataStr)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(
                createResponse(
                    status = PayRequestResponse.PayRequestStatus.FAILED,
                    errorCode = "CARD_DECLINED",
                    errorMessage = "Not enough funds",
                    gatewayCode = "CARD_REJECTED",
                ),
            )
        viewModel.result.observeForever(observer)

        viewModel.handleGooglePayResult(paymentData)
        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    errorMessage = "Not enough funds",
                    errorType = TyroPayRequestErrorType.SERVER_ERROR,
                    errorCode = "CARD_DECLINED",
                    gatewayCode = "CARD_REJECTED",
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handleGooglePayResult() should return RUN_3DS when submit 3ds is required`() = runBlocking {
        val requestCaptor: ArgumentCaptor<GooglePayPayRequest> = ArgumentCaptor.forClass(
            GooglePayPayRequest::class.java,
        )
        `when`(payRequestGooglePayRepo.submitPayRequest(eq("paySecret123"), capture(requestCaptor))).thenReturn(true)
        `when`(paymentData.toJson()).thenReturn(paymentDataStr)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.AWAITING_AUTHENTICATION))

        val res = viewModel.handleGooglePayResult(paymentData)
        assertEquals(PaymentHandlingResult.RUN_3DS, res)
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handleGooglePayResult() should update result to failed when processing takes too long and times out`() = runBlocking {
        val requestCaptor: ArgumentCaptor<GooglePayPayRequest> = ArgumentCaptor.forClass(
            GooglePayPayRequest::class.java,
        )
        `when`(payRequestGooglePayRepo.submitPayRequest(eq("paySecret123"), capture(requestCaptor))).thenReturn(true)
        `when`(paymentData.toJson()).thenReturn(paymentDataStr)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))

        viewModel.result.observeForever(observer)

        viewModel.handleGooglePayResult(paymentData)
        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    errorMessage = "Pay Request timed out processing",
                    errorType = TyroPayRequestErrorType.SERVER_ERROR,
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handleGooglePayResult() should update result to failed when unexpected status returned`() = runBlocking {
        val requestCaptor: ArgumentCaptor<GooglePayPayRequest> = ArgumentCaptor.forClass(
            GooglePayPayRequest::class.java,
        )
        `when`(payRequestGooglePayRepo.submitPayRequest(eq("paySecret123"), capture(requestCaptor))).thenReturn(true)
        `when`(paymentData.toJson()).thenReturn(paymentDataStr)
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.VOIDED))

        viewModel.result.observeForever(observer)

        viewModel.handleGooglePayResult(paymentData)
        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    errorMessage = "Pay Request returned unexpected status",
                    errorType = TyroPayRequestErrorType.UNKNOWN_ERROR,
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handlePayCompletionFlow() should throw when cannot fetch pay request`() = runBlocking {
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(null)

        try {
            viewModel.handlePayCompletionFlow("paySecret123")
            fail()
        } catch (e: java.lang.Exception) {
            assertEquals(IllegalStateException::class.java, e.javaClass)
            assertEquals("Could not fetch Pay Request.", e.message)
        }
    }

    @Test
    fun `handlePayCompletionFlow() should update result to success when polling succeeds`() = runBlocking {
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.SUCCESS))
        viewModel.result.observeForever(observer)

        viewModel.handlePayCompletionFlow("paySecret123")

        verify(savedStateHandle)["polling_started"] = true
        verifyNoMoreInteractions(savedStateHandle)

        verify(observer).onChanged(TyroGooglePayClient.Result.Success)
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handlePayCompletionFlow() should update result to fail when polling results in a fail`() = runBlocking {
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.PROCESSING))
            .thenReturn(
                createResponse(
                    status = PayRequestResponse.PayRequestStatus.FAILED,
                    errorCode = "TOO_RICH_YO",
                    gatewayCode = "BILLIONAIRE",
                ),
            )
        viewModel.result.observeForever(observer)

        viewModel.handlePayCompletionFlow("paySecret123")

        verify(savedStateHandle)["polling_started"] = true
        verifyNoMoreInteractions(savedStateHandle)

        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    errorMessage = "Pay Request Failed",
                    errorType = TyroPayRequestErrorType.SERVER_ERROR,
                    errorCode = "TOO_RICH_YO",
                    gatewayCode = "BILLIONAIRE",
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handle3DSCompletionFlow() should update result to success when pay request is in SUCCESS status`() = runBlocking {
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.SUCCESS, null, null, null, PayRequestResponse.ThreeDSecureStatus.SUCCESS))
        viewModel.result.observeForever(observer)

        viewModel.handle3DSCompletionFlow("paySecret123")

        verifyNoMoreInteractions(savedStateHandle)

        verify(observer).onChanged(TyroGooglePayClient.Result.Success)
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handle3DSCompletionFlow() should update result to 3ds failed error when pay request is in FAILED status`() = runBlocking {
        `when`(payRequestRepository.fetchPayRequest("paySecret123"))
            .thenReturn(createResponse(PayRequestResponse.PayRequestStatus.FAILED, null, null, null, PayRequestResponse.ThreeDSecureStatus.FAILED))
        viewModel.result.observeForever(observer)

        viewModel.handle3DSCompletionFlow("paySecret123")

        verifyNoMoreInteractions(savedStateHandle)

        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    "3DS failed",
                    TyroPayRequestErrorType.THREED_SECURE_ERROR,
                    errorCode = "FAILED",
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `handle3dsWebviewClose() should update with failed result`() = runBlocking {
        viewModel.result.observeForever(observer)
        viewModel.handle3dsWebviewClose()
        verify(observer).onChanged(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    "Process ended unexpectedly, fetch Pay Request Status for result.",
                    TyroPayRequestErrorType.UNKNOWN_ERROR,
                    ErrorCode.PROCESS_ENDED_UNEXPECTEDLY_FETCH_PAY_REQUEST_STATUS.toString(),
                ),
            ),
        )
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `get hasStarted should return value from the savedStateHandle`() {
        `when`(savedStateHandle.get<Boolean>("has_started")).thenReturn(true).thenReturn(false)
        assert(viewModel.hasStarted)
        assertFalse(viewModel.hasStarted)
    }

    @Test
    fun `set hasStarted should set value to savedStateHandle`() {
        viewModel.hasStarted = true
        verify(savedStateHandle)["has_started"] = true
        viewModel.hasStarted = false
        verify(savedStateHandle)["has_started"] = false
        verifyNoMoreInteractions(savedStateHandle)
    }

    @Test
    fun `get pollingStarted should return value from the savedStateHandle`() {
        `when`(savedStateHandle.get<Boolean>("polling_started")).thenReturn(true).thenReturn(false)
        assert(viewModel.pollingStarted)
        assertFalse(viewModel.pollingStarted)
    }

    @Test
    fun `set pollingStarted should set value to savedStateHandle`() {
        viewModel.pollingStarted = true
        verify(savedStateHandle)["polling_started"] = true
        viewModel.pollingStarted = false
        verify(savedStateHandle)["polling_started"] = false
        verifyNoMoreInteractions(savedStateHandle)
    }

    @Test
    fun `Factory should create viewModel when liveMode = false and not error`() {
        TyroGooglePayViewModel.Factory(TyroGooglePayActivityContract.PayRequestParams("paySecret123", TyroGooglePayClient.Config(liveMode = false, merchantName = "merchantName")))
    }

    fun `Factory should create viewModel when liveMode = true and not error`() {
        TyroGooglePayViewModel.Factory(TyroGooglePayActivityContract.PayRequestParams("paySecret123", TyroGooglePayClient.Config(liveMode = true, merchantName = "merchantName", listOf(GooglePayCardNetwork.MASTERCARD, GooglePayCardNetwork.VISA))))
    }

    private fun createResponse(
        status: PayRequestResponse.PayRequestStatus,
        errorCode: String? = null,
        errorMessage: String? = null,
        gatewayCode: String? = null,
        threeDSStatus: PayRequestResponse.ThreeDSecureStatus? = null,
    ): PayRequestResponse {
        return PayRequestResponse(
            PayRequestResponse.PayRequestOrigin("order123", "ref123", "name123"),
            status,
            PayRequestResponse.Capture(PayRequestResponse.CaptureMethod.AUTOMATIC, null),
            PayRequestResponse.AmountWithCurrency(100L, "AUD"),
            threeDSStatus?.let { PayRequestResponse.ThreeDSecure(it, null, null) },
            errorCode,
            errorMessage,
            gatewayCode,
        )
    }

    private class FakeTestTask(private val shouldThrow: Boolean = false) : Task<PaymentData>() {
        override fun <X : Throwable?> getResult(p0: Class<X>): PaymentData {
            if (shouldThrow) throw ApiException(Status.RESULT_TIMEOUT)
            return Mockito.mock()
        }

        override fun addOnCompleteListener(p0: OnCompleteListener<PaymentData>): Task<PaymentData> {
            p0.onComplete(this)
            return FakeTestTask(shouldThrow)
        }

        override fun addOnFailureListener(p0: OnFailureListener): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun getException(): java.lang.Exception? {
            throw UnsupportedOperationException()
        }

        override fun getResult(): PaymentData {
            throw UnsupportedOperationException()
        }

        override fun isCanceled(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun isComplete(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun addOnSuccessListener(
            p0: Executor,
            p1: OnSuccessListener<in PaymentData>,
        ): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun addOnSuccessListener(
            p0: Activity,
            p1: OnSuccessListener<in PaymentData>,
        ): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun addOnSuccessListener(p0: OnSuccessListener<in PaymentData>): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun addOnCompleteListener(
            p0: Activity,
            p1: OnCompleteListener<PaymentData>,
        ): Task<PaymentData> {
            throw UnsupportedOperationException()
        }

        override fun isSuccessful(): Boolean {
            if (shouldThrow) return false
            return true
        }
    }
}
