package com.example.simpleledger.ui.transfer

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simpleledger.data.transfer.BackupValidationException
import com.example.simpleledger.data.transfer.DataTransferManager
import com.example.simpleledger.ui.components.CompactTopBar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TRANSFER_LOG_TAG = "TransferScreen"

private enum class TransferOperation {
    EXPORT_JSON,
    EXPORT_CSV,
    IMPORT_JSON,
    CLEAR_ALL,
}

@Composable
fun TransferScreen(
    transferManager: DataTransferManager,
    onImportCompleted: suspend () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var operation by remember { mutableStateOf<TransferOperation?>(null) }
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    fun failureMessage(throwable: Throwable, fallback: String): String = when (throwable) {
        is BackupValidationException -> throwable.message ?: "备份文件格式不正确"
        else -> throwable.message ?: fallback
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            operation = TransferOperation.EXPORT_JSON
            val outcome = runSuspendCatching { transferManager.exportJson(uri) }
            operation = null
            outcome.fold(
                onSuccess = { result ->
                    snackbarHostState.showSnackbar(
                        "已备份 ${result.exportedCount} 笔账目和 ${result.recurringRuleCount} 条周期规则",
                    )
                },
                onFailure = { snackbarHostState.showSnackbar(failureMessage(it, "导出失败")) },
            )
        }
    }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            operation = TransferOperation.EXPORT_CSV
            val outcome = runSuspendCatching { transferManager.exportCsv(uri) }
            operation = null
            outcome.fold(
                onSuccess = { result -> snackbarHostState.showSnackbar("已导出 ${result.exportedCount} 笔账目") },
                onFailure = { snackbarHostState.showSnackbar(failureMessage(it, "导出失败")) },
            )
        }
    }
    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingImportUri = uri
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入这份备份？") },
            text = { Text("账目和周期规则会合并到当前账本；ID 相同的内容将使用备份更新。导入过程会整体完成或整体回滚。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImportUri ?: return@TextButton
                        pendingImportUri = null
                        scope.launch {
                            operation = TransferOperation.IMPORT_JSON
                            val outcome = runSuspendCatching { transferManager.importJson(uri) }
                            val postImportFailure = outcome.getOrNull()?.let {
                                runSuspendCatching { onImportCompleted() }.exceptionOrNull()
                            }
                            operation = null
                            outcome.fold(
                                onSuccess = { result ->
                                    if (postImportFailure != null) {
                                        Log.w(
                                            TRANSFER_LOG_TAG,
                                            "Import succeeded but post-import recurring processing failed",
                                            postImportFailure,
                                        )
                                    }
                                    val followUpMessage = if (postImportFailure == null) {
                                        ""
                                    } else {
                                        "；数据已保存，但周期账单补记暂未完成"
                                    }
                                    snackbarHostState.showSnackbar(
                                        "导入完成：账目新增 ${result.insertedCount}、更新 ${result.updatedCount}；" +
                                            "周期新增 ${result.recurringInsertedCount}、更新 ${result.recurringUpdatedCount}" +
                                            followUpMessage,
                                    )
                                },
                                onFailure = { snackbarHostState.showSnackbar(failureMessage(it, "导入失败")) },
                            )
                        }
                    },
                ) { Text("开始导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("取消") }
            },
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("清空本机全部数据？") },
            text = {
                Text(
                    "将永久删除本机的全部账目和周期规则，且无法撤销。" +
                        "本次操作不会连接坚果云、上传空账本或删除任何备份文件，建议先完成备份。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        scope.launch {
                            operation = TransferOperation.CLEAR_ALL
                            val outcome = runSuspendCatching { transferManager.clearLocalData() }
                            operation = null
                            outcome.fold(
                                onSuccess = { result ->
                                    val deletedCount = result.deletedTransactionCount +
                                        result.deletedRecurringRuleCount
                                    snackbarHostState.showSnackbar(
                                        if (deletedCount == 0) {
                                            "账本已经是空的"
                                        } else {
                                            "已清空 ${result.deletedTransactionCount} 笔账目和 " +
                                                "${result.deletedRecurringRuleCount} 条周期规则"
                                        },
                                    )
                                },
                                onFailure = { snackbarHostState.showSnackbar(failureMessage(it, "清空失败")) },
                            )
                        }
                    },
                    enabled = operation == null,
                ) {
                    Text("确认清空本机", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopBar(
                title = "数据管理",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = "备份与迁移",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "文件由你选择保存位置，应用无需读取整个存储空间。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                item {
                    TransferCard(
                        icon = Icons.Rounded.Backup,
                        title = "完整备份",
                        description = "导出 JSON 文件，可恢复全部账目字段和周期记账规则。",
                    ) {
                        Button(
                            enabled = operation == null,
                            onClick = { jsonExportLauncher.launch(defaultFileName("json")) },
                        ) { Text("导出备份") }
                    }
                }
                item {
                    TransferCard(
                        icon = Icons.Rounded.TableChart,
                        title = "导出表格",
                        description = "导出 UTF-8 CSV 文件，方便使用 Excel 或其他表格工具查看。",
                    ) {
                        OutlinedButton(
                            enabled = operation == null,
                            onClick = { csvExportLauncher.launch(defaultFileName("csv")) },
                        ) { Text("导出 CSV") }
                    }
                }
                item {
                    TransferCard(
                        icon = Icons.Rounded.Restore,
                        title = "恢复备份",
                        description = "选择由简账导出的 JSON 文件。导入前会检查格式与数据完整性。",
                    ) {
                        OutlinedButton(
                            enabled = operation == null,
                            onClick = {
                                jsonImportLauncher.launch(
                                    arrayOf("application/json", "text/plain", "application/octet-stream"),
                                )
                            },
                        ) { Text("选择备份文件") }
                    }
                }
                item {
                    DangerTransferCard(
                        onClear = { showClearConfirmation = true },
                        enabled = operation == null,
                    )
                }
                item {
                    Text(
                        text = "提示：建议定期保存完整备份。CSV 适合查看和分析，不能用于恢复账本。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }

            if (operation != null) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = when (operation) {
                                TransferOperation.IMPORT_JSON -> "正在导入…"
                                TransferOperation.CLEAR_ALL -> "正在清空…"
                                else -> "正在导出…"
                            },
                            modifier = Modifier.padding(start = 14.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DangerTransferCard(
    onClear: () -> Unit,
    enabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.errorContainer.copy(alpha = 0.58f),
            contentColor = colors.onSurface,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.errorContainer,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = colors.onErrorContainer,
                    )
                }
                Text(
                    text = "清空本机数据",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onErrorContainer,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = "永久删除本机全部账目和周期规则，不上传空账本，也不删除备份文件。",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onErrorContainer,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
            )
            OutlinedButton(
                onClick = onClear,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                Text("清空本机数据", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun TransferCard(
    icon: ImageVector,
    title: String,
    description: String,
    action: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
            )
            action()
        }
    }
}

private fun defaultFileName(extension: String): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
    return "简账_$timestamp.$extension"
}

private suspend fun <T> runSuspendCatching(
    block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    Result.failure(exception)
}
