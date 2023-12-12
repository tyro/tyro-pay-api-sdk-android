package com.tyro.payapi.payrequest.service

import com.tyro.payapi.payrequest.model.PayRequestResponse
import com.tyro.payapi.payrequest.repository.PayRequestRepository
import kotlinx.coroutines.delay
import timber.log.Timber

internal class PayRequestPoller(
    private val payRequestRepository: PayRequestRepository,
) {

    suspend fun pollForResult(
        paySecret: String,
        conditionFn: (statusResult: PayRequestResponse) -> Boolean,
        pollIntervalMillis: Long,
        pollMaxRetries: Long,
    ): PayRequestResponse? {
        var attemptNumber = 0
        var statusResult: PayRequestResponse? = null
        while (attemptNumber < pollMaxRetries) {
            Timber.d("polling attempt $attemptNumber")
            statusResult = payRequestRepository.fetchPayRequest(paySecret)
            if (statusResult == null) {
                break
            }
            Timber.d("statusResult $statusResult")
            if (!conditionFn(statusResult)) {
                delay(pollIntervalMillis)
                attemptNumber++
            } else {
                break
            }
        }
        return statusResult
    }
}
