package com.example.simpleledger.domain.repository

import com.example.simpleledger.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

interface RecurringRuleRepository {
    fun observeAll(): Flow<List<RecurringRule>>

    suspend fun getAllSnapshot(): List<RecurringRule>

    suspend fun getById(id: String): RecurringRule?

    suspend fun upsert(rule: RecurringRule)

    suspend fun delete(id: String): Boolean

    suspend fun disableIncomeRules(
        updatedAtEpochMs: Long = System.currentTimeMillis(),
    ): Int

    suspend fun setEnabled(
        id: String,
        isEnabled: Boolean,
        updatedAtEpochMs: Long = System.currentTimeMillis(),
    ): Boolean
}
