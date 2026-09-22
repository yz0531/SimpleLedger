package com.example.simpleledger.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simpleledger.data.backup.NutstoreBackupManager
import com.example.simpleledger.ui.components.CompactTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutstoreBackupScreen(
    manager: NutstoreBackupManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by manager.settings.collectAsStateWithLifecycle()
    val status by manager.status.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf(settings.username) }
    // Keep credentials out of saved instance state so the plaintext password is never placed in a Bundle.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            CompactTopBar(
                title = "坚果云备份",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                "备份到你的坚果云",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "应用只访问 /SimpleLedger/backup 目录。密码会由 Android Keystore 加密后保存在本机。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("WebDAV 账号", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            label = { Text("坚果云账号 / 邮箱") },
                            singleLine = true,
                            enabled = !status.isRunning,
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            label = { Text("第三方应用密码") },
                            singleLine = true,
                            enabled = !status.isRunning,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                    )
                                }
                            },
                            supportingText = {
                                Text("请使用坚果云“安全选项 → 第三方应用管理”生成的应用密码")
                            },
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    if (manager.connectAndSave(username, password)) password = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            enabled = username.isNotBlank() && password.isNotBlank() && !status.isRunning,
                        ) {
                            Text(if (settings.hasCredentials) "重新连接并保存" else "连接并保存")
                        }
                    }
                }
            }

            if (settings.hasCredentials) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        ),
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("进入应用时自动备份", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "仅在账本内容变化时上传",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = settings.automaticBackupEnabled,
                                    onCheckedChange = manager::setAutomaticBackupEnabled,
                                    enabled = !status.isRunning,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "自动备份使用日期时间命名，只保留最新 3 份；手动备份不计入这 3 份。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = { scope.launch { manager.manualBackup() } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                enabled = !status.isRunning,
                            ) {
                                Icon(Icons.Rounded.Backup, contentDescription = null)
                                Text("立即备份", modifier = Modifier.padding(start = 8.dp))
                            }
                            OutlinedButton(
                                onClick = { showRestoreConfirmation = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                enabled = !status.isRunning,
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null)
                                Text("恢复最新备份", modifier = Modifier.padding(start = 8.dp))
                            }
                            settings.lastBackupAtEpochMs?.let { timestamp ->
                                Text(
                                    "最近成功备份：${formatBackupTime(timestamp)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                    }
                }
            }

            status.message?.let { message ->
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

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text("恢复最近的云端备份？") },
            text = { Text("相同记录会更新，云端存在但本地没有的记录会补充；其他本地记录会保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmation = false
                        scope.launch { manager.restoreLatestBackup() }
                    },
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false }) {
                    Text("取消")
                }
            },
        )
    }
}

private val backupTimeFormatter = DateTimeFormatter.ofPattern(
    "yyyy年M月d日 HH:mm",
    Locale.SIMPLIFIED_CHINESE,
)

private fun formatBackupTime(epochMs: Long): String = rememberlessFormat(epochMs)

private fun rememberlessFormat(epochMs: Long): String = Instant.ofEpochMilli(epochMs)
    .atZone(ZoneId.systemDefault())
    .format(backupTimeFormatter)
