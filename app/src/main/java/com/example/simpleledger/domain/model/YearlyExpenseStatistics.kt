package com.example.simpleledger.domain.model

import java.lang.Math.addExact
import java.time.LocalDate

data class MonthlyExpense(
    val month: Int,
    val amountMinor: Long,
) {
    init {
        require(month in 1..12) { "month must be between 1 and 12" }
        require(amountMinor >= 0L) { "amountMinor must not be negative" }
    }
}

data class YearlyExpenseStatistics(
    val year: Int,
    val monthlyExpenses: List<MonthlyExpense>,
) {
    init {
        require(monthlyExpenses.map(MonthlyExpense::month) == (1..12).toList()) {
            "monthlyExpenses must contain each month from 1 through 12 in order"
        }
    }

    val totalExpenseMinor: Long = monthlyExpenses.fold(0L) { total, month ->
        addExact(total, month.amountMinor)
    }

    /** The annual total divided across all 12 calendar months, rounded down to a whole minor unit. */
    val averageMonthlyExpenseMinor: Long = totalExpenseMinor / MONTHS_PER_YEAR

    /** The earliest month wins when multiple months share the same highest expense. */
    val highestExpenseMonth: MonthlyExpense? = monthlyExpenses
        .maxOfOrNull(MonthlyExpense::amountMinor)
        ?.takeIf { it > 0L }
        ?.let { highestAmount -> monthlyExpenses.first { it.amountMinor == highestAmount } }

    val isEmpty: Boolean = totalExpenseMinor == 0L

    fun expenseForMonth(month: Int): Long {
        require(month in 1..12) { "month must be between 1 and 12" }
        return monthlyExpenses[month - 1].amountMinor
    }

    companion object {
        private const val MONTHS_PER_YEAR = 12L

        fun from(
            transactions: List<LedgerTransaction>,
            year: Int,
        ): YearlyExpenseStatistics {
            val monthlyTotals = LongArray(MONTHS_PER_YEAR.toInt())

            transactions.forEach { transaction ->
                if (transaction.type != TransactionType.EXPENSE || transaction.amountMinor <= 0L) {
                    return@forEach
                }

                val date = runCatching { LocalDate.parse(transaction.occurredOn) }.getOrNull()
                    ?: return@forEach
                if (date.year != year) return@forEach

                val monthIndex = date.monthValue - 1
                monthlyTotals[monthIndex] = addExact(
                    monthlyTotals[monthIndex],
                    transaction.amountMinor,
                )
            }

            return YearlyExpenseStatistics(
                year = year,
                monthlyExpenses = monthlyTotals.mapIndexed { index, amountMinor ->
                    MonthlyExpense(month = index + 1, amountMinor = amountMinor)
                },
            )
        }
    }
}
