package com.example.simpleledger.domain.model

data class LedgerSummary(
    val incomeMinor: Long,
    val expenseMinor: Long,
) {
    val balanceMinor: Long get() = incomeMinor - expenseMinor

    companion object {
        val Empty = LedgerSummary(incomeMinor = 0L, expenseMinor = 0L)

        fun from(transactions: Iterable<LedgerTransaction>): LedgerSummary {
            var income = 0L
            var expense = 0L
            transactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> income += transaction.amountMinor
                    TransactionType.EXPENSE -> expense += transaction.amountMinor
                }
            }
            return LedgerSummary(incomeMinor = income, expenseMinor = expense)
        }
    }
}
