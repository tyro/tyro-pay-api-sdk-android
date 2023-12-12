package com.tyro.payapi.util

import java.math.BigDecimal
import java.math.RoundingMode

object PriceUtils {

    private val CENTS = BigDecimal(100)

    /**
     * Converts cents to a decimal string
     */
    fun Long.centsToDecimalString() = BigDecimal(this)
        .divide(CENTS)
        .setScale(2, RoundingMode.HALF_EVEN)
        .toString()
}
