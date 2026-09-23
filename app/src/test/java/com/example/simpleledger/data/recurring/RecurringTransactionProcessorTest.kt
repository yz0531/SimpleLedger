package com.example.simpleledger.data.recurring

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import com.example.simpleledger.domain.recurring.RecurringPostingPlanner
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringTransactionProcessorTest {
    @Test
    fun `processing catches up atomically and advances every rule`() = runBlocking {
        val store = FakeStore(
            rules = mutableListOf(
                rule(
                    id = "coffee",
                    frequency = RecurringFrequency.DAILY,
                    startDate = "2026-09-19",
                    nextExecutionDate = "2026-09-19",
                ),
                rule(
                    id = "rent",
                    frequency = RecurringFrequency.MONTHLY,
                    startDate = "2026-07-31",
                    nextExecutionDate = "2026-07-31",
                ),
            ),
        )
        val processor = RecurringTransactionProcessor(
            store = store,
            todayProvider = { LocalDate.parse("2026-09-21") },
            clock = { 5_000L },
        )

        val result = processor.processDue()

        assertEquals(1, store.transactionCount)
        assertEquals(2, result.processedRuleCount)
        assertEquals(5, result.createdTransactionCount)
        assertEquals(0, result.skippedDuplicateCount)
        assertEquals(
            listOf(
                "2026-07-31",
                "2026-08-31",
                "2026-09-19",
                "2026-09-20",
                "2026-09-21",
            ),
            store.transactions.values.map { it.occurredOn }.sorted(),
        )
        assertEquals("2026-09-22", store.rules.first { it.id == "coffee" }.nextExecutionDate)
        assertEquals("2026-09-30", store.rules.first { it.id == "rent" }.nextExecutionDate)
    }

    @Test
    fun `stable occurrence id skips a duplicate and still advances the rule`() = runBlocking {
        val rule = rule(
            id = "subscription",
            frequency = RecurringFrequency.MONTHLY,
            startDate = "2026-09-01",
            nextExecutionDate = "2026-09-01",
        )
        val duplicateDate = LocalDate.parse("2026-09-01")
        val duplicate = LedgerTransaction(
            id = RecurringPostingPlanner.generatedTransactionId(rule.id, duplicateDate),
            type = rule.type,
            amountMinor = rule.amountMinor,
            categoryId = rule.categoryId,
            occurredOn = duplicateDate.toString(),
            note = rule.note,
            createdAtEpochMs = 1_000L,
            updatedAtEpochMs = 1_000L,
        )
        val store = FakeStore(
            rules = mutableListOf(rule),
            transactions = linkedMapOf(duplicate.id to duplicate),
        )
        val processor = RecurringTransactionProcessor(
            store = store,
            todayProvider = { LocalDate.parse("2026-09-21") },
            clock = { 5_000L },
        )

        val result = processor.processDue()

        assertEquals(0, result.createdTransactionCount)
        assertEquals(1, result.skippedDuplicateCount)
        assertEquals("2026-10-01", store.rules.single().nextExecutionDate)
    }

    @Test
    fun `running processor twice is idempotent`() = runBlocking {
        val store = FakeStore(
            rules = mutableListOf(
                rule(
                    id = "daily",
                    frequency = RecurringFrequency.DAILY,
                    startDate = "2026-09-20",
                    nextExecutionDate = "2026-09-20",
                ),
            ),
        )
        val processor = RecurringTransactionProcessor(
            store = store,
            todayProvider = { LocalDate.parse("2026-09-21") },
            clock = { 5_000L },
        )

        val first = processor.processDue()
        val second = processor.processDue()

        assertEquals(2, first.createdTransactionCount)
        assertEquals(0, second.createdTransactionCount)
        assertEquals(2, store.transactions.size)
        assertEquals("2026-09-22", store.rules.single().nextExecutionDate)
    }

    @Test
    fun `restoring an older rule state does not duplicate generated transactions`() = runBlocking {
        val restoredRule = rule(
            id = "daily-backup",
            frequency = RecurringFrequency.DAILY,
            startDate = "2026-09-20",
            nextExecutionDate = "2026-09-20",
        )
        val store = FakeStore(rules = mutableListOf(restoredRule))
        val processor = RecurringTransactionProcessor(
            store = store,
            todayProvider = { LocalDate.parse("2026-09-21") },
            clock = { 5_000L },
        )

        val first = processor.processDue()
        store.rules[0] = restoredRule // Simulate importing the same older backup again.
        val afterRestore = processor.processDue()

        assertEquals(2, first.createdTransactionCount)
        assertEquals(0, afterRestore.createdTransactionCount)
        assertEquals(2, afterRestore.skippedDuplicateCount)
        assertEquals(2, store.transactions.size)
        assertEquals("2026-09-22", store.rules.single().nextExecutionDate)
    }

    @Test
    fun `failure rolls the whole processing transaction back`() = runBlocking {
        val store = FakeStore(
            rules = mutableListOf(
                rule(
                    id = "first",
                    frequency = RecurringFrequency.DAILY,
                    startDate = "2026-09-20",
                    nextExecutionDate = "2026-09-20",
                ),
                rule(
                    id = "deleted-during-processing",
                    frequency = RecurringFrequency.DAILY,
                    startDate = "2026-09-21",
                    nextExecutionDate = "2026-09-21",
                ),
            ),
            failRuleUpdateId = "deleted-during-processing",
        )
        val processor = RecurringTransactionProcessor(
            store = store,
            todayProvider = { LocalDate.parse("2026-09-21") },
            clock = { 5_000L },
        )

        val failure = runCatching { processor.processDue() }

        assertTrue(failure.isFailure)
        assertTrue(store.transactions.isEmpty())
        assertEquals("2026-09-20", store.rules.first { it.id == "first" }.nextExecutionDate)
    }

    private fun rule(
        id: String,
        frequency: RecurringFrequency,
        startDate: String,
        nextExecutionDate: String,
    ) = RecurringRule(
        id = id,
        type = TransactionType.EXPENSE,
        amountMinor = 1_500L,
        categoryId = Categories.expenseOther.id,
        note = id,
        frequency = frequency,
        startDate = startDate,
        nextExecutionDate = nextExecutionDate,
        isEnabled = true,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 1_000L,
    )
}

