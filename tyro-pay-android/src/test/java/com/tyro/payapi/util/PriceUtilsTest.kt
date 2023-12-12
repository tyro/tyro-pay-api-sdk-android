package com.tyro.payapi.util

import com.tyro.payapi.util.PriceUtils.centsToDecimalString
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PriceUtilsTest {
    @Test
    fun `centsToDecimalString() should convert correctly`() {
        assertEquals("1.00", 100L.centsToDecimalString())
        assertEquals("22.50", 2250L.centsToDecimalString())
        assertEquals("9.59", 959L.centsToDecimalString())
        assertEquals("0.01", 1L.centsToDecimalString())
        assertEquals("0.69", 69L.centsToDecimalString())
        assertEquals("9999999999.99", 999999999999L.centsToDecimalString())
    }
}
