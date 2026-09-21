package com.example.simpleledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParserTest {
    @Test
    fun parsesCommonDecimalInputsExactly() {
        assertEquals(1L, MoneyParser.parse("0.01"))
        assertEquals(1_234L, MoneyParser.parse("12.34"))
        assertEquals(1_250L, MoneyParser.parse("12,5"))
        assertEquals(100_000L, MoneyParser.parse("1000"))
    }

    @Test
    fun rejectsZeroNegativeAndOverPreciseAmounts() {
        assertNull(MoneyParser.parse("0"))
        assertNull(MoneyParser.parse("-1"))
        assertNull(MoneyParser.parse("1.001"))
        assertNull(MoneyParser.parse("1,000.00"))
        assertNull(MoneyParser.parse(""))
    }

    @Test
    fun enforcesMaximumAmount() {
        assertEquals(MoneyParser.MAX_AMOUNT_MINOR, MoneyParser.parse("999999999.99"))
        assertNull(MoneyParser.parse("1000000000.00"))
    }
}
