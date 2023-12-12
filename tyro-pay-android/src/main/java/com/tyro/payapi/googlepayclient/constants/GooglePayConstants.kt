package com.tyro.payapi.googlepayclient.constants

internal object GooglePayConstants {
    const val API_VERSION = 2
    const val API_VERSION_MINOR = 0

    // We need to implement 3ds if we use PAN_ONLY
    val ALLOWED_AUTH_METHODS = listOf("CRYPTOGRAM_3DS", "PAN_ONLY")
    val DEFAULT_ALLOWED_CARD_NETWORKS = GooglePayCardNetwork.values().map { it.toString() }
    const val GATEWAY = "verygoodsecurity"

    // This is the VGS ORGANIZATION ID
    const val GATEWAY_MERCHANT_ID = "ACnqw45PLq1aBztBuxDKkZXJ"
    const val COUNTRY_CODE = "AU"
    const val CURRENCY_CODE = "AUD"

    const val PAYMENT_METHOD_DATA = "paymentMethodData"
    const val TOKENIZATION_DATA = "tokenizationData"
    const val TOKEN = "token"
}
