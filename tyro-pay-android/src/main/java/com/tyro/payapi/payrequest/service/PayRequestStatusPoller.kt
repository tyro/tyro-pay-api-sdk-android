package com.tyro.payapi.payrequest.service

import com.tyro.payapi.payrequest.model.PayRequestResponse

internal class PayRequestStatusPoller(
    private val payRequestPoller: PayRequestPoller,
) {
    suspend fun pollForInitialStatusUpdate(
        paySecret: String,
        pollIntervalMillis: Long = POLL_INTERVAL_MS,
    ): PayRequestResponse? {
        return payRequestPoller.pollForResult(
            paySecret,
            { statusResult -> INITIAL_UPDATED_STATUSES.contains(statusResult.status) },
            pollIntervalMillis,
            PAY_REQUEST_POLL_MAX_RETRIES,
        )
    }

    companion object {
        const val POLL_INTERVAL_MS = 500L
        const val PAY_REQUEST_POLL_MAX_RETRIES = 60L
        val INITIAL_UPDATED_STATUSES = listOf(
            PayRequestResponse.PayRequestStatus.SUCCESS,
            PayRequestResponse.PayRequestStatus.FAILED,
            PayRequestResponse.PayRequestStatus.AWAITING_AUTHENTICATION,
        )
    }
}
