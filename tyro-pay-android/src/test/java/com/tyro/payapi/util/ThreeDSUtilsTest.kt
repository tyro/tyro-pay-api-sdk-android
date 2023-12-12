package com.tyro.payapi.util

import com.tyro.payapi.util.ThreeDSUtils.getWebViewUrl
import com.tyro.payapi.util.ThreeDSUtils.isWebViewFinished
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ThreeDSUtilsTest {

    @Test
    fun `isWebViewFinished() should respond correctly`() {
        assertEquals(false, isWebViewFinished(null))
        assertEquals(false, isWebViewFinished("https://test.com?paysecret=pssst"))
        assertEquals(false, isWebViewFinished("https://test.com?paysecret=pssst#foo=bar"))
        assertEquals(true, isWebViewFinished("https://test.com?paysecret=pssst#result=done"))
    }

    @Test
    fun `getWebViewUrl() should return the correct url`() {
        assertEquals("https://pay.3ds.connect.tyro.com/?paysecret=my-pay-secret", getWebViewUrl("my-pay-secret"))
        assertEquals("https://pay.3ds.connect.tyro.com/?paysecret=my+pay+secret", getWebViewUrl("my pay secret"))
    }
}
