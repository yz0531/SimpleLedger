package com.example.simpleledger.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.LedgerAppearance
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.ui.components.CompactTopBar

@Composable
fun SettingsScreen(
    mode: LedgerMode,
    appearance: LedgerAppearance,
    onModeChanged: (LedgerMode) -> Unit,
    onSkinPicker: () -> Unit,
    onTransfer: () -> Unit,
    onNutstoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { CompactTopBar(title = "设置") },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeading(
                    title = "记账方式",
                    subtitle = "按你的习惯精简首页和记账流程",
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModeChip(
                                label = "只记支出",
                                icon = Icons.Rounded.Payments,
                                selected = mode == LedgerMode.EXPENSE_ONLY,
                                onClick = { onModeChanged(LedgerMode.EXPENSE_ONLY) },
                                modifier = Modifier.weight(1f),
                            )
                            ModeChip(
                                label = "收入与支出",
                                icon = Icons.Rounded.Savings,
                                selected = mode == LedgerMode.INCOME_AND_EXPENSE,
                                onClick = { onModeChanged(LedgerMode.INCOME_AND_EXPENSE) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = if (mode == LedgerMode.EXPENSE_ONLY) {
                                "首页、记账和周期账单只显示支出；已有收入记录会保留。"
                            } else {
                                "同时记录收入与支出，并显示月度结余。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            item {
                SectionHeading(
                    title = "个性化",
                    subtitle = "让图片背景更贴合你的使用心情",
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            item {
                SettingsEntry(
                    icon = Icons.Rounded.Palette,
                    title = "外观皮肤",
                    subtitle = "${appearance.skin.displayName} · ${appearance.color.displayName} · " +
                        if (appearance.skin.hasImage) "透明度 ${(appearance.imageOpacity * 100).toInt()}%" else "纯色背景",
                    onClick = onSkinPicker,
                )
            }
            item {
                SectionHeading(
                    title = "数据与备份",
                    subtitle = "导出迁移，或同步到你的坚果云",
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsEntry(
                        icon = Icons.Rounded.Inventory2,
                        title = "导入与导出",
                        subtitle = "备份、恢复或导出 CSV 表格",
                        onClick = onTransfer,
                    )
                    SettingsEntry(
                        icon = Icons.Rounded.CloudUpload,
                        title = "坚果云备份",
                        subtitle = "WebDAV 在线备份，自动保留最近 3 份",
                        onClick = onNutstoreBackup,
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("本地优先，云端可选", fontWeight = FontWeight.SemiBold)
                            Text(
                                "账目默认保存在设备中；坚果云凭据加密保存在本机。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null) },
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        },
    )
}

@Composable
private fun SettingsEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}
