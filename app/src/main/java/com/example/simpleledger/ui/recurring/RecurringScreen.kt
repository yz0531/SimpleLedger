package com.example.simpleledger.ui.recurring

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import com.example.simpleledger.domain.repository.RecurringRuleRepository
import com.example.simpleledger.ui.components.categoryIcon
import com.example.simpleledger.ui.components.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    repository: RecurringRuleRepository,
    mode: LedgerMode,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allRules by repository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val rules = remember(allRules, mode) {
        if (mode == LedgerMode.EXPENSE_ONLY) {
            allRules.filter { it.type == TransactionType.EXPENSE }
        } else {
            allRules
        }
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("周期记账")
                        Text(
                            "固定开销，按时自动记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("新建周期") },
                shape = RoundedCornerShape(20.dp),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "到期后会在后台记账；若系统延迟任务，下次打开应用会自动补齐。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            if (rules.isEmpty()) {
                item { EmptyRecurringRules() }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("我的周期", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${rules.count { it.isEnabled }} 个启用",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(rules, key = RecurringRule::id) { rule ->
                    RecurringRuleCard(
                        rule = rule,
                        onClick = { onEdit(rule.id) },
                        onEnabledChange = { enabled ->
                            scope.launch {
                                runCatching { repository.setEnabled(rule.id, enabled) }
                                    .onFailure {
                                        snackbarHostState.showSnackbar(it.message ?: "更新周期状态失败")
                                    }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringRuleCard(
    rule: RecurringRule,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val category = Categories.find(rule.categoryId)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (rule.type == TransactionType.EXPENSE) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        categoryIcon(rule.categoryId),
                        contentDescription = category?.label,
                        tint = if (rule.type == TransactionType.EXPENSE) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = rule.note.ifBlank { category?.label ?: "周期账目" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${rule.frequency.displayName()} · ${nextDateLabel(rule.nextExecutionDate, rule.isEnabled)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = formatMoney(rule.amountMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.type == TransactionType.EXPENSE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun EmptyRecurringRules() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(78.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.EventRepeat,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            "还没有周期账单",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "房租、会员费、通勤等固定开销可以自动记账",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

fun RecurringFrequency.displayName(): String = when (this) {
    RecurringFrequency.DAILY -> "每天"
    RecurringFrequency.WEEKLY -> "每周"
    RecurringFrequency.BIWEEKLY -> "每两周"
    RecurringFrequency.MONTHLY -> "每月"
}

private fun nextDateLabel(value: String, enabled: Boolean): String {
    if (!enabled) return "已暂停"
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    return "下次 ${date.format(DateTimeFormatter.ofPattern("M月d日"))}"
}
