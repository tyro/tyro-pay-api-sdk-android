package com.tyro.payapi.googlepayclient.googlepay

import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants

class PaymentsClientFactory {
    fun createPaymentsClient(isLive: Boolean, context: Context): PaymentsClient {
        val options = Wallet.WalletOptions.Builder()
            .setEnvironment(
                if (isLive) {
                    WalletConstants.ENVIRONMENT_PRODUCTION
                } else WalletConstants.ENVIRONMENT_TEST,
            )
            .build()
        return Wallet.getPaymentsClient(context, options)
    }
}
