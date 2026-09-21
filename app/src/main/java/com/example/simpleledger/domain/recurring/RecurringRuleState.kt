package com.example.simpleledger.domain.recurring

import com.example.simpleledger.domain.model.RecurringRule
import java.time.LocalDate

object RecurringRuleState {
    fun withEnabledState(
        rule: RecurringRule,
        isEnabled: Boolean,
        effectiveDate: LocalDate,
        requestedUpdatedAtEpochMs: Long,
    ): RecurringRule {
        require(requestedUpdatedAtEpochMs >= 0L) { "更新时间无效" }
        val monotonicUpdatedAt = maxOf(
            requestedUpdatedAtEpochMs,
            rule.createdAtEpochMs,
            rule.updatedAtEpochMs,
        )
        val nextExecutionDate = if (isEnabled && !rule.isEnabled) {
            RecurringPostingPlanner.firstOccurrenceAfter(
                currentNextExecutionDate = LocalDate.parse(rule.nextExecutionDate),
                afterDate = effectiveDate,
                frequency = rule.frequency,
                anchorStartDate = LocalDate.parse(rule.startDate),
            ).toString()
        } else {
            rule.nextExecutionDate
        }
        return rule.copy(
            isEnabled = isEnabled,
            nextExecutionDate = nextExecutionDate,
            updatedAtEpochMs = monotonicUpdatedAt,
        )
    }
}
