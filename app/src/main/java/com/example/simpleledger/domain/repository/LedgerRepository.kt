package com.example.simpleledger.domain.repository

import com.example.simpleledger.domain.model.ImportResult
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.YearlyExpenseStatistics
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun observeDateRange(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<LedgerTransaction>>

    fun observeYearlyExpenseStatistics(year: Int): Flow<YearlyExpenseStatistics>

    suspend fun getById(id: String): LedgerTransaction?

    suspend fun upsert(transaction: LedgerTransaction)

    suspend fun delete(id: String)

    suspend fun delete(transaction: LedgerTransaction) = delete(transaction.id)

    suspend fun getAllSnapshot(): List<LedgerTransaction>

    suspend fun importTransactions(transactions: List<LedgerTransaction>): ImportResult
}
