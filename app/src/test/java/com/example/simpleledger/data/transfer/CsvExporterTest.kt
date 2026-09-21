package com.example.simpleledger.data.transfer

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.TransactionType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun writesUtf8BomEscapesCellsAndNeutralizesFormulas() {
        val transaction = LedgerTransaction(
            id = "id-1",
            type = TransactionType.EXPENSE,
            amountMinor = 1_250L,
            categoryId = Categories.expenseFood.id,
            occurredOn = "2026-09-21",
            note = "=SUM(1,2)\n\"quoted\"",
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 200L,
        )

        val bytes = CsvExporter().encode(listOf(transaction))
        val text = bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)

        assertArrayEquals(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()),
            bytes.copyOfRange(0, 3),
        )
        assertTrue(text.contains("12.50,1250,CNY"))
        assertTrue(text.contains("\"'=SUM(1,2)\n\"\"quoted\"\"\""))
        assertTrue(text.lines().first().startsWith("id,type,amount,"))
    }
}
