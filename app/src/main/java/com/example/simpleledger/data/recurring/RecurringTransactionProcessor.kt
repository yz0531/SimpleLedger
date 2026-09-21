package com.example.simpleledger.data.recurring

import android.content.Context
import androidx.room.withTransaction
import com.example.simpleledger.data.local.LedgerDatabase
import com.example.simpleledger.data.local.toDomain
import com.example.simpleledger.data.local.toEntity
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.RecurringProcessResult
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.recurring.RecurringPostingPlanner
import java.time.LocalDate

internal interface RecurringProcessingSession {
    suspend fun getDueRules(throughDate: LocalDate): List<RecurringRule>

    suspend fun insertTransactionIfAbsent(transaction: LedgerTransaction): Boolean

    suspend fun updateRuleAfterProcessing(
        id: String,
        nextExecutionDate: LocalDate,
        updatedAtEpochMs: Long,
    ): Boolean
}

internal interface RecurringProcessingStore {
    suspend fun <T> inTransaction(block: suspend RecurringProcessingSession.() -> T): T
}

private class RoomRecurringProcessingStore(
    private val database: LedgerDatabase,
) : RecurringProcessingStore {
    private val session = object : RecurringProcessingSession {
        override suspend fun getDueRules(throughDate: LocalDate): List<RecurringRule> =
            database.recurringRuleDao()
                .getDueEnabled(throughDate.toString())
                .map { it.toDomain() }

        override suspend fun insertTransactionIfAbsent(transaction: LedgerTransaction): Boolean =
            database.transactionDao().insertIfAbsent(transaction.toEntity()) != INSERT_CONFLICT

        override suspend fun updateRuleAfterProcessing(
            id: String,
            nextExecutionDate: LocalDate,
            updatedAtEpochMs: Long,
        ): Boolean = database.recurringRuleDao().updateAfterProcessing(
            id = id,
            nextExecutionDate = nextExecutionDate.toString(),
            updatedAtEpochMs = updatedAtEpochMs,
        ) == 1
    }

    override suspend fun <T> inTransaction(
        block: suspend RecurringProcessingSession.() -> T,
    ): T = database.withTransaction { block(session) }

    companion object {
        private const val INSERT_CONFLICT = -1L
    }
}

class RecurringTransactionProcessor internal constructor(
    private val store: RecurringProcessingStore,
    private val todayProvider: () -> LocalDate = LocalDate::now,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun processDue(
        today: LocalDate = todayProvider(),
        includeIncome: Boolean = true,
    ): RecurringProcessResult =
        store.inTransaction {
            val dueRules = getDueRules(today).filter { includeIncome || it.type != com.example.simpleledger.domain.model.TransactionType.INCOME }
            var createdCount = 0
            var duplicateCount = 0

            dueRules.forEach { rule ->
                val processingTime = clock().coerceAtLeast(rule.updatedAtEpochMs)
                val plan = RecurringPostingPlanner.plan(
                    rule = rule,
                    throughDate = today,
                    generatedAtEpochMs = processingTime,
                )
                plan.transactions.forEach { transaction ->
                    if (insertTransactionIfAbsent(transaction)) {
                        createdCount += 1
                    } else {
                        duplicateCount += 1
                    }
                }
                check(
                    updateRuleAfterProcessing(
                        id = rule.id,
                        nextExecutionDate = plan.nextExecutionDate,
                        updatedAtEpochMs = processingTime,
                    ),
                ) { "周期规则在执行期间被删除：${rule.id}" }
            }

            RecurringProcessResult(
                processedRuleCount = dueRules.size,
                createdTransactionCount = createdCount,
                skippedDuplicateCount = duplicateCount,
            )
        }

    companion object {
        fun create(context: Context): RecurringTransactionProcessor =
            create(LedgerDatabase.getInstance(context.applicationContext))

        fun create(database: LedgerDatabase): RecurringTransactionProcessor =
            RecurringTransactionProcessor(RoomRecurringProcessingStore(database))
    }
}
