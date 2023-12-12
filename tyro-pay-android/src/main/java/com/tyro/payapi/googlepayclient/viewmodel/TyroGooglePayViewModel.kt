package com.tyro.payapi.googlepayclient.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.gson.Gson
import com.tyro.payapi.googlepayclient.TyroGooglePayClient
import com.tyro.payapi.googlepayclient.constants.GooglePayConstants
import com.tyro.payapi.googlepayclient.googlepay.DefaultGooglePayRepository
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRepository
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRequestFactory
import com.tyro.payapi.googlepayclient.googlepay.PaymentsClientFactory
import com.tyro.payapi.googlepayclient.view.TyroGooglePayActivityContract
import com.tyro.payapi.payrequest.constants.ErrorCode
import com.tyro.payapi.payrequest.constants.TyroPayRequestErrorType
import com.tyro.payapi.payrequest.model.GooglePayPayRequest
import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.model.TyroPayRequestError
import com.tyro.payapi.payrequest.repository.DefaultPayRequestGooglePayRepository
import com.tyro.payapi.payrequest.repository.DefaultPayRequestRepository
import com.tyro.payapi.payrequest.repository.PayRequestGooglePayRepository
import com.tyro.payapi.payrequest.repository.PayRequestRepository
import com.tyro.payapi.payrequest.service.PayRequestPoller
import com.tyro.payapi.payrequest.service.PayRequestStatusPoller
import com.tyro.payapi.retrofit.RetrofitFactory
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import timber.log.Timber

