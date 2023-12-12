package com.tyro.payapi.googlepayclient.googlepay

import com.tyro.payapi.googlepayclient.constants.GooglePayCardNetwork
import com.tyro.payapi.googlepayclient.constants.GooglePayConstants
import com.tyro.payapi.util.PriceUtils.centsToDecimalString
import org.json.JSONArray
import org.json.JSONObject

internal class GooglePayRequestFactory {

    private fun createAllowedPaymentMethods(allowedCardNetworks: List<GooglePayCardNetwork>?): JSONObject =
        JSONObject().apply {
            put("type", "CARD")
            put(
                "parameters",
                JSONObject().apply {
                    put("allowedAuthMethods", JSONArray(GooglePayConstants.ALLOWED_AUTH_METHODS))
                    put(
                        "allowedCardNetworks",
                        JSONArray(
                            allowedCardNetworks?.map { it.toString() }
                                ?: GooglePayConstants.DEFAULT_ALLOWED_CARD_NETWORKS,
                        ),
                    )
                },
            )
        }

    fun createIsGooglePayReadyRequest(allowedCardNetworks: List<GooglePayCardNetwork>?): JSONObject =
        JSONObject().apply {
            put("apiVersion", GooglePayConstants.API_VERSION)
            put("apiVersionMinor", GooglePayConstants.API_VERSION_MINOR)
            put("allowedPaymentMethods", JSONArray().put(createAllowedPaymentMethods(allowedCardNetworks)))
        }

    fun createGooglePayPaymentDataRequest(
        totalPriceCents: Long,
        merchantName: String,
        allowedCardNetworks: List<GooglePayCardNetwork>?,
    ): JSONObject {
        return JSONObject().apply {
            put("apiVersion", GooglePayConstants.API_VERSION)
            put("apiVersionMinor", GooglePayConstants.API_VERSION_MINOR)
            put(
                "allowedPaymentMethods",
                JSONArray().put(
                    createAllowedPaymentMethods(allowedCardNetworks).apply {
                        put(
                            "tokenizationSpecification",
                            JSONObject().apply {
                                put("type", "PAYMENT_GATEWAY")
                                put(
                                    "parameters",
                                    JSONObject(
                                        mapOf(
                                            "gateway" to GooglePayConstants.GATEWAY,
                                            "gatewayMerchantId" to GooglePayConstants.GATEWAY_MERCHANT_ID,
                                        ),
                                    ),
                                )
                            },
                        )
                    },
                ),
            )
            put(
                "transactionInfo",
                JSONObject().apply {
                    put("totalPrice", totalPriceCents.centsToDecimalString())
                    put("totalPriceStatus", "FINAL")
                    put("countryCode", GooglePayConstants.COUNTRY_CODE)
                    put("currencyCode", GooglePayConstants.CURRENCY_CODE)
                },
            )
            put("merchantInfo", JSONObject().put("merchantName", merchantName))
            put("shippingAddressRequired", false)
        }
    }
}
