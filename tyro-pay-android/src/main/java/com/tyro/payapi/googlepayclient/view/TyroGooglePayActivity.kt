package com.tyro.payapi.googlepayclient.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.tyro.payapi.R
import com.tyro.payapi.databinding.TyroGooglePayActivityBinding
import com.tyro.payapi.googlepayclient.TyroGooglePayClient
import com.tyro.payapi.googlepayclient.viewmodel.PaymentHandlingResult
import com.tyro.payapi.googlepayclient.viewmodel.TyroGooglePayViewModel
import com.tyro.payapi.payrequest.model.TyroPayRequestError
import com.tyro.payapi.util.ThreeDSUtils
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.logging.Logger
internal class TyroGooglePayActivity : AppCompatActivity() {
    private val viewModel: TyroGooglePayViewModel by viewModels {
        TyroGooglePayViewModel.Factory(params)
    }
    private lateinit var params: TyroGooglePayActivityContract.Params
    private lateinit var binding: TyroGooglePayActivityBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFadeAnimations()

        binding = TyroGooglePayActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.d("onCreate()")
        Timber.d(TyroGooglePayActivityContract.Params.fromIntent(intent).toString())

        params = runCatching {
            requireNotNull(TyroGooglePayActivityContract.Params.fromIntent(intent)) {
                "Activity was started without arguments"
            }
        }.getOrElse {
            finishWithResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(it.message!!),
                ),
            )
            return
        }

        viewModel.result.observe(this) {
                result ->
            result?.let(::finishWithResult)
        }

        binding.webviewCloseButton.setOnClickListener {
            val builder: AlertDialog.Builder? = let {
                AlertDialog.Builder(it)
            }.apply {
                setPositiveButton(
                    R.string.webview_close_dialog_positive_button,
                ) { _, _ ->
                    viewModel.handle3dsWebviewClose()
                }
                setNegativeButton(
                    R.string.webview_close_dialog_negative_button,
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                setMessage(R.string.webview_close_dialog_message)
                setTitle(R.string.webview_close_dialog_title)
            }
            val dialog: AlertDialog? = builder?.create()
            dialog?.show()
        }

        val webView = binding.webview
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                if (ThreeDSUtils.isWebViewFinished(url)) {
                    finish3dsFlow()
                }
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                // show the webview once it has loaded the url
                binding.webview.visibility = View.VISIBLE
                binding.webviewCloseButton.visibility = View.VISIBLE
                super.onPageCommitVisible(view, url)
            }
        }

        if (!viewModel.hasStarted) {
            Timber.d("loadingGooglePayPaymentData()")
            lifecycleScope.launch {
                kotlin.runCatching {
                    viewModel.loadGooglePayPaymentData()
                }.fold(
                    onSuccess = {
                        startGooglePayForResult(it)
                        viewModel.hasStarted = true
                    },
                    onFailure = {
                        Timber.e(it)
                        viewModel.updateResult(
                            TyroGooglePayClient.Result.Failed(
                                TyroPayRequestError(it.message!!),
                            ),
                        )
                    },
                )
            }
        } else if (viewModel.pollingStarted) {
            Timber.d("resuming polling")
            lifecycleScope.launch {
                kotlin.runCatching {
                    val res = viewModel.handlePayCompletionFlow(params.paySecret)
                    if (res == PaymentHandlingResult.RUN_3DS) {
                        start3dsFlow()
                    }
                }.onFailure {
                    Timber.e(it)
                    viewModel.updateResult(
                        TyroGooglePayClient.Result.Failed(
                            TyroPayRequestError(it.message!!),
                        ),
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        // disable back button so user cant cancel the process during
        // submission and polling
    }

    override fun finish() {
        super.finish()
        setFadeAnimations()
    }

    private fun startGooglePayForResult(task: Task<PaymentData>) {
        AutoResolveHelper.resolveTask(
            task,
            this,
            GOOGLE_PAY_REQUEST_CODE,
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_PAY_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    handleGooglePayResult(data)
                }
                RESULT_CANCELED -> {
                    Logger.getGlobal().warning("cancelled")
                    viewModel.updateResult(
                        TyroGooglePayClient.Result.Cancelled,
                    )
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    Logger.getGlobal().warning("result error")
                    viewModel.updateResult(
                        TyroGooglePayClient.Result.Failed(
                            TyroPayRequestError(),
                        ),
                    )
                }
                else -> {
                    Logger.getGlobal().warning("else")
                    viewModel.updateResult(
                        TyroGooglePayClient.Result.Failed(
                            TyroPayRequestError(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleGooglePayResult(data: Intent?) {
        val paymentData = data?.let { PaymentData.getFromIntent(it) }
        if (paymentData == null) {
            viewModel.updateResult(
                TyroGooglePayClient.Result.Failed(
                    TyroPayRequestError(),
                ),
            )
            return
        }
        binding.spinner.visibility = View.VISIBLE
        lifecycleScope.launch {
            val res = viewModel.handleGooglePayResult(paymentData)
            if (res == PaymentHandlingResult.RUN_3DS) {
                start3dsFlow()
            }
        }
    }

    private fun start3dsFlow() {
        Timber.d("Starting 3ds flow")
        binding.webview.loadUrl(ThreeDSUtils.getWebViewUrl(params.paySecret))
    }

    private fun finish3dsFlow() {
        Timber.d("Finishing 3ds flow")
        lifecycleScope.launch {
            kotlin.runCatching {
                binding.webview.visibility = View.GONE
                viewModel.handle3DSCompletionFlow(params.paySecret)
            }.onFailure {
                Timber.e(it)
                viewModel.updateResult(
                    TyroGooglePayClient.Result.Failed(
                        TyroPayRequestError(it.message!!),
                    ),
                )
            }
        }
    }

    private fun finishWithResult(result: TyroGooglePayClient.Result) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(TyroGooglePayActivityContract.EXTRA_RESULT to result),
                ),
        )
        finish()
    }

    private fun setFadeAnimations() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private companion object {
        // random code
        private const val GOOGLE_PAY_REQUEST_CODE = 696969
    }
}
