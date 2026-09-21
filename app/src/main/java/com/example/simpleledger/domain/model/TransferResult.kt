package com.example.simpleledger.domain.model

data class ImportResult(
    val importedCount: Int,
    val insertedCount: Int,
    val updatedCount: Int,
    val recurringImportedCount: Int = 0,
    val recurringInsertedCount: Int = 0,
    val recurringUpdatedCount: Int = 0,
)

data class ExportResult(
    val exportedCount: Int,
    val bytesWritten: Long,
    val recurringRuleCount: Int = 0,
)
