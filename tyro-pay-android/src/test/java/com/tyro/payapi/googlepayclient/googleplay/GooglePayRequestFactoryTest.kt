package com.tyro.payapi.googlepayclient.googleplay

import com.tyro.payapi.googlepayclient.constants.GooglePayCardNetwork
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRequestFactory
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayRequestFactoryTest {
    private lateinit var googlePayRequestFactory: GooglePayRequestFactory

    @Before
    fun setUp() {
        googlePayRequestFactory = GooglePayRequestFactory()
    }

    @Test
    fun `createIsGooglePayReadyRequest() should return correct json object when no cards specified`() {
        val json = googlePayRequestFactory.createIsGooglePayReadyRequest(null)
        assertEquals(
            """
            {"apiVersion":2,"apiVersionMinor":0,"allowedPaymentMethods":[{"type":"CARD","parameters":{"allowedAuthMethods":["CRYPTOGRAM_3DS","PAN_ONLY"],"allowedCardNetworks":["AMEX","JCB","MASTERCARD","VISA"]}}]}
            """.trimIndent(),
            json.toString(),
        )
    }

    @Test
    fun `createIsGooglePayReadyRequest() should return correct json object when cards specified`() {
        val json = googlePayRequestFactory.createIsGooglePayReadyRequest(
            listOf(
                GooglePayCardNetwork.MASTERCARD,
                GooglePayCardNetwork.VISA,
            ),
        )
        assertEquals(
            """
            {"apiVersion":2,"apiVersionMinor":0,"allowedPaymentMethods":[{"type":"CARD","parameters":{"allowedAuthMethods":["CRYPTOGRAM_3DS","PAN_ONLY"],"allowedCardNetworks":["MASTERCARD","VISA"]}}]}
            """.trimIndent(),
            json.toString(),
        )
    }

    @Test
    fun `createGooglePayPaymentDataRequest() should return correct json when no cards specified`() {
        val json = googlePayRequestFactory.createGooglePayPaymentDataRequest(110L, "Awesome Merchant", null)
        assertEquals(
            """
            {"apiVersion":2,"apiVersionMinor":0,"allowedPaymentMethods":[{"type":"CARD","parameters":{"allowedAuthMethods":["CRYPTOGRAM_3DS","PAN_ONLY"],"allowedCardNetworks":["AMEX","JCB","MASTERCARD","VISA"]},"tokenizationSpecification":{"type":"PAYMENT_GATEWAY","parameters":{"gateway":"verygoodsecurity","gatewayMerchantId":"ACnqw45PLq1aBztBuxDKkZXJ"}}}],"transactionInfo":{"totalPrice":"1.10","totalPriceStatus":"FINAL","countryCode":"AU","currencyCode":"AUD"},"merchantInfo":{"merchantName":"Awesome Merchant"},"shippingAddressRequired":false}
            """.trimIndent(),
            json.toString(),
        )
    }

    @Test
    fun `createGooglePayPaymentDataRequest() should return correct json when cards specified`() {
        val json = googlePayRequestFactory.createGooglePayPaymentDataRequest(
            696969L,
            "ABC",
            listOf(
                GooglePayCardNetwork.AMEX,
            ),
        )
        assertEquals(
            """
            {"apiVersion":2,"apiVersionMinor":0,"allowedPaymentMethods":[{"type":"CARD","parameters":{"allowedAuthMethods":["CRYPTOGRAM_3DS","PAN_ONLY"],"allowedCardNetworks":["AMEX"]},"tokenizationSpecification":{"type":"PAYMENT_GATEWAY","parameters":{"gateway":"verygoodsecurity","gatewayMerchantId":"ACnqw45PLq1aBztBuxDKkZXJ"}}}],"transactionInfo":{"totalPrice":"6969.69","totalPriceStatus":"FINAL","countryCode":"AU","currencyCode":"AUD"},"merchantInfo":{"merchantName":"ABC"},"shippingAddressRequired":false}
            """.trimIndent(),
            json.toString(),
        )
    }
}
