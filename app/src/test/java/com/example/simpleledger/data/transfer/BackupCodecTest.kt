package com.example.simpleledger.data.transfer

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val transaction = LedgerTransaction(
        id = "29bbf769-af0d-46f5-9ae8-e784529f31ae",
        type = TransactionType.EXPENSE,
        amountMinor = 1_250L,
        categoryId = Categories.expenseFood.id,
        occurredOn = "2026-09-21",
        note = "午餐",
        createdAtEpochMs = 100L,
        updatedAtEpochMs = 200L,
    )
    private val recurringRule = RecurringRule(
        id = "rent-rule",
        type = TransactionType.EXPENSE,
        amountMinor = 350_000L,
        categoryId = Categories.expenseHousing.id,
        note = "房租",
        frequency = RecurringFrequency.MONTHLY,
        startDate = "2026-01-31",
        nextExecutionDate = "2026-02-28",
        isEnabled = true,
        createdAtEpochMs = 100L,
        updatedAtEpochMs = 200L,
    )

    @Test
    fun jsonRoundTripPreservesTransactions() {
        val codec = BackupCodec(clock = { 300L })

        val decoded = codec.decodeBackup(codec.encode(listOf(transaction))).transactions

        assertEquals(listOf(transaction), decoded)
    }

    @Test
    fun jsonRoundTripPreservesRecurringRules() {
        val codec = BackupCodec(clock = { 300L })

        val decoded = codec.decodeBackup(codec.encode(listOf(transaction), listOf(recurringRule)))

        assertEquals(listOf(transaction), decoded.transactions)
        assertEquals(listOf(recurringRule), decoded.recurringRules)
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val codec = BackupCodec(clock = { 300L })
        val invalid = codec.encodeToString(listOf(transaction))
            .replace("\"schemaVersion\": 2", "\"schemaVersion\": 99")

        assertThrows(BackupValidationException::class.java) {
            codec.decodeBackup(invalid)
        }
    }

    @Test
    fun rejectsUnknownCategory() {
        val codec = BackupCodec(clock = { 300L })
        val invalid = codec.encodeToString(listOf(transaction))
            .replace(Categories.expenseFood.id, "expense.unknown")

        assertThrows(BackupValidationException::class.java) {
            codec.decodeBackup(invalid)
        }
    }

    @Test
    fun rejectsUnknownJsonFields() {
        val codec = BackupCodec(clock = { 300L })
        val invalid = codec.encodeToString(listOf(transaction))
            .replace("\"currency\": \"CNY\"", "\"currency\": \"CNY\", \"unexpected\": true")

        assertThrows(BackupValidationException::class.java) {
            codec.decodeBackup(invalid)
        }
    }

    @Test
    fun rejectsOversizedInputBeforeParsing() {
        val oversized = ByteArray(BackupCodec.MAX_FILE_BYTES + 1) { ' '.code.toByte() }

        assertThrows(BackupValidationException::class.java) {
            BackupCodec().decodeBackup(oversized)
        }
    }

    @Test
    fun refusesToCreateABackupThatCannotBeImported() {
        val largeLedger = List(20_000) { index ->
            transaction.copy(id = "transaction-$index", note = "记".repeat(500))
        }

        assertThrows(BackupValidationException::class.java) {
            BackupCodec(clock = { 300L }).encode(largeLedger)
        }
    }
}
