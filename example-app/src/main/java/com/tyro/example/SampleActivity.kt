package com.tyro.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import android.view.Menu
import android.view.MenuItem
import com.tyro.payapi.googlepayclient.TyroGooglePayClient
import com.tyro.payapi.googlepayclient.constants.GooglePayCardNetwork
import com.tyro.example.databinding.ActivitySampleBinding
import java.util.logging.Logger

class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val tyroGooglePayClient = TyroGooglePayClient(
            activity = this,
            config = TyroGooglePayClient.Config(
                liveMode = false,
                merchantName = "Example Merchant",
                allowedCardNetworks = listOf(
                    GooglePayCardNetwork.VISA,
                    GooglePayCardNetwork.MASTERCARD,
                    GooglePayCardNetwork.AMEX,
                    GooglePayCardNetwork.JCB
                )
            ),
            googlePayReadyListener = :: onGooglePayReady,
            paymentResultListener = :: onPaymentResult,
        )

        // generate your paySecret before launching google pay
        val paySecret = "<pay-secret>"

        binding.fab.setOnClickListener {
            tyroGooglePayClient.launchGooglePay(paySecret)
        }
    }

    private fun onGooglePayReady(googlePayAvailable: Boolean){
        Logger.getGlobal().info("android app received tyro sdk googlePayAvailable = ${googlePayAvailable}")
    }

    private fun onPaymentResult(result: TyroGooglePayClient.Result){
        Logger.getGlobal().info("android app received tyro sdk result")

        when(result){
            TyroGooglePayClient.Result.Cancelled -> {
                // User cancelled operation
                Logger.getGlobal().info("tyro google pay cancelled")
            }
            TyroGooglePayClient.Result.Success -> {
                // Google Pay Success, show success view
                Logger.getGlobal().info("tyro google pay success")
            }
            is TyroGooglePayClient.Result.Failed -> {
                val (errorMessage, errorType, errorCode, gatewayCode) = result.error
                // Transaction failed for some reason
                // inspect the following fields to get more info
                Logger.getGlobal().severe("tyro google pay failed")
                Logger.getGlobal().severe("result.error.errorType: $errorType")
                Logger.getGlobal().severe("result.error.errorMessage: $errorMessage")
                Logger.getGlobal().severe("result.error.errorCode: $errorCode")
                Logger.getGlobal().severe("result.error.gatewayCode: $gatewayCode")
            }
        }
    }
}