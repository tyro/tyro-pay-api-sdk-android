package com.tyro.payapi.googlepayclient.googleplay

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.android.gms.wallet.WalletConstants
import com.tyro.payapi.googlepayclient.googlepay.PaymentsClientFactory
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentsClientFactoryTest {
    private lateinit var fact: PaymentsClientFactory

    @Before
    fun setUp() {
        fact = PaymentsClientFactory()
    }

    @Test
    fun `createPaymentsClient() should return client when isLive = false`() {
        val paymentsClient = fact.createPaymentsClient(false, getApplicationContext())
        assertNotNull(paymentsClient)
        assertEquals(WalletConstants.ENVIRONMENT_TEST, paymentsClient.apiOptions.environment)
    }

    @Test
    fun `createPaymentsClient() should return client when isLive = true`() {
        val paymentsClient = fact.createPaymentsClient(true, getApplicationContext())
        assertNotNull(paymentsClient)
        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, paymentsClient.apiOptions.environment)
    }
}
