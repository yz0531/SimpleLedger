package com.example.simpleledger.data.repository

import androidx.room.withTransaction
import com.example.simpleledger.data.local.LedgerDatabase
import com.example.simpleledger.data.local.toDomain
import com.example.simpleledger.data.local.toEntity
import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.recurring.RecurringPostingPlanner
import com.example.simpleledger.domain.recurring.RecurringRuleState
import com.example.simpleledger.domain.repository.RecurringRuleRepository
import com.example.simpleledger.util.DateValidator
import com.example.simpleledger.util.MoneyParser
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineRecurringRuleRepository(
    private val database: LedgerDatabase,
    private val todayProvider: () -> LocalDate = LocalDate::now,
) : RecurringRuleRepository {
    private val dao = database.recurringRuleDao()

    override fun observeAll(): Flow<List<RecurringRule>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAllSnapshot(): List<RecurringRule> =
        dao.getAllSnapshot().map { it.toDomain() }

    override suspend fun getById(id: String): RecurringRule? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(rule: RecurringRule) {
        validate(rule)
        dao.upsert(rule.toEntity())
    }

    override suspend fun delete(id: String): Boolean = dao.deleteById(id) > 0

    override suspend fun disableIncomeRules(updatedAtEpochMs: Long): Int {
        require(updatedAtEpochMs >= 0L) { "更新时间无效" }
        return dao.disableIncomeRules(updatedAtEpochMs)
    }

    override suspend fun setEnabled(
        id: String,
        isEnabled: Boolean,
        updatedAtEpochMs: Long,
    ): Boolean = database.withTransaction {
        val existing = dao.getById(id) ?: return@withTransaction false
        val updated = RecurringRuleState.withEnabledState(
            rule = existing.toDomain(),
            isEnabled = isEnabled,
            effectiveDate = todayProvider(),
            requestedUpdatedAtEpochMs = updatedAtEpochMs,
        )
        dao.setEnabled(
            id = id,
            isEnabled = isEnabled,
            nextExecutionDate = updated.nextExecutionDate,
            updatedAtEpochMs = updated.updatedAtEpochMs,
        ) > 0
    }

    private fun validate(rule: RecurringRule) {
        require(
            rule.id.isNotBlank() &&
                rule.id.length <= MAX_RULE_ID_LENGTH &&
                rule.id.none { it.isISOControl() },
        ) { "周期规则 ID 无效" }
        require(rule.amountMinor in 1..MoneyParser.MAX_AMOUNT_MINOR) { "周期金额无效" }
        val category = requireNotNull(Categories.find(rule.categoryId)) { "周期规则包含未知分类" }
        require(category.type == rule.type) { "周期规则的分类与收支类型不匹配" }
        require(rule.note.length <= MAX_NOTE_LENGTH) { "周期备注不能超过 $MAX_NOTE_LENGTH 个字符" }
        require(DateValidator.isValidIsoLocalDate(rule.startDate)) { "周期开始日期无效" }
        require(DateValidator.isValidIsoLocalDate(rule.nextExecutionDate)) { "下次执行日期无效" }
        require(!LocalDate.parse(rule.nextExecutionDate).isBefore(LocalDate.parse(rule.startDate))) {
            "下次执行日期不能早于开始日期"
        }
        require(
            RecurringPostingPlanner.isOccurrenceDate(
                startDate = LocalDate.parse(rule.startDate),
                candidateDate = LocalDate.parse(rule.nextExecutionDate),
                frequency = rule.frequency,
            ),
        ) { "下次执行日期不在所选周期的执行序列上" }
        require(rule.createdAtEpochMs >= 0L) { "创建时间无效" }
        require(rule.updatedAtEpochMs >= rule.createdAtEpochMs) { "更新时间不能早于创建时间" }
    }

    companion object {
        const val MAX_NOTE_LENGTH = 500
        const val MAX_RULE_ID_LENGTH = 80
    }
}
