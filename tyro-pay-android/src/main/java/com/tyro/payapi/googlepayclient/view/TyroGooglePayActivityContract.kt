package com.tyro.payapi.googlepayclient.view

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.tyro.payapi.googlepayclient.TyroGooglePayClient
import com.tyro.payapi.payrequest.constants.ErrorCode
import com.tyro.payapi.payrequest.constants.TyroPayRequestErrorType
import com.tyro.payapi.payrequest.model.TyroPayRequestError
import kotlinx.parcelize.Parcelize

class TyroGooglePayActivityContract : ActivityResultContract<TyroGooglePayActivityContract.Params, TyroGooglePayClient.Result>() {

    override fun createIntent(context: Context, input: Params) = Intent(context, TyroGooglePayActivity::class.java).putExtras(input.toBundle())

    override fun parseResult(resultCode: Int, intent: Intent?): TyroGooglePayClient.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: TyroGooglePayClient.Result.Failed(
            TyroPayRequestError(
                "Process ended unexpectedly, fetch Pay Request Status for result.",
                TyroPayRequestErrorType.UNKNOWN_ERROR,
                ErrorCode.PROCESS_ENDED_UNEXPECTEDLY_FETCH_PAY_REQUEST_STATUS.toString(),
            ),
        )
    }

    @Parcelize
    data class PayRequestParams(
        override val paySecret: String,
        override val config: TyroGooglePayClient.Config,
    ) : Params()

    sealed class Params : Parcelable {
        internal abstract val paySecret: String
        internal abstract val config: TyroGooglePayClient.Config

        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            internal fun fromIntent(intent: Intent): Params? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_RESULT = "extra_result"
    }
}
