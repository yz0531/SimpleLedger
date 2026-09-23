package com.example.simpleledger.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simpleledger.data.backup.NutstoreBackupFile
import com.example.simpleledger.data.backup.NutstoreBackupKind
import com.example.simpleledger.data.backup.NutstoreBackupManager
import com.example.simpleledger.ui.components.CompactTopBar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CloudBackupManagementScreen(
    manager: NutstoreBackupManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog by manager.backupCatalog.collectAsStateWithLifecycle()
    val status by manager.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var restoreTarget by remember { mutableStateOf<NutstoreBackupFile?>(null) }
    var deleteTarget by remember { mutableStateOf<NutstoreBackupFile?>(null) }

    LaunchedEffect(manager) {
        manager.refreshBackupFiles()
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CompactTopBar(
                title = "云端备份管理",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    IconButton(
                        onClick = { scope.launch { manager.refreshBackupFiles() } },
                        enabled = !status.isRunning,
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新云端备份")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("只管理简账备份", fontWeight = FontWeight.SemiBold)
                            Text(
                                "仅显示 /SimpleLedger/backup 中符合本应用命名规则的备份，其他文件不会被下载、展示或删除。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (!catalog.hasLoaded && status.isRunning) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("正在读取备份列表…", modifier = Modifier.padding(start = 12.dp))
                    }
                }
            } else if (catalog.hasLoaded && catalog.files.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null)
                            Text(
                                "还没有可管理的云端备份",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Text(
                                "返回上一页创建一次手动备份即可在这里查看。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            items(catalog.files, key = NutstoreBackupFile::fileName) { backup ->
                CloudBackupCard(
                    backup = backup,
                    enabled = !status.isRunning,
                    onRestore = { restoreTarget = backup },
                    onDelete = { deleteTarget = backup },
                )
            }

            status.message?.takeIf { catalog.hasLoaded || !status.isRunning }?.let { message ->
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (status.isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (status.isRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            } else {
                                Icon(
                                    if (status.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                )
                            }
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    restoreTarget?.let { backup ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text("恢复这份云端备份？") },
            text = {
                Text("将恢复 ${formatCloudBackupTime(backup)} 的备份。相同记录会更新，其他本地记录会保留。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreTarget = null
                        scope.launch { manager.restoreBackup(backup.fileName) }
                    },
                ) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text("取消") }
            },
        )
    }

    deleteTarget?.let { backup ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这份云端备份？") },
            text = { Text("只会删除所选的坚果云备份，不会清除手机中的账目。此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        scope.launch { manager.deleteBackup(backup.fileName) }
                    },
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CloudBackupCard(
    backup: NutstoreBackupFile,
    enabled: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = if (backup.kind == NutstoreBackupKind.AUTOMATIC) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
            ) {
                Icon(
                    imageVector = if (backup.kind == NutstoreBackupKind.AUTOMATIC) {
                        Icons.Rounded.CloudQueue
                    } else {
                        Icons.Rounded.Backup
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    if (backup.kind == NutstoreBackupKind.AUTOMATIC) "自动备份" else "手动备份",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatCloudBackupTime(backup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    backup.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRestore, enabled = enabled) {
                Icon(Icons.Rounded.Restore, contentDescription = "恢复这份备份")
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "删除这份备份",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private val cloudBackupTimeFormatter = DateTimeFormatter.ofPattern(
    "yyyy年M月d日 HH:mm:ss",
    Locale.SIMPLIFIED_CHINESE,
)
private val cloudBackupFileTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")

private fun formatCloudBackupTime(backup: NutstoreBackupFile): String {
    backup.createdAtEpochMs?.let { epochMs ->
        return Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .format(cloudBackupTimeFormatter)
    }
    return runCatching {
        LocalDateTime.parse(backup.timestampText, cloudBackupFileTimeFormatter)
            .format(cloudBackupTimeFormatter)
    }.getOrDefault(backup.timestampText)
}
