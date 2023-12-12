package com.tyro.payapi.payrequest.model

import android.os.Parcelable
import com.tyro.payapi.payrequest.constants.TyroPayRequestErrorType
import kotlinx.parcelize.Parcelize

@Parcelize
data class TyroPayRequestError @JvmOverloads constructor(
    val errorMessage: String = "Something went wrong",
    val errorType: TyroPayRequestErrorType = TyroPayRequestErrorType.UNKNOWN_ERROR,
    val errorCode: String? = null,
    val gatewayCode: String? = null,
) : Parcelable
