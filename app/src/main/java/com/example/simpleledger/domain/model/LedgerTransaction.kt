package com.example.simpleledger.domain.model

data class LedgerTransaction(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val categoryId: String,
    val occurredOn: String,
    val note: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
