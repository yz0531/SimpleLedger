package com.example.simpleledger.domain.recurring

import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.min

data class RecurringPostingPlan(
    val transactions: List<LedgerTransaction>,
    val nextExecutionDate: LocalDate,
)

object RecurringPostingPlanner {
    fun plan(
        rule: RecurringRule,
        throughDate: LocalDate,
        generatedAtEpochMs: Long,
    ): RecurringPostingPlan {
        val startDate = LocalDate.parse(rule.startDate)
        var occurrenceDate = LocalDate.parse(rule.nextExecutionDate)
        require(!occurrenceDate.isBefore(startDate)) {
            "下次执行日期不能早于开始日期"
        }
        require(isOccurrenceDate(startDate, occurrenceDate, rule.frequency)) {
            "下次执行日期不在周期规则的执行序列上"
        }

        if (!rule.isEnabled || occurrenceDate.isAfter(throughDate)) {
            return RecurringPostingPlan(emptyList(), occurrenceDate)
        }

        val createdAtEpochMs = generatedAtEpochMs.coerceAtLeast(rule.createdAtEpochMs)
        val transactions = mutableListOf<LedgerTransaction>()
        while (!occurrenceDate.isAfter(throughDate)) {
            if (transactions.size >= MAX_CATCH_UP_OCCURRENCES) {
                throw RecurringCatchUpLimitExceededException(
                    "单条周期规则一次最多补记 $MAX_CATCH_UP_OCCURRENCES 笔，请调整开始日期",
                )
            }
            transactions += LedgerTransaction(
                id = generatedTransactionId(rule.id, occurrenceDate),
                type = rule.type,
                amountMinor = rule.amountMinor,
                categoryId = rule.categoryId,
                occurredOn = occurrenceDate.toString(),
                note = rule.note,
                createdAtEpochMs = createdAtEpochMs,
                updatedAtEpochMs = createdAtEpochMs,
            )
            occurrenceDate = nextOccurrence(
                currentOccurrence = occurrenceDate,
                frequency = rule.frequency,
                anchorStartDate = startDate,
            )
        }
        return RecurringPostingPlan(transactions, occurrenceDate)
    }

    fun nextOccurrence(
        currentOccurrence: LocalDate,
        frequency: RecurringFrequency,
        anchorStartDate: LocalDate,
    ): LocalDate = when (frequency) {
        RecurringFrequency.DAILY -> currentOccurrence.plusDays(1)
        RecurringFrequency.WEEKLY -> currentOccurrence.plusWeeks(1)
        RecurringFrequency.BIWEEKLY -> currentOccurrence.plusWeeks(2)
        RecurringFrequency.MONTHLY -> {
            val targetMonth = YearMonth.from(currentOccurrence).plusMonths(1)
            targetMonth.atDay(min(anchorStartDate.dayOfMonth, targetMonth.lengthOfMonth()))
        }
    }

    fun isOccurrenceDate(
        startDate: LocalDate,
        candidateDate: LocalDate,
        frequency: RecurringFrequency,
    ): Boolean {
        if (candidateDate.isBefore(startDate)) return false
        return when (frequency) {
            RecurringFrequency.DAILY -> true
            RecurringFrequency.WEEKLY ->
                ChronoUnit.DAYS.between(startDate, candidateDate) % 7L == 0L
            RecurringFrequency.BIWEEKLY ->
                ChronoUnit.DAYS.between(startDate, candidateDate) % 14L == 0L
            RecurringFrequency.MONTHLY -> {
                val candidateMonth = YearMonth.from(candidateDate)
                val expectedDay = min(startDate.dayOfMonth, candidateMonth.lengthOfMonth())
                candidateDate.dayOfMonth == expectedDay
            }
        }
    }

    fun firstOccurrenceAfter(
        currentNextExecutionDate: LocalDate,
        afterDate: LocalDate,
        frequency: RecurringFrequency,
        anchorStartDate: LocalDate,
    ): LocalDate {
        require(!currentNextExecutionDate.isBefore(anchorStartDate)) {
            "下次执行日期不能早于开始日期"
        }
        require(isOccurrenceDate(anchorStartDate, currentNextExecutionDate, frequency)) {
            "下次执行日期不在周期规则的执行序列上"
        }
        if (currentNextExecutionDate.isAfter(afterDate)) return currentNextExecutionDate

        return when (frequency) {
            RecurringFrequency.DAILY,
            RecurringFrequency.WEEKLY,
            RecurringFrequency.BIWEEKLY,
            -> {
                val intervalDays = when (frequency) {
                    RecurringFrequency.DAILY -> 1L
                    RecurringFrequency.WEEKLY -> 7L
                    RecurringFrequency.BIWEEKLY -> 14L
                    RecurringFrequency.MONTHLY -> error("已在外层分支处理")
                }
                val elapsedDays = ChronoUnit.DAYS.between(currentNextExecutionDate, afterDate)
                currentNextExecutionDate.plusDays((elapsedDays / intervalDays + 1L) * intervalDays)
            }
            RecurringFrequency.MONTHLY -> {
                var targetMonth = YearMonth.from(afterDate)
                var candidate = targetMonth.atDay(min(anchorStartDate.dayOfMonth, targetMonth.lengthOfMonth()))
                if (!candidate.isAfter(afterDate)) {
                    targetMonth = targetMonth.plusMonths(1)
                    candidate = targetMonth.atDay(min(anchorStartDate.dayOfMonth, targetMonth.lengthOfMonth()))
                }
                candidate
            }
        }
    }

    fun generatedTransactionId(ruleId: String, occurrenceDate: LocalDate): String {
        val source = "simple-ledger-recurring-v1|$ruleId|$occurrenceDate"
        return UUID.nameUUIDFromBytes(source.toByteArray(StandardCharsets.UTF_8)).toString()
    }

    const val MAX_CATCH_UP_OCCURRENCES = 3_660
}

class RecurringCatchUpLimitExceededException(message: String) : IllegalArgumentException(message)