internal class TyroGooglePayViewModel(
    private val paymentsClient: PaymentsClient,
    private val params: TyroGooglePayActivityContract.Params,
    private val payRequestRepo: PayRequestRepository,
    private val payRequestGooglePayRepo: PayRequestGooglePayRepository,
    private val googlePayRepository: GooglePayRepository,
    private val googlePayRequestFactory: GooglePayRequestFactory,
    private val savedStateHandle: SavedStateHandle,
    private val payRequestStatusPoller: PayRequestStatusPoller,
    private val gson: Gson,
) : ViewModel() {
    // keep track if this process has already started and
    // should be persisted
    var hasStarted: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_STARTED_KEY) == true
        set(value) = savedStateHandle.set(HAS_STARTED_KEY, value)
    var pollingStarted: Boolean
        get() = savedStateHandle.get<Boolean>(POLLING_STARTED_KEY) == true
        set(value) = savedStateHandle.set(POLLING_STARTED_KEY, value)

    private val nonDistinctResult = MutableLiveData<TyroGooglePayClient.Result>()
    internal val result = nonDistinctResult.distinctUntilChanged()

    fun updateResult(result: TyroGooglePayClient.Result) {
        nonDistinctResult.value = result
    }

    private suspend fun isGogglePayReady(): Boolean =
        googlePayRepository.isGooglePayReady(params.config.allowedCardNetworks).first()

    fun handle3dsWebviewClose() {
        updateResult(
            TyroGooglePayClient.Result.Failed(
                TyroPayRequestError(
                    "Process ended unexpectedly, fetch Pay Request Status for result.",
                    TyroPayRequestErrorType.UNKNOWN_ERROR,
                    ErrorCode.PROCESS_ENDED_UNEXPECTEDLY_FETCH_PAY_REQUEST_STATUS.toString(),
                ),
            ),
        )
    }

    suspend fun loadGooglePayPaymentData(): Task<PaymentData> {
        Timber.d("loadPaymentData()")
        check(isGogglePayReady()) {
            "Google Pay is not available"
        }
        val payRequest = requireNotNull(payRequestRepo.fetchPayRequest(params.paySecret)) {
            "Could not fetch Pay Request."
        }
        check(SUBMITTABLE_PAY_REQUEST_STATUSES.contains(payRequest.status)) {
            "Pay Request cannot be submitted when status is ${payRequest.status}"
        }

        val googlePayPaymentData = googlePayRequestFactory.createGooglePayPaymentDataRequest(
            payRequest.total.amount,
            params.config.merchantName,
            params.config.allowedCardNetworks,
        )
        val googlePayPaymentDataRequest =
            PaymentDataRequest.fromJson(googlePayPaymentData.toString())
        Timber.d("paymentDataRequest ${googlePayPaymentDataRequest.toJson()}")
        return paymentsClient.loadPaymentData(googlePayPaymentDataRequest)
    }

    suspend fun handleGooglePayResult(paymentData: PaymentData): PaymentHandlingResult {
        Timber.d("handleGooglePayResult(): token generated")
        hasStarted = false // reset state here if process interrupted, we need to fetch token again
        val googlePayTokenJsonString = JSONObject(paymentData.toJson()).getJSONObject(
            GooglePayConstants.PAYMENT_METHOD_DATA,
        ).getJSONObject(
            GooglePayConstants.TOKENIZATION_DATA,
        ).getString(GooglePayConstants.TOKEN)
        Timber.d(googlePayTokenJsonString)
        val googlePayToken = gson.fromJson(googlePayTokenJsonString, GooglePayPayRequest.Token::class.java)
        val googlePayPayRequest = GooglePayPayRequest(GooglePayPayRequest.GooglePayPayload(googlePayToken))

        val payRequestSubmitSuccess = payRequestGooglePayRepo.submitPayRequest(params.paySecret, googlePayPayRequest)
        hasStarted = true // we have submitted the request, don't restart google pay
        if (!payRequestSubmitSuccess) {
            updateResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(
                        "Problem submitting pay request",
                        TyroPayRequestErrorType.SERVER_VALIDATION_ERROR,
                    ),
                ),
            )
            return PaymentHandlingResult.ENDED
        }

        return handlePayCompletionFlow(params.paySecret)
    }

    suspend fun handlePayCompletionFlow(paySecret: String): PaymentHandlingResult {
        pollingStarted = true
        val payRequestResponse: PayRequestResponse? =
            payRequestStatusPoller.pollForInitialStatusUpdate(paySecret)
        checkNotNull(payRequestResponse) {
            "Could not fetch Pay Request."
        }
        Timber.d("payRequest status result $${payRequestResponse.status}")
        when (payRequestResponse.status) {
            PayRequestResponse.PayRequestStatus.PROCESSING -> updateResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(
                        "Pay Request timed out processing",
                        TyroPayRequestErrorType.SERVER_ERROR,
                    ),
                ),
            )
            PayRequestResponse.PayRequestStatus.FAILED -> updateResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(
                        payRequestResponse.errorMessage ?: "Pay Request Failed",
                        TyroPayRequestErrorType.SERVER_ERROR,
                        payRequestResponse.errorCode,
                        payRequestResponse.gatewayCode,
                    ),
                ),
            )
            PayRequestResponse.PayRequestStatus.AWAITING_AUTHENTICATION -> return PaymentHandlingResult.RUN_3DS
            PayRequestResponse.PayRequestStatus.SUCCESS -> updateResult(TyroGooglePayClient.Result.Success)
            else -> updateResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(
                        "Pay Request returned unexpected status",
                        TyroPayRequestErrorType.UNKNOWN_ERROR,
                    ),
                ),
            )
        }

        return PaymentHandlingResult.ENDED
    }

    suspend fun handle3DSCompletionFlow(paySecret: String) {
        val payRequest = requireNotNull(payRequestRepo.fetchPayRequest(paySecret)) {
            "Could not fetch Pay Request."
        }
        if (payRequest.status == PayRequestResponse.PayRequestStatus.SUCCESS && payRequest.threeDSecure!!.status == PayRequestResponse.ThreeDSecureStatus.SUCCESS) {
            updateResult(
                TyroGooglePayClient.Result.Success,
            )
        } else {
            updateResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(
                        "3DS failed",
                        TyroPayRequestErrorType.THREED_SECURE_ERROR,
                        errorCode = payRequest.threeDSecure?.status?.toString(),
                    ),
                ),
            )
        }
    }

    internal class Factory(
        private val params: TyroGooglePayActivityContract.Params,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(extras[APPLICATION_KEY])
            val googlePayRequestFactory = GooglePayRequestFactory()
            val paymentsClientFactory = PaymentsClientFactory()
            val paymentsClient =
                paymentsClientFactory.createPaymentsClient(params.config.liveMode, application)
            val retrofitFactory = RetrofitFactory()
            val payRequestGooglePayFactory = DefaultPayRequestGooglePayRepository.Factory()
            val payRequestRepo = DefaultPayRequestRepository(retrofitFactory)
            return TyroGooglePayViewModel(
                paymentsClientFactory.createPaymentsClient(params.config.liveMode, application),
                params,
                payRequestRepo,
                if (params.config.liveMode) {
                    payRequestGooglePayFactory.createLiveRepository(retrofitFactory)
                } else payRequestGooglePayFactory.createSandboxRepository(retrofitFactory),
                DefaultGooglePayRepository(paymentsClient, googlePayRequestFactory),
                googlePayRequestFactory,
                extras.createSavedStateHandle(),
                PayRequestStatusPoller(PayRequestPoller(payRequestRepo)),
                Gson(),
            ) as T
        }
    }
    companion object {
        const val HAS_STARTED_KEY = "has_started"
        const val POLLING_STARTED_KEY = "polling_started"
        val SUBMITTABLE_PAY_REQUEST_STATUSES = listOf(
            PayRequestResponse.PayRequestStatus.AWAITING_PAYMENT_INPUT,
            PayRequestResponse.PayRequestStatus.AWAITING_AUTHENTICATION,
            PayRequestResponse.PayRequestStatus.FAILED,
        )
    }
}
enum class PaymentHandlingResult {
    ENDED,
    RUN_3DS,
}
