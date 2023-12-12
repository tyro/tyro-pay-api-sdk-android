package com.tyro.payapi.googlepayclient.googlepay

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.tyro.payapi.googlepayclient.constants.GooglePayCardNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber

fun interface GooglePayRepository {
    fun isGooglePayReady(allowedCardNetworks: List<GooglePayCardNetwork>?): Flow<Boolean>
}

internal class DefaultGooglePayRepository(
    private val paymentsClient: PaymentsClient,
    private val googlePayRequestFactory: GooglePayRequestFactory,
) : GooglePayRepository {
    override fun isGooglePayReady(allowedCardNetworks: List<GooglePayCardNetwork>?): Flow<Boolean> {
        val isReadyStateFlow = MutableStateFlow<Boolean?>(null)

        val isGoogleReadyPayRequest = googlePayRequestFactory.createIsGooglePayReadyRequest(allowedCardNetworks)
        Timber.d("Google Pay Ready request ")
        Timber.d(isGoogleReadyPayRequest.toString())
        paymentsClient.isReadyToPay(IsReadyToPayRequest.fromJson(isGoogleReadyPayRequest.toString()))
            .addOnCompleteListener { task ->
                try {
                    val isReady = task.getResult(ApiException::class.java)
                    Timber.d("isReadyToPay = $isReady")
                    isReadyStateFlow.value = isReady
                } catch (exception: ApiException) {
                    Timber.d("isReadyToPay threw exception")
                    Timber.e(exception)
                    isReadyStateFlow.value = false
                }
            }
        return isReadyStateFlow.filterNotNull()
    }
}
