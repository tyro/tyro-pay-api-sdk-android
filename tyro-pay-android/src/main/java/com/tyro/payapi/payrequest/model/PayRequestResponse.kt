package com.tyro.payapi.payrequest.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PayRequestResponse(
    val origin: PayRequestOrigin,
    val status: PayRequestStatus,
    val capture: Capture?,
    val total: AmountWithCurrency,
    val threeDSecure: ThreeDSecure?,
    val errorCode: String?,
    val errorMessage: String?,
    val gatewayCode: String?,
) : Parcelable {

    @Parcelize
    data class ThreeDSecure(
        val status: ThreeDSecureStatus,
        val methodURL: String?,
        val challengeURL: String?,
    ) : Parcelable

    enum class ThreeDSecureStatus {
        AWAITING_3DS_METHOD,
        AWAITING_AUTH,
        AWAITING_CHALLENGE,
        SUCCESS,
        FAILED,
    }

    @Parcelize
    data class Capture(
        val method: CaptureMethod,
        val total: AmountWithCurrency?,
    ) : Parcelable

    @Parcelize
    data class AmountWithCurrency(
        val amount: Long,
        val currency: String,
    ) : Parcelable

    enum class CaptureMethod {
        AUTOMATIC,
        MANUAL,
    }

    @Parcelize
    data class PayRequestOrigin(
        val orderId: String,
        val orderReference: String?,
        val name: String?,
    ) : Parcelable

    enum class PayRequestStatus {
        AWAITING_PAYMENT_INPUT,
        AWAITING_AUTHENTICATION,
        PROCESSING,
        SUCCESS,
        FAILED,
        VOIDED,
    }
}
