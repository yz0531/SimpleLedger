package com.example.simpleledger.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.LedgerMode

@Composable
fun SettingsScreen(
    mode: LedgerMode,
    onModeChanged: (LedgerMode) -> Unit,
    onSkinPicker: () -> Unit,
    onTransfer: () -> Unit,
    onNutstoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 6.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeading(title = "记账方式")
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
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
                }
            }
        }
        item {
            SectionHeading(
                title = "个性化",
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        item {
            SettingsEntry(
                icon = Icons.Rounded.Palette,
                title = "外观皮肤",
                onClick = onSkinPicker,
            )
        }
        item {
            SectionHeading(
                title = "数据与备份",
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsEntry(
                    icon = Icons.Rounded.Inventory2,
                    title = "导入与导出",
                    onClick = onTransfer,
                )
                SettingsEntry(
                    icon = Icons.Rounded.CloudUpload,
                    title = "坚果云备份",
                    onClick = onNutstoreBackup,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
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
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurface,
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
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}
