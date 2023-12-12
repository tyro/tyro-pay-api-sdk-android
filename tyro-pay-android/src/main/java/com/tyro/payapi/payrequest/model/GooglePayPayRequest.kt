package com.tyro.payapi.payrequest.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GooglePayPayRequest @JvmOverloads constructor(
    val google_pay_payload: GooglePayPayload,
    val paymentType: String = "GOOGLE_PAY",
) : Parcelable {

    @Parcelize
    data class GooglePayPayload(
        val token: Token,
    ) : Parcelable

    @Parcelize
    data class Token(
        val signature: String,
        val intermediateSigningKey: SigningKey,
        val protocolVersion: String,
        val signedMessage: String,
    ) : Parcelable

    @Parcelize
    data class SigningKey(
        val signedKey: String,
        val signatures: List<String>,
    ) : Parcelable
}
