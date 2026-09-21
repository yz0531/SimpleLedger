package com.example.simpleledger.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType

@Entity(
    tableName = "recurring_rules",
    indices = [
        Index(value = ["isEnabled", "nextExecutionDate"]),
    ],
)
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val categoryId: String,
    val note: String,
    val frequency: String,
    val startDate: String,
    val nextExecutionDate: String,
    val isEnabled: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

internal fun RecurringRuleEntity.toDomain(): RecurringRule = RecurringRule(
    id = id,
    type = checkNotNull(TransactionType.fromWireValue(type)) {
        "数据库中存在未知的周期记账类型：$type"
    },
    amountMinor = amountMinor,
    categoryId = categoryId,
    note = note,
    frequency = checkNotNull(RecurringFrequency.fromWireValue(frequency)) {
        "数据库中存在未知的周期频率：$frequency"
    },
    startDate = startDate,
    nextExecutionDate = nextExecutionDate,
    isEnabled = isEnabled,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun RecurringRule.toEntity(): RecurringRuleEntity = RecurringRuleEntity(
    id = id,
    type = type.wireValue,
    amountMinor = amountMinor,
    categoryId = categoryId,
    note = note,
    frequency = frequency.wireValue,
    startDate = startDate,
    nextExecutionDate = nextExecutionDate,
    isEnabled = isEnabled,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)
