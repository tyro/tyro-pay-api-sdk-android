package com.tyro.payapi.googlepayclient

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.tyro.payapi.googlepayclient.googlepay.DefaultGooglePayRepository
import com.tyro.payapi.googlepayclient.view.TyroGooglePayActivityContract
import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.model.TyroPayRequestError
import com.tyro.payapi.payrequest.repository.DefaultPayRequestRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.lang.Exception
import java.lang.IllegalStateException

@RunWith(RobolectricTestRunner::class)
class TyroGooglePayClientTest {
    private val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
        TestFragment()
    }
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val isGooglePayReadyInvocations = mutableListOf<Boolean>()
    private val paymentResults = mutableListOf<TyroGooglePayClient.Result>()

    private val isGoogleReadyListener = TyroGooglePayClient.GooglePayReadyListener(isGooglePayReadyInvocations::add)
    private val paymentResultListener = TyroGooglePayClient.PaymentResultListener(paymentResults::add)

    @Mock
    private lateinit var googlePayRepository: DefaultGooglePayRepository

    @Mock
    private lateinit var applicationInfo: ApplicationInfo

    @Mock
    private lateinit var payRequestRepository: DefaultPayRequestRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should return googlePayReady = false when google pay is not available`() {
        `when`(googlePayRepository.isGooglePayReady(any())).thenReturn(flowOf(false))
        scenario.onFragment { fragment ->
            TyroGooglePayClient(
                testScope,
                applicationInfo,
                TyroGooglePayClient.Config(
                    false,
                    "Tyro Test Merchant",
                ),
                isGoogleReadyListener,
                googlePayRepository,
                activityResultLauncher = fragment.registerForActivityResult(
                    TyroGooglePayActivityContract(),
                    FakeActivityResultRegistry(TyroGooglePayClient.Result.Success),
                ) {
                    paymentResultListener.onPaymentResult(it)
                },
                payRequestRepository,
            )
            scenario.moveToState(Lifecycle.State.STARTED)
            assertFalse(isGooglePayReadyInvocations[0])
        }
    }

    @Test
    fun `launchGooglePay() should return success result when Google Pay is available`() {
        `when`(googlePayRepository.isGooglePayReady(any())).thenReturn(flowOf(true))
        scenario.onFragment { fragment ->
            val client = TyroGooglePayClient(
                testScope,
                applicationInfo,
                TyroGooglePayClient.Config(
                    false,
                    "Tyro Test Merchant",
                ),
                isGoogleReadyListener,
                googlePayRepository,
                activityResultLauncher = fragment.registerForActivityResult(
                    TyroGooglePayActivityContract(),
                    FakeActivityResultRegistry(TyroGooglePayClient.Result.Success),
                ) {
                    paymentResultListener.onPaymentResult(it)
                },
                payRequestRepository,
            )
            scenario.moveToState(Lifecycle.State.RESUMED)
            assert(isGooglePayReadyInvocations[0])
            client.launchGooglePay("paySecret123")
            assertEquals(TyroGooglePayClient.Result.Success, paymentResults[0])
        }
    }

    @Test
    fun `launchGooglePay() should return cancelled result when Google Pay is available`() {
        `when`(googlePayRepository.isGooglePayReady(any())).thenReturn(flowOf(true))
        scenario.onFragment { fragment ->
            val client = TyroGooglePayClient(
                testScope,
                applicationInfo,
                TyroGooglePayClient.Config(
                    false,
                    "Tyro Test Merchant",
                ),
                isGoogleReadyListener,
                googlePayRepository,
                activityResultLauncher = fragment.registerForActivityResult(
                    TyroGooglePayActivityContract(),
                    FakeActivityResultRegistry(TyroGooglePayClient.Result.Cancelled),
                ) {
                    paymentResultListener.onPaymentResult(it)
                },
                payRequestRepository,
            )
            scenario.moveToState(Lifecycle.State.STARTED)
            assert(isGooglePayReadyInvocations[0])
            client.launchGooglePay("paySecret123")
            assertEquals(TyroGooglePayClient.Result.Cancelled, paymentResults[0])
        }
    }

    @Test
    fun `launchGooglePay() should return failed result when Google Pay is available`() {
        `when`(googlePayRepository.isGooglePayReady(any())).thenReturn(flowOf(true))
        scenario.onFragment { fragment ->
            val client = TyroGooglePayClient(
                testScope,
                applicationInfo,
                TyroGooglePayClient.Config(
                    true,
                    "Tyro Test Merchant",
                ),
                isGoogleReadyListener,
                googlePayRepository,
                activityResultLauncher = fragment.registerForActivityResult(
                    TyroGooglePayActivityContract(),
                    FakeActivityResultRegistry(
                        TyroGooglePayClient.Result.Failed(
                            TyroPayRequestError(
                                "err message",
                            ),
                        ),
                    ),
                ) {
                    paymentResultListener.onPaymentResult(it)
                },
                payRequestRepository,
            )
            scenario.moveToState(Lifecycle.State.STARTED)
            assert(isGooglePayReadyInvocations[0])
            client.launchGooglePay("paySecret123")
            assertEquals(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(
                        "err message",
                    ),
                ),
                paymentResults[0],
            )
        }
    }

    @Test
    fun `launchGooglePay() should throw when Google Pay aint available`() {
        `when`(googlePayRepository.isGooglePayReady(any())).thenReturn(flowOf(false))
        scenario.onFragment { fragment ->
            val client = TyroGooglePayClient(
                testScope,
                applicationInfo,
                TyroGooglePayClient.Config(
                    true,
                    "Tyro Test Merchant",
                ),
                isGoogleReadyListener,
                googlePayRepository,
                activityResultLauncher = fragment.registerForActivityResult(
                    TyroGooglePayActivityContract(),
                    FakeActivityResultRegistry(
                        TyroGooglePayClient.Result.Failed(
                            TyroPayRequestError(
                                "err message",
                            ),
                        ),
                    ),
                ) {
                    paymentResultListener.onPaymentResult(it)
                },
                payRequestRepository,
            )
            scenario.moveToState(Lifecycle.State.CREATED)
            assertFalse(isGooglePayReadyInvocations[0])
            try {
                client.launchGooglePay("paySecret123")
                fail()
            } catch (e: Exception) {
                assertEquals(IllegalStateException::class.java, e.javaClass)
                assertEquals("Google Pay must be available on this device in order to call this method", e.message)
            }
        }
    }

    @Test
    fun `fetchPayRequest() should return the pay request`() {
        `when`(googlePayRepository.isGooglePayReady(any())).thenReturn(flowOf(true))

        scenario.onFragment { fragment ->
            val client = TyroGooglePayClient(
                testScope,
                applicationInfo,
                TyroGooglePayClient.Config(
                    false,
                    "Tyro Test Merchant",
                ),
                isGoogleReadyListener,
                googlePayRepository,
                activityResultLauncher = fragment.registerForActivityResult(
                    TyroGooglePayActivityContract(),
                    FakeActivityResultRegistry(TyroGooglePayClient.Result.Success),
                ) {
                    paymentResultListener.onPaymentResult(it)
                },
                payRequestRepository,
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            runBlocking {
                val fakeResp = createResponse(PayRequestResponse.PayRequestStatus.SUCCESS)
                `when`(payRequestRepository.fetchPayRequest("paySecret123")).thenReturn(
                    fakeResp,
                )
                val response = client.fetchPayRequest("paySecret123")
                assertEquals(fakeResp, response)
            }
        }
    }

    private fun createResponse(
        status: PayRequestResponse.PayRequestStatus,
        errorCode: String? = null,
        errorMessage: String? = null,
        gatewayCode: String? = null,
    ): PayRequestResponse {
        return PayRequestResponse(
            PayRequestResponse.PayRequestOrigin("order123", "ref123", "name123"),
            status,
            PayRequestResponse.Capture(PayRequestResponse.CaptureMethod.AUTOMATIC, null),
            PayRequestResponse.AmountWithCurrency(100L, "AUD"),
            PayRequestResponse.ThreeDSecure(PayRequestResponse.ThreeDSecureStatus.SUCCESS, null, null),
            errorCode,
            errorMessage,
            gatewayCode,
        )
    }

    private class FakeActivityResultRegistry(
        private val result: TyroGooglePayClient.Result,
    ) : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            dispatchResult(
                requestCode,
                result,
            )
        }
    }

    internal class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View = FrameLayout(inflater.context)
    }
}
