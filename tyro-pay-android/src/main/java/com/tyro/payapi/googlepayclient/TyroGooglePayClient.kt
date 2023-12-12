package com.tyro.payapi.googlepayclient

import android.content.pm.ApplicationInfo
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tyro.payapi.googlepayclient.constants.GooglePayCardNetwork
import com.tyro.payapi.googlepayclient.googlepay.DefaultGooglePayRepository
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRepository
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRequestFactory
import com.tyro.payapi.googlepayclient.googlepay.PaymentsClientFactory
import com.tyro.payapi.googlepayclient.view.TyroGooglePayActivityContract
import com.tyro.payapi.log.LoggerUtil
import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.model.TyroPayRequestError
import com.tyro.payapi.payrequest.repository.DefaultPayRequestRepository
import com.tyro.payapi.payrequest.repository.PayRequestRepository
import com.tyro.payapi.retrofit.RetrofitFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class TyroGooglePayClient internal constructor(
    lifecycleScope: CoroutineScope,
    applicationInfo: ApplicationInfo,
    private val config: Config,
    private val googlePayReadyListener: GooglePayReadyListener,
    private val googlePayRepository: GooglePayRepository,
    private val activityResultLauncher: ActivityResultLauncher<TyroGooglePayActivityContract.Params>,
    private val payRequestRepo: PayRequestRepository,
) {
    private var isGooglePayReady = false

    init {
        LoggerUtil.enableLogsIfDebug(applicationInfo)
        lifecycleScope.launch {
            googlePayReadyListener.onGooglePayReady(
                googlePayRepository.isGooglePayReady(config.allowedCardNetworks).first().also {
                    isGooglePayReady = it
                },
            )
        }
    }

    constructor(
        activity: ComponentActivity,
        config: Config,
        googlePayReadyListener: GooglePayReadyListener,
        paymentResultListener: PaymentResultListener,
    ) : this(
        activity.lifecycleScope,
        activity.applicationInfo,
        config,
        googlePayReadyListener,
        DefaultGooglePayRepository(
            PaymentsClientFactory().createPaymentsClient(config.liveMode, activity.application),
            GooglePayRequestFactory(),
        ),
        activity.registerForActivityResult(
            TyroGooglePayActivityContract(),
        ) {
            paymentResultListener.onPaymentResult(it)
        },
        DefaultPayRequestRepository(RetrofitFactory()),
    )

    constructor(
        fragment: Fragment,
        config: Config,
        googlePayReadyListener: GooglePayReadyListener,
        paymentResultListener: PaymentResultListener,
    ) : this(
        fragment.viewLifecycleOwner.lifecycleScope,
        fragment.requireActivity().applicationInfo,
        config,
        googlePayReadyListener,
        DefaultGooglePayRepository(
            PaymentsClientFactory().createPaymentsClient(config.liveMode, fragment.requireActivity().application),
            GooglePayRequestFactory(),
        ),
        fragment.registerForActivityResult(
            TyroGooglePayActivityContract(),
        ) {
            paymentResultListener.onPaymentResult(it)
        },
        DefaultPayRequestRepository(RetrofitFactory()),
    )

    fun launchGooglePay(paySecret: String) {
        check(isGooglePayReady) {
            "Google Pay must be available on this device in order to call this method"
        }

        activityResultLauncher.launch(
            TyroGooglePayActivityContract.PayRequestParams(
                paySecret = paySecret,
                config = config,
            ),
        )
    }

    suspend fun fetchPayRequest(paySecret: String): PayRequestResponse? {
        return payRequestRepo.fetchPayRequest(paySecret)
    }

    @Parcelize
    data class Config @JvmOverloads constructor(
        val liveMode: Boolean = false,
        val merchantName: String,
        val allowedCardNetworks: List<GooglePayCardNetwork> = GooglePayCardNetwork.values().asList(),
    ) : Parcelable

    fun interface GooglePayReadyListener {
        fun onGooglePayReady(available: Boolean)
    }

    fun interface PaymentResultListener {
        fun onPaymentResult(result: Result)
    }

    sealed class Result : Parcelable {
        @Parcelize
        object Success : Result()

        @Parcelize
        data class Failed(
            val error: TyroPayRequestError,
        ) : Result()

        @Parcelize
        object Cancelled : Result()
    }
}
