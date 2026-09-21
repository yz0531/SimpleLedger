package com.example.simpleledger.data.backup

import com.example.simpleledger.data.transfer.BackupCodec
import com.example.simpleledger.data.transfer.LedgerBackup
import com.example.simpleledger.data.transfer.LedgerBackupStore
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class NutstoreBackupStatus(
    val isRunning: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

enum class AutomaticBackupResult {
    BACKED_UP,
    UNCHANGED,
    DISABLED,
    NOT_CONFIGURED,
}

class NutstoreBackupManager(
    private val backupStore: LedgerBackupStore,
    private val credentialStore: NutstoreCredentialStore,
    private val backupCodec: BackupCodec = BackupCodec(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    private val clientFactory: (NutstoreCredentials) -> NutstoreWebDavClient = ::NutstoreWebDavClient,
) {
    private val operationMutex = Mutex()
    private val mutableStatus = MutableStateFlow(NutstoreBackupStatus())

    val settings: StateFlow<NutstoreSettings> = credentialStore.settings
    val status: StateFlow<NutstoreBackupStatus> = mutableStatus.asStateFlow()

    suspend fun connectAndSave(username: String, password: String): Boolean = runOperation(
        runningMessage = "正在连接坚果云…",
        successMessage = "坚果云已连接，备份目录已准备好",
    ) {
        require(username.isNotBlank()) { "请输入坚果云账号" }
        require(password.isNotBlank()) { "请输入坚果云第三方应用密码" }
        val credentials = NutstoreCredentials(username.trim(), password)
        withContext(ioDispatcher) {
            clientFactory(credentials).ensureBackupDirectory()
        }
        credentialStore.saveCredentials(credentials.username, credentials.password)
        true
    } ?: false

    fun setAutomaticBackupEnabled(enabled: Boolean) {
        credentialStore.setAutomaticBackupEnabled(enabled)
        mutableStatus.value = NutstoreBackupStatus(
            message = if (enabled) "已开启进入应用时自动备份" else "已关闭自动备份",
        )
    }

    suspend fun manualBackup(): Boolean = runOperation(
        runningMessage = "正在上传备份…",
        successMessage = "已备份到坚果云",
    ) {
        val credentials = requireCredentials()
        withContext(ioDispatcher) {
            val snapshot = backupStore.snapshot()
            val fingerprint = BackupFingerprint.calculate(snapshot)
            val timestamp = clock()
            val client = clientFactory(credentials)
            client.ensureBackupDirectory()
            client.upload(
                NutstoreBackupFiles.manualName(timestamp),
                backupCodec.encode(snapshot.transactions, snapshot.recurringRules),
            )
            credentialStore.markBackupSucceeded(fingerprint, timestamp)
        }
        true
    } ?: false

    suspend fun restoreLatestBackup(): Boolean = runOperation(
        runningMessage = "正在下载最近备份…",
        successMessage = null,
    ) {
        val credentials = requireCredentials()
        withContext(ioDispatcher) {
            val client = clientFactory(credentials)
            client.ensureBackupDirectory()
            val latestFile = NutstoreBackupFiles.mostRecent(client.listBackupFiles())
                ?: throw IllegalStateException("坚果云 backup 文件夹中还没有可恢复的备份")
            val backup = backupCodec.decodeBackup(client.download(latestFile))
            val result = backupStore.import(backup)
            mutableStatus.value = NutstoreBackupStatus(
                message = buildString {
                    append("已从 ")
                    append(latestFile)
                    append(" 恢复：账目新增 ")
                    append(result.insertedCount)
                    append(" 条、更新 ")
                    append(result.updatedCount)
                    append(" 条；周期新增 ")
                    append(result.recurringInsertedCount)
                    append(" 条、更新 ")
                    append(result.recurringUpdatedCount)
                    append(" 条")
                },
            )
        }
        true
    } ?: false

    suspend fun automaticBackupIfChanged(): AutomaticBackupResult {
        val settings = credentialStore.settings.value
        if (!settings.automaticBackupEnabled) return AutomaticBackupResult.DISABLED
        val credentials = credentialStore.credentials() ?: run {
            if (settings.hasCredentials) {
                mutableStatus.value = NutstoreBackupStatus(
                    message = "无法读取已保存的密码，请在设置中重新连接坚果云",
                    isError = true,
                )
            }
            return AutomaticBackupResult.NOT_CONFIGURED
        }

        return runOperation(
            runningMessage = "正在检查自动备份…",
            successMessage = null,
            reportErrors = true,
        ) {
            withContext(ioDispatcher) {
                val snapshot = backupStore.snapshot()
                val fingerprint = BackupFingerprint.calculate(snapshot)
                if (fingerprint == credentialStore.lastFingerprint()) {
                    mutableStatus.value = NutstoreBackupStatus(message = "账本没有变化，无需重复备份")
                    return@withContext AutomaticBackupResult.UNCHANGED
                }

                val timestamp = clock()
                val client = clientFactory(credentials)
                client.ensureBackupDirectory()
                client.upload(
                    NutstoreBackupFiles.automaticName(timestamp),
                    backupCodec.encode(snapshot.transactions, snapshot.recurringRules),
                )
                val obsoleteFiles = NutstoreBackupFiles.filesToDelete(client.listBackupFiles())
                obsoleteFiles.forEach(client::delete)
                credentialStore.markBackupSucceeded(fingerprint, timestamp)
                mutableStatus.value = NutstoreBackupStatus(message = "账本有变化，已自动备份到坚果云")
                AutomaticBackupResult.BACKED_UP
            }
        } ?: AutomaticBackupResult.NOT_CONFIGURED
    }

    private fun requireCredentials(): NutstoreCredentials = credentialStore.credentials()
        ?: throw IllegalStateException("请先输入账号和第三方应用密码并完成连接")

    private suspend fun <T> runOperation(
        runningMessage: String,
        successMessage: String?,
        reportErrors: Boolean = true,
        block: suspend () -> T,
    ): T? = operationMutex.withLock {
        mutableStatus.value = NutstoreBackupStatus(isRunning = true, message = runningMessage)
        try {
            val result = block()
            if (successMessage != null) {
                mutableStatus.value = NutstoreBackupStatus(message = successMessage)
            } else if (mutableStatus.value.isRunning) {
                mutableStatus.value = NutstoreBackupStatus()
            }
            result
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (reportErrors) {
                mutableStatus.value = NutstoreBackupStatus(
                    message = exception.message ?: "坚果云备份失败，请稍后重试",
                    isError = true,
                )
            } else {
                mutableStatus.value = NutstoreBackupStatus()
            }
            null
        }
    }
}

internal object BackupFingerprint {
    private val stableCodec = BackupCodec(clock = { 0L })

    fun calculate(backup: LedgerBackup): String {
        val bytes = stableCodec.encode(
            transactions = backup.transactions.sortedBy { it.id },
            recurringRules = backup.recurringRules.sortedBy { it.id },
        )
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

internal object NutstoreBackupFiles {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    private val automaticPattern = Regex("^auto_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3})\\.json$")
    private val backupPattern = Regex("^(?:auto|manual)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3})\\.json$")

    fun automaticName(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        "auto_${format(epochMs, zoneId)}.json"

    fun manualName(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        "manual_${format(epochMs, zoneId)}.json"

    fun filesToDelete(remoteNames: List<String>, keep: Int = 3): List<String> = remoteNames
        .mapNotNull { name -> automaticPattern.matchEntire(name)?.groupValues?.get(1)?.let { it to name } }
        .sortedByDescending { it.first }
        .drop(keep.coerceAtLeast(0))
        .map { it.second }

    fun mostRecent(remoteNames: List<String>): String? = remoteNames
        .mapNotNull { name -> backupPattern.matchEntire(name)?.groupValues?.get(1)?.let { it to name } }
        .maxByOrNull { it.first }
        ?.second

    private fun format(epochMs: Long, zoneId: ZoneId): String =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).format(formatter)
}
