package com.example.simpleledger.domain.repository

import com.example.simpleledger.domain.model.ImportResult
import com.example.simpleledger.domain.model.LedgerSummary
import com.example.simpleledger.domain.model.LedgerTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface LedgerRepository {
    fun observeAll(): Flow<List<LedgerTransaction>>

    fun observeSummary(): Flow<LedgerSummary> = observeAll().map { LedgerSummary.from(it) }

    suspend fun getById(id: String): LedgerTransaction?

    suspend fun upsert(transaction: LedgerTransaction)

    suspend fun delete(id: String)

    suspend fun delete(transaction: LedgerTransaction) = delete(transaction.id)

    suspend fun getAllSnapshot(): List<LedgerTransaction>

    suspend fun importTransactions(transactions: List<LedgerTransaction>): ImportResult
}
