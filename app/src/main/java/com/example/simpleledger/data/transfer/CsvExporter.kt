package com.example.simpleledger.data.transfer

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.util.MoneyFormatter
import java.io.ByteArrayOutputStream

class CsvExporter {
    fun encode(transactions: List<LedgerTransaction>): ByteArray {
        val body = encodeToString(transactions).toByteArray(Charsets.UTF_8)
        return ByteArrayOutputStream(UTF8_BOM.size + body.size).use { output ->
            output.write(UTF8_BOM)
            output.write(body)
            output.toByteArray()
        }
    }

    fun export(transactions: List<LedgerTransaction>): ByteArray = encode(transactions)

    fun encodeToString(transactions: List<LedgerTransaction>): String = buildString {
        append(
            listOf(
                "id",
                "type",
                "amount",
                "amount_minor",
                "currency",
                "category_id",
                "category_name",
                "occurred_on",
                "note",
                "created_at_epoch_ms",
                "updated_at_epoch_ms",
            ).joinToString(","),
        )
        append("\r\n")
        transactions.forEach { transaction ->
            val categoryName = Categories.find(transaction.categoryId)?.label.orEmpty()
            val values = listOf(
                transaction.id,
                transaction.type.wireValue,
                MoneyFormatter.toPlainAmount(transaction.amountMinor),
                transaction.amountMinor.toString(),
                BackupCodec.CURRENCY,
                transaction.categoryId,
                categoryName,
                transaction.occurredOn,
                transaction.note,
                transaction.createdAtEpochMs.toString(),
                transaction.updatedAtEpochMs.toString(),
            )
            append(values.joinToString(",") { escapeCell(neutralizeFormula(it)) })
            append("\r\n")
        }
    }

    private fun neutralizeFormula(value: String): String {
        val firstMeaningful = value.firstOrNull { !it.isWhitespace() } ?: return value
        return if (firstMeaningful in FORMULA_PREFIXES) "'$value" else value
    }

    private fun escapeCell(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\r' || it == '\n' }) return value
        return buildString(value.length + 2) {
            append('"')
            value.forEach { character ->
                if (character == '"') append("\"\"") else append(character)
            }
            append('"')
        }
    }

    companion object {
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')
    }
}
