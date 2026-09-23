package com.example.simpleledger.data.backup

import com.example.simpleledger.data.transfer.BackupCodec
import com.example.simpleledger.data.transfer.LedgerBackup
import com.example.simpleledger.data.transfer.LedgerBackupStore
import com.example.simpleledger.domain.model.ImportResult
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
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

enum class NutstoreBackupKind {
    AUTOMATIC,
    MANUAL,
}

data class NutstoreBackupFile(
    val fileName: String,
    val kind: NutstoreBackupKind,
    val timestampText: String,
    val createdAtEpochMs: Long?,
)

data class NutstoreBackupCatalog(
    val files: List<NutstoreBackupFile> = emptyList(),
    val hasLoaded: Boolean = false,
)

class NutstoreBackupManager(
    private val backupStore: LedgerBackupStore,
    private val credentialStore: NutstoreCredentialStore,
    private val backupCodec: BackupCodec = BackupCodec(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    private val clientFactory: (NutstoreCredentials) -> NutstoreWebDavClient = ::NutstoreWebDavClient,
    private val postRestoreMaintenance: suspend () -> Unit = {},
) {
    private val operationMutex = Mutex()
    private val mutableStatus = MutableStateFlow(NutstoreBackupStatus())
    private val mutableBackupCatalog = MutableStateFlow(NutstoreBackupCatalog())

    val settings: StateFlow<NutstoreSettings> = credentialStore.settings
    val status: StateFlow<NutstoreBackupStatus> = mutableStatus.asStateFlow()
    val backupCatalog: StateFlow<NutstoreBackupCatalog> = mutableBackupCatalog.asStateFlow()

    fun savedCredentials(): NutstoreCredentials? = credentialStore.credentials()

    suspend fun connectAndSave(username: String, password: String): Unit = runOperation(
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
    }

    fun setAutomaticBackupEnabled(enabled: Boolean) {
        credentialStore.setAutomaticBackupEnabled(enabled)
        mutableStatus.value = NutstoreBackupStatus(
            message = if (enabled) "已开启进入应用时自动备份" else "已关闭自动备份",
        )
    }

    suspend fun manualBackup(): Unit = runOperation(
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
            val uploadedFile = NutstoreBackupFiles.manualName(timestamp)
            client.upload(
                uploadedFile,
                backupCodec.encode(snapshot.transactions, snapshot.recurringRules),
            )
            credentialStore.markBackupSucceeded(fingerprint, timestamp)
            addUploadedFileToLoadedCatalog(uploadedFile)
        }
    }

    suspend fun refreshBackupFiles(): Unit = runOperation(
        runningMessage = "正在读取云端备份…",
    ) {
        val credentials = requireCredentials()
        val files = withContext(ioDispatcher) {
            val client = clientFactory(credentials)
            client.ensureBackupDirectory()
            NutstoreBackupFiles.managedFiles(client.listBackupFiles())
        }
        mutableBackupCatalog.value = NutstoreBackupCatalog(files = files, hasLoaded = true)
    }

    suspend fun restoreLatestBackup(): Unit = runOperation(
        runningMessage = "正在下载最近备份…",
    ) {
        val credentials = requireCredentials()
        val restored = withContext(ioDispatcher) {
            val client = clientFactory(credentials)
            client.ensureBackupDirectory()
            val latestFile = NutstoreBackupFiles.mostRecent(client.listBackupFiles())
                ?: throw IllegalStateException("坚果云 backup 文件夹中还没有可恢复的备份")
            val backup = backupCodec.decodeBackup(client.download(latestFile))
            val result = backupStore.import(backup)
            latestFile to result
        }
        val (latestFile, result) = restored
        publishRestoreResult(latestFile, result)
    }

    suspend fun restoreBackup(fileName: String): Unit = runOperation(
        runningMessage = "正在下载所选备份…",
    ) {
        val managedFile = NutstoreBackupFiles.fromName(fileName)
            ?: throw IllegalArgumentException("只能恢复本应用创建的云端备份")
        val credentials = requireCredentials()
        val restored = withContext(ioDispatcher) {
            val client = clientFactory(credentials)
            client.ensureBackupDirectory()
            val backup = backupCodec.decodeBackup(client.download(managedFile.fileName))
            val result = backupStore.import(backup)
            managedFile.fileName to result
        }
        publishRestoreResult(restored.first, restored.second)
    }

    suspend fun deleteBackup(fileName: String): Unit = runOperation(
        runningMessage = "正在删除云端备份…",
        successMessage = "所选云端备份已删除",
    ) {
        val managedFile = NutstoreBackupFiles.fromName(fileName)
            ?: throw IllegalArgumentException("只能删除本应用创建的云端备份")
        val credentials = requireCredentials()
        withContext(ioDispatcher) {
            val client = clientFactory(credentials)
            client.ensureBackupDirectory()
            client.delete(managedFile.fileName)
        }
        val current = mutableBackupCatalog.value
        mutableBackupCatalog.value = current.copy(
            files = current.files.filterNot { it.fileName == managedFile.fileName },
            hasLoaded = true,
        )
    }

    suspend fun automaticBackupIfChanged() {
        val settings = credentialStore.settings.value
        if (!settings.automaticBackupEnabled) return
        val credentials = credentialStore.credentials() ?: run {
            if (settings.hasCredentials) {
                mutableStatus.value = NutstoreBackupStatus(
                    message = "无法读取已保存的密码，请在设置中重新连接坚果云",
                    isError = true,
                )
            }
            return
        }

        runOperation(
            runningMessage = "正在检查自动备份…",
        ) {
            withContext(ioDispatcher) {
                val snapshot = backupStore.snapshot()
                if (!snapshot.hasBackupContent) {
                    mutableStatus.value = NutstoreBackupStatus(message = "账本为空，未创建自动备份")
                    return@withContext
                }
                val fingerprint = BackupFingerprint.calculate(snapshot)
                if (fingerprint == credentialStore.lastFingerprint()) {
                    mutableStatus.value = NutstoreBackupStatus(message = "账本没有变化，无需重复备份")
                    return@withContext
                }

                val timestamp = clock()
                val client = clientFactory(credentials)
                client.ensureBackupDirectory()
                val uploadedFile = NutstoreBackupFiles.automaticName(timestamp)
                client.upload(
                    uploadedFile,
                    backupCodec.encode(snapshot.transactions, snapshot.recurringRules),
                )
                credentialStore.markBackupSucceeded(fingerprint, timestamp)
                addUploadedFileToLoadedCatalog(uploadedFile)
                val cleanupFailures = mutableListOf<String>()
                val deletedFiles = mutableSetOf<String>()
                try {
                    val obsoleteFiles = NutstoreBackupFiles.filesToDelete(
                        remoteNames = client.listBackupFiles(),
                        protectedNames = setOf(uploadedFile),
                    )
                    obsoleteFiles.forEach { obsoleteFile ->
                        try {
                            client.delete(obsoleteFile)
                            deletedFiles += obsoleteFile
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            cleanupFailures += obsoleteFile
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    cleanupFailures += "旧备份列表"
                }
                removeFilesFromLoadedCatalog(deletedFiles)
                mutableStatus.value = NutstoreBackupStatus(
                    message = if (cleanupFailures.isEmpty()) {
                        "账本有变化，已自动备份到坚果云"
                    } else {
                        "自动备份已上传；部分旧备份暂未清理，下次会继续处理"
                    },
                )
            }
        }
    }

    private fun requireCredentials(): NutstoreCredentials = credentialStore.credentials()
        ?: throw IllegalStateException("请先输入账号和第三方应用密码并完成连接")

    private suspend fun publishRestoreResult(
        fileName: String,
        result: ImportResult,
    ) {
        val maintenanceWarning = try {
            postRestoreMaintenance()
            null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            "；数据已恢复，但周期账单维护未完成，下次进入应用会自动重试"
        }
        mutableStatus.value = NutstoreBackupStatus(
            message = buildString {
                append("已从 ")
                append(fileName)
                append(" 恢复：账目新增 ")
                append(result.insertedCount)
                append(" 条、更新 ")
                append(result.updatedCount)
                append(" 条；周期新增 ")
                append(result.recurringInsertedCount)
                append(" 条、更新 ")
                append(result.recurringUpdatedCount)
                append(" 条")
                append(maintenanceWarning.orEmpty())
            },
            isError = maintenanceWarning != null,
        )
    }

    private fun addUploadedFileToLoadedCatalog(fileName: String) {
        val current = mutableBackupCatalog.value
        if (!current.hasLoaded) return
        mutableBackupCatalog.value = current.copy(
            files = NutstoreBackupFiles.managedFiles(current.files.map { it.fileName } + fileName),
        )
    }

    private fun removeFilesFromLoadedCatalog(fileNames: Set<String>) {
        if (fileNames.isEmpty()) return
        val current = mutableBackupCatalog.value
        if (!current.hasLoaded) return
        mutableBackupCatalog.value = current.copy(
            files = current.files.filterNot { it.fileName in fileNames },
        )
    }

    private suspend fun runOperation(
        runningMessage: String,
        successMessage: String? = null,
        block: suspend () -> Unit,
    ): Unit = operationMutex.withLock {
        mutableStatus.value = NutstoreBackupStatus(isRunning = true, message = runningMessage)
        try {
            block()
            if (successMessage != null) {
                mutableStatus.value = NutstoreBackupStatus(message = successMessage)
            } else if (mutableStatus.value.isRunning) {
                mutableStatus.value = NutstoreBackupStatus()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            mutableStatus.value = NutstoreBackupStatus(
                message = exception.message ?: "坚果云备份失败，请稍后重试",
                isError = true,
            )
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

internal val LedgerBackup.hasBackupContent: Boolean
    get() = transactions.isNotEmpty() || recurringRules.isNotEmpty()

internal object NutstoreBackupFiles {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    private val backupPattern = Regex(
        "^(auto|manual)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3})(?:_e(\\d{1,19}))?\\.json$",
    )

    fun automaticName(epochMs: Long, zoneId: ZoneId = ZoneOffset.UTC): String =
        "auto_${format(epochMs, zoneId)}_e$epochMs.json"

    fun manualName(epochMs: Long, zoneId: ZoneId = ZoneOffset.UTC): String =
        "manual_${format(epochMs, zoneId)}_e$epochMs.json"

    fun filesToDelete(
        remoteNames: List<String>,
        keep: Int = 3,
        protectedNames: Set<String> = emptySet(),
    ): List<String> {
        val automaticFiles = managedFiles(remoteNames)
            .filter { it.kind == NutstoreBackupKind.AUTOMATIC }
        val keepCount = keep.coerceAtLeast(0)
        val retained = linkedSetOf<String>()
        automaticFiles
            .filter { it.fileName in protectedNames }
            .take(keepCount)
            .forEach { retained += it.fileName }
        automaticFiles
            .filterNot { it.fileName in retained }
            .take((keepCount - retained.size).coerceAtLeast(0))
            .forEach { retained += it.fileName }
        return automaticFiles.map { it.fileName }.filterNot { it in retained }
    }

    fun mostRecent(remoteNames: List<String>): String? = managedFiles(remoteNames)
        .firstOrNull()
        ?.fileName

    fun managedFiles(remoteNames: List<String>): List<NutstoreBackupFile> = remoteNames
        .distinct()
        .mapNotNull(::parseManaged)
        .sortedWith { first, second -> compareManaged(second, first) }

    fun fromName(name: String): NutstoreBackupFile? = parseManaged(name)

    private fun format(epochMs: Long, zoneId: ZoneId): String =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).format(formatter)

    private fun parseManaged(name: String): NutstoreBackupFile? {
        val match = backupPattern.matchEntire(name) ?: return null
        return NutstoreBackupFile(
            fileName = name,
            kind = if (match.groupValues[1] == "auto") {
                NutstoreBackupKind.AUTOMATIC
            } else {
                NutstoreBackupKind.MANUAL
            },
            timestampText = match.groupValues[2],
            createdAtEpochMs = match.groupValues.getOrNull(3)
                ?.takeIf(String::isNotEmpty)
                ?.toLongOrNull(),
        )
    }

    private fun compareManaged(first: NutstoreBackupFile, second: NutstoreBackupFile): Int {
        if ((first.createdAtEpochMs != null) != (second.createdAtEpochMs != null)) {
            return if (first.createdAtEpochMs != null) 1 else -1
        }
        val timestampComparison = if (
            first.createdAtEpochMs != null && second.createdAtEpochMs != null
        ) {
            first.createdAtEpochMs.compareTo(second.createdAtEpochMs)
        } else {
            first.timestampText.compareTo(second.timestampText)
        }
        return timestampComparison.takeIf { it != 0 }
            ?: first.fileName.compareTo(second.fileName)
    }
}
