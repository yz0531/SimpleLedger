package com.example.simpleledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YearlyExpenseStatisticsTest {
    @Test
    fun aggregatesOnlyExpensesFromRequestedYearIntoTwelveMonths() {
        val statistics = YearlyExpenseStatistics.from(
            transactions = listOf(
                transaction("jan-a", TransactionType.EXPENSE, 12_500L, "2026-01-03"),
                transaction("jan-b", TransactionType.EXPENSE, 750L, "2026-01-31"),
                transaction("mar", TransactionType.EXPENSE, 30_000L, "2026-03-12"),
                transaction("income", TransactionType.INCOME, 999_999L, "2026-03-12"),
                transaction("other-year", TransactionType.EXPENSE, 88_888L, "2025-03-12"),
                transaction("invalid-date", TransactionType.EXPENSE, 77_777L, "not-a-date"),
            ),
            year = 2026,
        )

        assertEquals(12, statistics.monthlyExpenses.size)
        assertEquals(13_250L, statistics.expenseForMonth(1))
        assertEquals(0L, statistics.expenseForMonth(2))
        assertEquals(30_000L, statistics.expenseForMonth(3))
        assertEquals(43_250L, statistics.totalExpenseMinor)
        assertEquals(3_604L, statistics.averageMonthlyExpenseMinor)
        assertEquals(MonthlyExpense(month = 3, amountMinor = 30_000L), statistics.highestExpenseMonth)
        assertFalse(statistics.isEmpty)
    }

    @Test
    fun reportsEmptyYearWithZeroValuesAndNoHighestMonth() {
        val statistics = YearlyExpenseStatistics.from(
            transactions = listOf(
                transaction("income", TransactionType.INCOME, 20_000L, "2026-02-10"),
                transaction("future-expense", TransactionType.EXPENSE, 50_000L, "2027-02-10"),
            ),
            year = 2026,
        )

        assertTrue(statistics.isEmpty)
        assertEquals(List(12) { 0L }, statistics.monthlyExpenses.map { it.amountMinor })
        assertEquals(0L, statistics.totalExpenseMinor)
        assertEquals(0L, statistics.averageMonthlyExpenseMinor)
        assertNull(statistics.highestExpenseMonth)
    }

    @Test
    fun choosesEarlierMonthWhenHighestExpenseIsTied() {
        val statistics = YearlyExpenseStatistics.from(
            transactions = listOf(
                transaction("april", TransactionType.EXPENSE, 12_000L, "2026-04-30"),
                transaction("october", TransactionType.EXPENSE, 12_000L, "2026-10-01"),
            ),
            year = 2026,
        )

        assertEquals(4, statistics.highestExpenseMonth?.month)
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amountMinor: Long,
        occurredOn: String,
    ) = LedgerTransaction(
        id = id,
        type = type,
        amountMinor = amountMinor,
        categoryId = "test-category",
        occurredOn = occurredOn,
        note = "",
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
    )
}
