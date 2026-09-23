package com.example.simpleledger.data.transfer

import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.MAX_LEDGER_NOTE_LENGTH
import com.example.simpleledger.domain.model.MAX_RECURRING_RULE_ID_LENGTH
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import com.example.simpleledger.domain.recurring.RecurringPostingPlanner
import com.example.simpleledger.util.DateValidator
import com.example.simpleledger.util.MoneyParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupValidationException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

data class LedgerBackup(
    val transactions: List<LedgerTransaction>,
    val recurringRules: List<RecurringRule>,
)

class BackupCodec(
    private val clock: () -> Long = System::currentTimeMillis,
    private val json: Json = defaultJson,
) {
    fun encode(
        transactions: List<LedgerTransaction>,
        recurringRules: List<RecurringRule> = emptyList(),
    ): ByteArray {
        val bytes = encodeToString(transactions, recurringRules).toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_FILE_BYTES) {
            throw BackupValidationException("账本备份超过 10 MB，请先导出 CSV 或精简较长备注")
        }
        return bytes
    }

    fun encodeToString(
        transactions: List<LedgerTransaction>,
        recurringRules: List<RecurringRule> = emptyList(),
    ): String {
        validateTransactions(transactions)
        validateRecurringRules(recurringRules)
        val exportedAtEpochMs = clock()
        if (exportedAtEpochMs < 0L) {
            throw BackupValidationException("导出时间无效")
        }
        val document = BackupDocumentDto(
            schemaVersion = SCHEMA_VERSION,
            currency = CURRENCY,
            exportedAtEpochMs = exportedAtEpochMs,
            transactions = transactions.map { it.toDto() },
            recurringRules = recurringRules.map { it.toDto() },
        )
        return json.encodeToString(document)
    }

    fun decodeBackup(bytes: ByteArray): LedgerBackup {
        if (bytes.size > MAX_FILE_BYTES) {
            throw BackupValidationException("备份文件不能超过 10 MB")
        }
        val text = try {
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (exception: Exception) {
            throw BackupValidationException("备份文件不是有效的 UTF-8 文本", exception)
        }
        return decodeBackup(text.removePrefix("\uFEFF"), checkSize = false)
    }

    fun decodeBackup(text: String): LedgerBackup = decodeBackup(text, checkSize = true)

    private fun decodeBackup(text: String, checkSize: Boolean): LedgerBackup {
        if (checkSize && text.toByteArray(Charsets.UTF_8).size > MAX_FILE_BYTES) {
            throw BackupValidationException("备份文件不能超过 10 MB")
        }

        val document = try {
            json.decodeFromString<BackupDocumentDto>(text.removePrefix("\uFEFF"))
        } catch (exception: SerializationException) {
            throw BackupValidationException("备份 JSON 格式无效：${exception.message ?: "无法解析"}", exception)
        } catch (exception: IllegalArgumentException) {
            throw BackupValidationException("备份 JSON 格式无效：${exception.message ?: "无法解析"}", exception)
        }

        if (document.schemaVersion !in MIN_SUPPORTED_SCHEMA_VERSION..SCHEMA_VERSION) {
            throw BackupValidationException(
                "不支持的备份版本 ${document.schemaVersion}，当前支持版本 $MIN_SUPPORTED_SCHEMA_VERSION–$SCHEMA_VERSION",
            )
        }
        if (document.currency != CURRENCY) {
            throw BackupValidationException("仅支持 CNY 账本，文件币种为 ${document.currency}")
        }
        if (document.exportedAtEpochMs < 0L) {
            throw BackupValidationException("导出时间无效")
        }
        if (document.transactions.size > MAX_TRANSACTION_COUNT) {
            throw BackupValidationException("单次最多导入 $MAX_TRANSACTION_COUNT 条记录")
        }
        if (document.recurringRules.size > MAX_RECURRING_RULE_COUNT) {
            throw BackupValidationException("单次最多导入 $MAX_RECURRING_RULE_COUNT 条周期规则")
        }

        val transactions = document.transactions.mapIndexed { index, dto ->
            dto.toDomain(index)
        }
        validateTransactions(transactions)
        val recurringRules = document.recurringRules.mapIndexed { index, dto ->
            dto.toDomain(index)
        }
        validateRecurringRules(recurringRules)
        return LedgerBackup(transactions, recurringRules)
    }

    private fun validateTransactions(transactions: List<LedgerTransaction>) {
        if (transactions.size > MAX_TRANSACTION_COUNT) {
            throw BackupValidationException("单次最多处理 $MAX_TRANSACTION_COUNT 条记录")
        }
        val ids = HashSet<String>(transactions.size)
        transactions.forEachIndexed { index, transaction ->
            validateTransaction(transaction, index)
            if (!ids.add(transaction.id)) {
                throw BackupValidationException("第 ${index + 1} 条记录的 ID 与文件内其他记录重复")
            }
        }
    }

    private fun validateTransaction(transaction: LedgerTransaction, index: Int) {
        val prefix = "第 ${index + 1} 条记录"
        if (
            transaction.id.isBlank() ||
            transaction.id.length > MAX_ID_LENGTH ||
            transaction.id.any { it.isISOControl() }
        ) {
            throw BackupValidationException("$prefix 的 ID 无效")
        }
        if (transaction.amountMinor !in 1..MoneyParser.MAX_AMOUNT_MINOR) {
            throw BackupValidationException("$prefix 的金额无效")
        }
        val category = Categories.find(transaction.categoryId)
            ?: throw BackupValidationException("$prefix 包含未知分类 ${transaction.categoryId}")
        if (category.type != transaction.type) {
            throw BackupValidationException("$prefix 的分类与收支类型不匹配")
        }
        if (!DateValidator.isValidIsoLocalDate(transaction.occurredOn)) {
            throw BackupValidationException("$prefix 的日期无效，应使用 yyyy-MM-dd")
        }
        if (transaction.note.length > MAX_LEDGER_NOTE_LENGTH) {
            throw BackupValidationException("$prefix 的备注不能超过 $MAX_LEDGER_NOTE_LENGTH 个字符")
        }
        if (transaction.createdAtEpochMs < 0L || transaction.updatedAtEpochMs < transaction.createdAtEpochMs) {
            throw BackupValidationException("$prefix 的创建或更新时间无效")
        }
    }

    private fun validateRecurringRules(rules: List<RecurringRule>) {
        if (rules.size > MAX_RECURRING_RULE_COUNT) {
            throw BackupValidationException("单次最多处理 $MAX_RECURRING_RULE_COUNT 条周期规则")
        }
        val ids = HashSet<String>(rules.size)
        rules.forEachIndexed { index, rule ->
            val prefix = "第 ${index + 1} 条周期规则"
            if (
                rule.id.isBlank() ||
                rule.id.length > MAX_RECURRING_RULE_ID_LENGTH ||
                rule.id.any { it.isISOControl() }
            ) {
                throw BackupValidationException("$prefix 的 ID 无效")
            }
            if (!ids.add(rule.id)) {
                throw BackupValidationException("$prefix 的 ID 与文件内其他周期规则重复")
            }
            if (rule.amountMinor !in 1..MoneyParser.MAX_AMOUNT_MINOR) {
                throw BackupValidationException("$prefix 的金额无效")
            }
            val category = Categories.find(rule.categoryId)
                ?: throw BackupValidationException("$prefix 包含未知分类 ${rule.categoryId}")
            if (category.type != rule.type) {
                throw BackupValidationException("$prefix 的分类与收支类型不匹配")
            }
            if (rule.note.length > MAX_LEDGER_NOTE_LENGTH) {
                throw BackupValidationException("$prefix 的备注不能超过 $MAX_LEDGER_NOTE_LENGTH 个字符")
            }
            if (!DateValidator.isValidIsoLocalDate(rule.startDate) ||
                !DateValidator.isValidIsoLocalDate(rule.nextExecutionDate)
            ) {
                throw BackupValidationException("$prefix 的日期无效")
            }
            val start = java.time.LocalDate.parse(rule.startDate)
            val next = java.time.LocalDate.parse(rule.nextExecutionDate)
            if (!RecurringPostingPlanner.isOccurrenceDate(start, next, rule.frequency)) {
                throw BackupValidationException("$prefix 的下次执行日期不符合周期")
            }
            if (rule.createdAtEpochMs < 0L || rule.updatedAtEpochMs < rule.createdAtEpochMs) {
                throw BackupValidationException("$prefix 的创建或更新时间无效")
            }
        }
    }

    private fun BackupTransactionDto.toDomain(index: Int): LedgerTransaction {
        val parsedType = TransactionType.fromWireValue(type)
            ?: throw BackupValidationException("第 ${index + 1} 条记录包含未知类型 $type")
        if (currency != CURRENCY) {
            throw BackupValidationException("第 ${index + 1} 条记录的币种不是 CNY")
        }
        return LedgerTransaction(
            id = id,
            type = parsedType,
            amountMinor = amountMinor,
            categoryId = categoryId,
            occurredOn = occurredOn,
            note = note,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )
    }

    private fun LedgerTransaction.toDto(): BackupTransactionDto = BackupTransactionDto(
        id = id,
        type = type.wireValue,
        amountMinor = amountMinor,
        currency = CURRENCY,
        categoryId = categoryId,
        occurredOn = occurredOn,
        note = note,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
    )

    private fun BackupRecurringRuleDto.toDomain(index: Int): RecurringRule {
        val parsedType = TransactionType.fromWireValue(type)
            ?: throw BackupValidationException("第 ${index + 1} 条周期规则包含未知类型 $type")
        val parsedFrequency = RecurringFrequency.fromWireValue(frequency)
            ?: throw BackupValidationException("第 ${index + 1} 条周期规则包含未知频率 $frequency")
        return RecurringRule(
            id = id,
            type = parsedType,
            amountMinor = amountMinor,
            categoryId = categoryId,
            note = note,
            frequency = parsedFrequency,
            startDate = startDate,
            nextExecutionDate = nextExecutionDate,
            isEnabled = isEnabled,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )
    }

    private fun RecurringRule.toDto(): BackupRecurringRuleDto = BackupRecurringRuleDto(
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

    companion object {
        const val SCHEMA_VERSION = 2
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1
        const val CURRENCY = "CNY"
        const val MAX_FILE_BYTES = 10 * 1024 * 1024
        const val MAX_TRANSACTION_COUNT = 50_000
        const val MAX_RECURRING_RULE_COUNT = 10_000
        const val MAX_ID_LENGTH = 128

        private val defaultJson = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = false
            isLenient = false
            coerceInputValues = false
        }
    }
}

@Serializable
private data class BackupDocumentDto(
    val schemaVersion: Int,
    val currency: String,
    val exportedAtEpochMs: Long,
    val transactions: List<BackupTransactionDto>,
    val recurringRules: List<BackupRecurringRuleDto> = emptyList(),
)

@Serializable
private data class BackupTransactionDto(
    val id: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val occurredOn: String,
    val note: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Serializable
private data class BackupRecurringRuleDto(
    val id: String,
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
