package com.example.simpleledger.domain.recurring

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringRuleStateTest {
    @Test
    fun `reenabling skips occurrences from disabled period`() {
        val resumed = RecurringRuleState.withEnabledState(
            rule = rule(
                isEnabled = false,
                startDate = "2026-01-31",
                nextExecutionDate = "2026-02-28",
            ),
            isEnabled = true,
            effectiveDate = LocalDate.parse("2026-09-30"),
            requestedUpdatedAtEpochMs = 3_000L,
        )

        assertTrue(resumed.isEnabled)
        assertEquals("2026-10-31", resumed.nextExecutionDate)
    }

    @Test
    fun `disabling keeps next occurrence and update time is monotonic`() {
        val disabled = RecurringRuleState.withEnabledState(
            rule = rule(
                isEnabled = true,
                startDate = "2026-09-30",
                nextExecutionDate = "2026-10-31",
            ),
            isEnabled = false,
            effectiveDate = LocalDate.parse("2026-09-21"),
            requestedUpdatedAtEpochMs = 500L,
        )

        assertFalse(disabled.isEnabled)
        assertEquals("2026-10-31", disabled.nextExecutionDate)
        assertEquals(2_000L, disabled.updatedAtEpochMs)
    }

    private fun rule(
        isEnabled: Boolean,
        startDate: String,
        nextExecutionDate: String,
    ) = RecurringRule(
        id = "monthly-rule",
        type = TransactionType.EXPENSE,
        amountMinor = 1_500L,
        categoryId = Categories.expenseOther.id,
        note = "订阅",
        frequency = RecurringFrequency.MONTHLY,
        startDate = startDate,
        nextExecutionDate = nextExecutionDate,
        isEnabled = isEnabled,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 2_000L,
    )
}
