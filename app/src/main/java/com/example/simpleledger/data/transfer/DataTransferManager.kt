package com.example.simpleledger.data.transfer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.simpleledger.domain.model.ExportResult
import com.example.simpleledger.domain.model.ClearDataResult
import com.example.simpleledger.domain.model.ImportResult
import com.example.simpleledger.domain.repository.LedgerRepository
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataTransferException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

class DataTransferManager(
    context: Context,
    private val repository: LedgerRepository,
    private val backupStore: LedgerBackupStore,
    private val backupCodec: BackupCodec = BackupCodec(),
    private val csvExporter: CsvExporter = CsvExporter(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    suspend fun exportJson(uri: Uri): ExportResult = withContext(ioDispatcher) {
        val backup = backupStore.snapshot()
        val bytes = backupCodec.encode(backup.transactions, backup.recurringRules)
        write(uri, bytes)
        ExportResult(
            exportedCount = backup.transactions.size,
            recurringRuleCount = backup.recurringRules.size,
        )
    }

    suspend fun exportCsv(uri: Uri): ExportResult = withContext(ioDispatcher) {
        val transactions = repository.getAllSnapshot()
        val bytes = csvExporter.encode(transactions)
        write(uri, bytes)
        ExportResult(
            exportedCount = transactions.size,
        )
    }

    suspend fun importJson(uri: Uri): ImportResult = withContext(ioDispatcher) {
        val bytes = readBounded(uri)
        val backup = backupCodec.decodeBackup(bytes)
        backupStore.import(backup)
    }

    suspend fun clearLocalData(): ClearDataResult = withContext(ioDispatcher) {
        backupStore.clearAll()
    }

    private fun write(uri: Uri, bytes: ByteArray) {
        val stream = contentResolver.openOutputStream(uri, "w")
            ?: throw DataTransferException("无法打开所选文件进行写入")
        try {
            BufferedOutputStream(stream).use { output ->
                output.write(bytes)
                output.flush()
            }
        } catch (exception: IOException) {
            throw DataTransferException("写入文件失败，请检查存储空间或文件权限", exception)
        }
    }

    private fun readBounded(uri: Uri): ByteArray {
        val stream = contentResolver.openInputStream(uri)
            ?: throw DataTransferException("无法打开所选备份文件")
        try {
            return stream.buffered().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > BackupCodec.MAX_FILE_BYTES) {
                        throw BackupValidationException("备份文件不能超过 10 MB")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (exception: BackupValidationException) {
            throw exception
        } catch (exception: IOException) {
            throw DataTransferException("读取备份文件失败，请检查文件权限", exception)
        }
    }
}
