package com.example.simpleledger.domain.model

data class ImportResult(
    val insertedCount: Int,
    val updatedCount: Int,
    val recurringInsertedCount: Int = 0,
    val recurringUpdatedCount: Int = 0,
)

data class ExportResult(
    val exportedCount: Int,
    val recurringRuleCount: Int = 0,
)

data class ClearDataResult(
    val deletedTransactionCount: Int,
    val deletedRecurringRuleCount: Int,
)
