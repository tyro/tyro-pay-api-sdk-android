package com.tyro.payapi.googlepayclient.googleplay

import android.app.Activity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentsClient
import com.tyro.payapi.googlepayclient.googlepay.DefaultGooglePayRepository
import com.tyro.payapi.googlepayclient.googlepay.GooglePayRequestFactory
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.fail
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.lang.Exception
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
class DefaultGooglePayRepositoryTest {
    private lateinit var defaultGooglePayRepository: DefaultGooglePayRepository
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var googlePayRequestFactory: GooglePayRequestFactory

    @Before
    fun setUp() {
        paymentsClient = Mockito.mock()
        googlePayRequestFactory = GooglePayRequestFactory()
        defaultGooglePayRepository = DefaultGooglePayRepository(paymentsClient, googlePayRequestFactory)
    }

    @Test
    fun `isGooglePayReady() should return true when result is true`() = runBlocking {
        val fakeTestTask = FakeTestTask(true)
        `when`(paymentsClient.isReadyToPay(any())).thenReturn(fakeTestTask)
        val flow = defaultGooglePayRepository.isGooglePayReady(null)
        assert(flow.first())
    }

    @Test
    fun `isGooglePayReady() should return false when result is false`() = runBlocking {
        val fakeTestTask = FakeTestTask(false)
        `when`(paymentsClient.isReadyToPay(any())).thenReturn(fakeTestTask)
        val flow = defaultGooglePayRepository.isGooglePayReady(null)
        assertFalse(flow.first())
    }

    @Test
    fun `isGooglePayReady() should return false when exception is thrown`() = runBlocking {
        val fakeTestTask = FakeTestTask(isSuccess = true, shouldThrow = true)
        `when`(paymentsClient.isReadyToPay(any())).thenReturn(fakeTestTask)
        val flow = defaultGooglePayRepository.isGooglePayReady(null)
        assertFalse(flow.first())
    }

    @Test(expected = TimeoutCancellationException::class)
    fun `isGooglePayReady() should return nothing when no result`() = runBlocking {
        val testTask: Task<Boolean> = Mockito.mock()
        `when`(paymentsClient.isReadyToPay(any())).thenReturn(testTask)
        withTimeout(100) {
            defaultGooglePayRepository.isGooglePayReady(null).first()
            fail()
        }
    }
}

private class FakeTestTask(private val isSuccess: Boolean, private val shouldThrow: Boolean = false) : Task<Boolean>() {
    override fun <X : Throwable?> getResult(p0: Class<X>): Boolean {
        if (shouldThrow) throw ApiException(Status.RESULT_TIMEOUT)
        return isSuccess
    }

    override fun addOnCompleteListener(p0: OnCompleteListener<Boolean>): Task<Boolean> {
        p0.onComplete(this)
        return FakeTestTask(isSuccess, shouldThrow)
    }

    override fun addOnFailureListener(p0: OnFailureListener): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun getException(): Exception? {
        throw UnsupportedOperationException()
    }

    override fun getResult(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isCanceled(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isComplete(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addOnSuccessListener(
        p0: Executor,
        p1: OnSuccessListener<in Boolean>,
    ): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun addOnSuccessListener(
        p0: Activity,
        p1: OnSuccessListener<in Boolean>,
    ): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun addOnSuccessListener(p0: OnSuccessListener<in Boolean>): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun addOnCompleteListener(
        p0: Activity,
        p1: OnCompleteListener<Boolean>,
    ): Task<Boolean> {
        throw UnsupportedOperationException()
    }

    override fun isSuccessful(): Boolean {
        throw UnsupportedOperationException()
    }
}
