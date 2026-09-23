package com.example.simpleledger.data.transfer

import androidx.room.withTransaction
import com.example.simpleledger.data.local.LedgerDatabase
import com.example.simpleledger.data.local.toDomain
import com.example.simpleledger.data.local.toEntity
import com.example.simpleledger.domain.model.ClearDataResult
import com.example.simpleledger.domain.model.ImportResult

class LedgerBackupStore(
    private val database: LedgerDatabase,
) {
    suspend fun snapshot(): LedgerBackup = database.withTransaction {
        LedgerBackup(
            transactions = database.transactionDao().getAllSnapshot().map { it.toDomain() },
            recurringRules = database.recurringRuleDao().getAllSnapshot().map { it.toDomain() },
        )
    }

    suspend fun import(backup: LedgerBackup): ImportResult = database.withTransaction {
        val transactionIds = database.transactionDao().getAllIds().toHashSet()
        val recurringIds = database.recurringRuleDao().getAllIds().toHashSet()
        val updatedTransactions = backup.transactions.count { it.id in transactionIds }
        val updatedRecurring = backup.recurringRules.count { it.id in recurringIds }

        database.transactionDao().upsertAll(backup.transactions.map { it.toEntity() })
        database.recurringRuleDao().upsertAll(backup.recurringRules.map { it.toEntity() })

        ImportResult(
            insertedCount = backup.transactions.size - updatedTransactions,
            updatedCount = updatedTransactions,
            recurringInsertedCount = backup.recurringRules.size - updatedRecurring,
            recurringUpdatedCount = updatedRecurring,
        )
    }

    suspend fun clearAll(): ClearDataResult = database.withTransaction {
        val deletedRecurringRules = database.recurringRuleDao().deleteAll()
        val deletedTransactions = database.transactionDao().deleteAll()
        ClearDataResult(
            deletedTransactionCount = deletedTransactions,
            deletedRecurringRuleCount = deletedRecurringRules,
        )
    }
}
