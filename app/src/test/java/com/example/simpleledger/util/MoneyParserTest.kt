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

    @Test
    fun evaluatesAllFourOperationsWithNormalPrecedence() {
        assertEquals(1_750L, MoneyParser.parseExpression("12.5+5"))
        assertEquals(8_000L, MoneyParser.parseExpression("100-20"))
        assertEquals(600L, MoneyParser.parseExpression("12-2×3"))
        assertEquals(3_750L, MoneyParser.parseExpression("12.5*3"))
        assertEquals(250L, MoneyParser.parseExpression("10/4"))
        assertEquals(250L, MoneyParser.parseExpression("10÷4"))
        assertEquals(1_400L, MoneyParser.parseExpression("10+8/2"))
    }

    @Test
    fun roundsMultiplicationResultsToTheNearestCent() {
        assertEquals(151L, MoneyParser.parseExpression("1.23×1.23"))
        assertEquals(1L, MoneyParser.parseExpression("0.1×0.1"))
        assertEquals(33L, MoneyParser.parseExpression("1÷3"))
    }

    @Test
    fun rejectsIncompleteOrNonPositiveExpressions() {
        assertNull(MoneyParser.parseExpression("12+"))
        assertNull(MoneyParser.parseExpression("12**2"))
        assertNull(MoneyParser.parseExpression("12/0"))
        assertNull(MoneyParser.parseExpression("10-10"))
        assertNull(MoneyParser.parseExpression("1-2"))
        assertNull(MoneyParser.parseExpression(""))
    }
}
