package com.example.simpleledger.domain.model

const val MAX_RECURRING_RULE_ID_LENGTH = 80

data class RecurringRule(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val categoryId: String,
    val note: String,
    val frequency: RecurringFrequency,
    val startDate: String,
    val nextExecutionDate: String,
    val isEnabled: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class RecurringProcessResult(
    val processedRuleCount: Int,
    val createdTransactionCount: Int,
    val skippedDuplicateCount: Int,
)
