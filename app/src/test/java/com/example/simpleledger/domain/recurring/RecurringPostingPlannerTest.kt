package com.example.simpleledger.domain.recurring

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringPostingPlannerTest {
    @Test
    fun `daily rule catches up through today and advances to tomorrow`() {
        val plan = RecurringPostingPlanner.plan(
            rule = rule(
                frequency = RecurringFrequency.DAILY,
                startDate = "2026-09-18",
                nextExecutionDate = "2026-09-18",
            ),
            throughDate = LocalDate.parse("2026-09-21"),
            generatedAtEpochMs = 2_000L,
        )

        assertEquals(
            listOf("2026-09-18", "2026-09-19", "2026-09-20", "2026-09-21"),
            plan.transactions.map { it.occurredOn },
        )
        assertEquals(LocalDate.parse("2026-09-22"), plan.nextExecutionDate)
        assertTrue(plan.transactions.all { it.createdAtEpochMs == 2_000L })
    }

    @Test
    fun `weekly and biweekly rules use exact week intervals`() {
        val weekly = RecurringPostingPlanner.nextOccurrence(
            LocalDate.parse("2026-09-21"),
            RecurringFrequency.WEEKLY,
            LocalDate.parse("2026-09-21"),
        )
        val biweekly = RecurringPostingPlanner.nextOccurrence(
            LocalDate.parse("2026-09-21"),
            RecurringFrequency.BIWEEKLY,
            LocalDate.parse("2026-09-21"),
        )

        assertEquals(LocalDate.parse("2026-09-28"), weekly)
        assertEquals(LocalDate.parse("2026-10-05"), biweekly)
    }

    @Test
    fun `weekly schedules stay on the selected weekday after a long pause`() {
        val weekly = RecurringPostingPlanner.firstOccurrenceAfter(
            currentNextExecutionDate = LocalDate.parse("2026-01-07"),
            afterDate = LocalDate.parse("2026-09-21"),
            frequency = RecurringFrequency.WEEKLY,
            anchorStartDate = LocalDate.parse("2026-01-07"),
        )
        val biweekly = RecurringPostingPlanner.firstOccurrenceAfter(
            currentNextExecutionDate = LocalDate.parse("2026-01-07"),
            afterDate = LocalDate.parse("2026-09-21"),
            frequency = RecurringFrequency.BIWEEKLY,
            anchorStartDate = LocalDate.parse("2026-01-07"),
        )

        assertEquals(LocalDate.parse("2026-09-23"), weekly)
        assertEquals(LocalDate.parse("2026-09-30"), biweekly)
        assertEquals(LocalDate.parse("2026-01-07").dayOfWeek, weekly.dayOfWeek)
        assertEquals(LocalDate.parse("2026-01-07").dayOfWeek, biweekly.dayOfWeek)
    }

    @Test
    fun `monthly rule remains anchored after a short month`() {
        val january = LocalDate.parse("2025-01-31")
        val february = RecurringPostingPlanner.nextOccurrence(
            january,
            RecurringFrequency.MONTHLY,
            january,
        )
        val march = RecurringPostingPlanner.nextOccurrence(
            february,
            RecurringFrequency.MONTHLY,
            january,
        )

        assertEquals(LocalDate.parse("2025-02-28"), february)
        assertEquals(LocalDate.parse("2025-03-31"), march)
    }

    @Test
    fun `monthly rule clips leap year February to the 29th`() {
        val next = RecurringPostingPlanner.nextOccurrence(
            currentOccurrence = LocalDate.parse("2024-01-31"),
            frequency = RecurringFrequency.MONTHLY,
            anchorStartDate = LocalDate.parse("2024-01-31"),
        )

        assertEquals(LocalDate.parse("2024-02-29"), next)
    }

    @Test
    fun `disabled or future rule produces no transaction`() {
        val disabledPlan = RecurringPostingPlanner.plan(
            rule = rule(isEnabled = false),
            throughDate = LocalDate.parse("2026-09-21"),
            generatedAtEpochMs = 2_000L,
        )
        val futurePlan = RecurringPostingPlanner.plan(
            rule = rule(
                startDate = "2026-10-01",
                nextExecutionDate = "2026-10-01",
            ),
            throughDate = LocalDate.parse("2026-09-21"),
            generatedAtEpochMs = 2_000L,
        )

        assertTrue(disabledPlan.transactions.isEmpty())
        assertTrue(futurePlan.transactions.isEmpty())
    }

    @Test
    fun `generated transaction ids are stable per rule and date`() {
        val date = LocalDate.parse("2026-09-21")
        val first = RecurringPostingPlanner.generatedTransactionId("rent", date)
        val retry = RecurringPostingPlanner.generatedTransactionId("rent", date)
        val anotherDate = RecurringPostingPlanner.generatedTransactionId("rent", date.plusMonths(1))

        assertEquals(first, retry)
        assertTrue(first != anotherDate)
    }

    @Test
    fun `occurrence validation follows each frequency`() {
        val start = LocalDate.parse("2026-09-01")

        assertTrue(
            RecurringPostingPlanner.isOccurrenceDate(
                start,
                LocalDate.parse("2026-09-15"),
                RecurringFrequency.BIWEEKLY,
            ),
        )
        assertFalse(
            RecurringPostingPlanner.isOccurrenceDate(
                start,
                LocalDate.parse("2026-09-08"),
                RecurringFrequency.BIWEEKLY,
            ),
        )
        assertTrue(
            RecurringPostingPlanner.isOccurrenceDate(
                LocalDate.parse("2026-01-31"),
                LocalDate.parse("2026-02-28"),
                RecurringFrequency.MONTHLY,
            ),
        )
    }

    @Test
    fun `reenabling skips disabled period and chooses first occurrence after today`() {
        val next = RecurringPostingPlanner.firstOccurrenceAfter(
            currentNextExecutionDate = LocalDate.parse("2026-01-31"),
            afterDate = LocalDate.parse("2026-09-30"),
            frequency = RecurringFrequency.MONTHLY,
            anchorStartDate = LocalDate.parse("2026-01-31"),
        )

        assertEquals(LocalDate.parse("2026-10-31"), next)
    }

    @Test
    fun `catch up over safety limit fails without returning a partial plan`() {
        assertThrows(RecurringCatchUpLimitExceededException::class.java) {
            RecurringPostingPlanner.plan(
                rule = rule(
                    frequency = RecurringFrequency.DAILY,
                    startDate = "2010-01-01",
                    nextExecutionDate = "2010-01-01",
                ),
                throughDate = LocalDate.parse("2026-09-21"),
                generatedAtEpochMs = 2_000L,
            )
        }
    }

    private fun rule(
        frequency: RecurringFrequency = RecurringFrequency.DAILY,
        startDate: String = "2026-09-21",
        nextExecutionDate: String = startDate,
        isEnabled: Boolean = true,
    ) = RecurringRule(
        id = "rule-id",
        type = TransactionType.EXPENSE,
        amountMinor = 1_500L,
        categoryId = Categories.expenseFood.id,
        note = "早餐",
        frequency = frequency,
        startDate = startDate,
        nextExecutionDate = nextExecutionDate,
        isEnabled = isEnabled,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 1_000L,
    )
}
