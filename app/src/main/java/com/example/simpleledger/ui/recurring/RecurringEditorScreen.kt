package com.example.simpleledger.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.domain.model.RecurringFrequency
import com.example.simpleledger.domain.model.RecurringRule
import com.example.simpleledger.domain.model.TransactionType
import com.example.simpleledger.domain.recurring.RecurringPostingPlanner
import com.example.simpleledger.domain.repository.RecurringRuleRepository
import com.example.simpleledger.ui.components.CategoryPicker
import com.example.simpleledger.ui.components.CompactTopBar
import com.example.simpleledger.ui.components.amountInput
import com.example.simpleledger.ui.components.formatEditorDay
import com.example.simpleledger.ui.components.parseAmountMinor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private const val MAX_RECURRING_NOTE_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditorScreen(
    repository: RecurringRuleRepository,
    ruleId: String?,
    mode: LedgerMode,
    processDue: suspend () -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = ruleId != null
    val expenseOnly = mode == LedgerMode.EXPENSE_ONLY
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var loadedRule by remember { mutableStateOf<RecurringRule?>(null) }
    var isLoading by remember(ruleId) { mutableStateOf(isEditing) }
    var isSaving by remember { mutableStateOf(false) }
    var amount by rememberSaveable(ruleId) { mutableStateOf("") }
    var typeName by rememberSaveable(ruleId) { mutableStateOf(TransactionType.EXPENSE.name) }
    var categoryId by rememberSaveable(ruleId) {
        mutableStateOf(Categories.expenseOther.id)
    }
    var frequencyName by rememberSaveable(ruleId) { mutableStateOf(RecurringFrequency.MONTHLY.name) }
    var startDate by rememberSaveable(ruleId) { mutableStateOf(LocalDate.now().toString()) }
    var note by rememberSaveable(ruleId) { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    val frequency = RecurringFrequency.valueOf(frequencyName)

    LaunchedEffect(ruleId) {
        if (ruleId == null) return@LaunchedEffect
        isLoading = true
        runCatching { repository.getById(ruleId) }
            .onSuccess { rule ->
                if (rule == null) {
                    snackbarHostState.showSnackbar("这条周期规则不存在或已被删除")
                    onBack()
                } else {
                    loadedRule = rule
                    amount = amountInput(rule.amountMinor)
                    typeName = rule.type.name
                    categoryId = rule.categoryId
                    frequencyName = rule.frequency.name
                    startDate = rule.startDate
                    note = rule.note
                }
            }
            .onFailure {
                snackbarHostState.showSnackbar(it.message ?: "读取周期规则失败")
                onBack()
            }
        isLoading = false
    }

    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = null,
            initialDisplayedMonthMillis = initialMillis,
        )
        LaunchedEffect(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                startDate = Instant.ofEpochMilli(millis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString()
                dateError = null
                showDatePicker = false
            }
        }
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这条周期规则？") },
            text = { Text("已经自动生成的账目会保留，之后不再继续记账。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val id = ruleId ?: return@TextButton
                        scope.launch {
                            isSaving = true
                            runCatching { repository.delete(id) }
                                .onSuccess { onSaved() }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "删除失败") }
                            isSaving = false
                        }
                    },
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopBar(
                title = if (isEditing) "编辑周期账单" else "新建周期账单",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    if (isEditing) {
                        IconButton(enabled = !isSaving, onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "删除周期规则",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)),
            ) {
                if (!expenseOnly) {
                    SectionTitle("类型")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { option ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = type == option,
                                onClick = {
                                    typeName = option.name
                                    if (Categories.find(categoryId)?.type != option) {
                                        categoryId = Categories.forType(option).first().id
                                    }
                                },
                                label = {
                                    Text(
                                        if (option == TransactionType.EXPENSE) "支出" else "收入",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("固定金额", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { value ->
                                if (value.length <= 14 && value.all { it.isDigit() || it == '.' || it == ',' }) {
                                    amount = value
                                    amountError = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            prefix = { Text("¥ ") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            isError = amountError != null,
                            supportingText = amountError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                            ),
                            textStyle = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionTitle("分类")
                CategoryPicker(
                    type = type,
                    selectedId = categoryId,
                    onSelected = { categoryId = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                Spacer(Modifier.height(18.dp))
                SectionTitle("重复频率")
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecurringFrequency.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowItems.forEach { option ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = frequency == option,
                                    onClick = { frequencyName = option.name },
                                    label = {
                                        Text(
                                            option.displayName(),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    text = frequency.scheduleDescription(startDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(18.dp))
                SectionTitle("首次执行")
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = { showDatePicker = true },
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Text(formatEditorDay(startDate), modifier = Modifier.padding(start = 8.dp))
                }
                if (dateError != null) {
                    Text(
                        dateError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= MAX_RECURRING_NOTE_LENGTH) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称或备注（可选）") },
                    placeholder = { Text("例如：房租、视频会员") },
                    supportingText = { Text("${note.length}/$MAX_RECURRING_NOTE_LENGTH") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                Spacer(Modifier.height(22.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isSaving,
                    onClick = {
                        val amountMinor = parseAmountMinor(amount)
                        if (amountMinor == null) {
                            amountError = "请输入大于 0、最多两位小数的金额"
                            return@Button
                        }
                        val start = LocalDate.parse(startDate)
                        val original = loadedRule
                        if ((original == null || startDate != original.startDate) && start.isBefore(LocalDate.now())) {
                            dateError = "首次执行日期请选择今天或未来"
                            return@Button
                        }
                        scope.launch {
                            isSaving = true
                            val now = System.currentTimeMillis().coerceAtLeast(original?.createdAtEpochMs ?: 0L)
                            val scheduleChanged = original == null ||
                                original.startDate != startDate ||
                                original.frequency != frequency
                            val nextDate = if (scheduleChanged) {
                                RecurringPostingPlanner.firstOccurrenceAfter(
                                    currentNextExecutionDate = start,
                                    afterDate = LocalDate.now().minusDays(1),
                                    frequency = frequency,
                                    anchorStartDate = start,
                                ).toString()
                            } else {
                                original.nextExecutionDate
                            }
                            val rule = RecurringRule(
                                id = original?.id ?: UUID.randomUUID().toString(),
                                type = if (expenseOnly) TransactionType.EXPENSE else type,
                                amountMinor = amountMinor,
                                categoryId = categoryId,
                                note = note.trim(),
                                frequency = frequency,
                                startDate = startDate,
                                nextExecutionDate = nextDate,
                                isEnabled = original?.isEnabled ?: true,
                                createdAtEpochMs = original?.createdAtEpochMs ?: now,
                                updatedAtEpochMs = now,
                            )
                            runCatching {
                                repository.upsert(rule)
                                processDue()
                            }.onSuccess {
                                onSaved()
                            }.onFailure {
                                snackbarHostState.showSnackbar(it.message ?: "保存周期规则失败")
                            }
                            isSaving = false
                        }
                    },
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (isEditing) "保存修改" else "创建周期账单")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
}

private fun RecurringFrequency.scheduleDescription(startDate: String): String {
    val date = LocalDate.parse(startDate)
    return when (this) {
        RecurringFrequency.DAILY -> "从所选日期起，每个自然日记一笔"
        RecurringFrequency.WEEKLY -> {
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
            "从所选日期起，每周${weekday}记一笔"
        }
        RecurringFrequency.BIWEEKLY -> {
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
            "从所选日期起，每两周的${weekday}记一笔"
        }
        RecurringFrequency.MONTHLY ->
            "每个自然月的 ${date.dayOfMonth} 日记一笔；短月自动取月末"
    }
}
