package com.example.simpleledger.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredOn"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val categoryId: String,
    val occurredOn: String,
    val note: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

internal fun TransactionEntity.toDomain(): LedgerTransaction = LedgerTransaction(
    id = id,
    type = checkNotNull(TransactionType.fromWireValue(type)) {
        "数据库中存在未知的收支类型：$type"
    },
    amountMinor = amountMinor,
    categoryId = categoryId,
    occurredOn = occurredOn,
    note = note,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun LedgerTransaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type.wireValue,
    amountMinor = amountMinor,
    categoryId = categoryId,
    occurredOn = occurredOn,
    note = note,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)