private class FakeStore(
    val rules: MutableList<RecurringRule>,
    val transactions: LinkedHashMap<String, LedgerTransaction> = linkedMapOf(),
    private val failRuleUpdateId: String? = null,
) : RecurringProcessingStore, RecurringProcessingSession {
    var transactionCount: Int = 0
        private set

    override suspend fun <T> inTransaction(
        block: suspend RecurringProcessingSession.() -> T,
    ): T {
        transactionCount += 1
        val rulesBefore = rules.toList()
        val transactionsBefore = LinkedHashMap(transactions)
        return try {
            block(this)
        } catch (exception: Exception) {
            rules.clear()
            rules.addAll(rulesBefore)
            transactions.clear()
            transactions.putAll(transactionsBefore)
            throw exception
        }
    }

    override suspend fun getDueRules(throughDate: LocalDate): List<RecurringRule> =
        rules.filter {
            it.isEnabled && !LocalDate.parse(it.nextExecutionDate).isAfter(throughDate)
        }.sortedBy { it.nextExecutionDate }

    override suspend fun insertTransactionIfAbsent(transaction: LedgerTransaction): Boolean =
        transactions.putIfAbsent(transaction.id, transaction) == null

    override suspend fun updateRuleAfterProcessing(
        id: String,
        nextExecutionDate: LocalDate,
        updatedAtEpochMs: Long,
    ): Boolean {
        if (id == failRuleUpdateId) return false
        val index = rules.indexOfFirst { it.id == id }
        if (index < 0) return false
        rules[index] = rules[index].copy(
            nextExecutionDate = nextExecutionDate.toString(),
            updatedAtEpochMs = updatedAtEpochMs,
        )
        return true
    }
}
