package com.example.simpleledger.ui.editor

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
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.TransactionType
import com.example.simpleledger.domain.repository.LedgerRepository
import com.example.simpleledger.ui.components.amountInput
import com.example.simpleledger.ui.components.CategoryPicker
import com.example.simpleledger.ui.components.CompactTopBar
import com.example.simpleledger.ui.components.formatEditorDay
import com.example.simpleledger.ui.components.parseAmountMinor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.launch

private const val MAX_NOTE_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    repository: LedgerRepository,
    transactionId: String?,
    mode: LedgerMode,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = transactionId != null
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var loadedTransaction by remember { mutableStateOf<LedgerTransaction?>(null) }
    var isLoading by remember(transactionId) { mutableStateOf(isEditing) }
    var isSaving by remember { mutableStateOf(false) }
    var amount by rememberSaveable(transactionId) { mutableStateOf("") }
    var typeName by rememberSaveable(transactionId) { mutableStateOf(TransactionType.EXPENSE.name) }
    var categoryId by rememberSaveable(transactionId) {
        mutableStateOf(Categories.forType(TransactionType.EXPENSE).firstOrNull()?.id.orEmpty())
    }
    var occurredOn by rememberSaveable(transactionId) { mutableStateOf(LocalDate.now().toString()) }
    var note by rememberSaveable(transactionId) { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    val expenseOnly = mode == LedgerMode.EXPENSE_ONLY

    LaunchedEffect(transactionId) {
        if (transactionId == null) return@LaunchedEffect
        isLoading = true
        runCatching { repository.getById(transactionId) }
            .onSuccess { transaction ->
                if (transaction == null) {
                    snackbarHostState.showSnackbar("这笔账目不存在或已被删除")
                    onBack()
                } else {
                    loadedTransaction = transaction
                    amount = amountInput(transaction.amountMinor)
                    typeName = transaction.type.name
                    categoryId = transaction.categoryId
                    occurredOn = transaction.occurredOn
                    note = transaction.note
                }
            }
            .onFailure {
                snackbarHostState.showSnackbar(it.message ?: "读取账目失败")
                onBack()
            }
        isLoading = false
    }

    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(occurredOn).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = null,
            initialDisplayedMonthMillis = initialMillis,
        )
        LaunchedEffect(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                occurredOn = Instant.ofEpochMilli(millis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString()
                showDatePicker = false
            }
        }
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这笔账目？") },
            text = { Text("删除后无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val id = transactionId ?: return@TextButton
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
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CompactTopBar(
                title = if (isEditing) "编辑账目" else if (expenseOnly) "记一笔花销" else "记一笔",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    if (isEditing) {
                        IconButton(
                            enabled = !isSaving,
                            onClick = { showDeleteDialog = true },
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "删除账目",
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
            ) {
                CircularProgressIndicator()
            }
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
                    Text("类型", style = MaterialTheme.typography.titleMedium)
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
                                        categoryId = Categories.forType(option).firstOrNull()?.id.orEmpty()
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (option == TransactionType.EXPENSE) "支出" else "收入",
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
                        Text(
                            text = if (type == TransactionType.EXPENSE) "花了多少" else "收入多少",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                Text("分类", style = MaterialTheme.typography.titleMedium)
                CategoryPicker(
                    type = type,
                    selectedId = categoryId,
                    onSelected = { categoryId = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                Spacer(Modifier.height(18.dp))
                Text("日期", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = { showDatePicker = true },
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Text(
                        text = formatEditorDay(occurredOn),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= MAX_NOTE_LENGTH) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    placeholder = { Text("写点什么…") },
                    supportingText = { Text("${note.length}/$MAX_NOTE_LENGTH") },
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
                        if (categoryId.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("请选择分类") }
                            return@Button
                        }
                        scope.launch {
                            isSaving = true
                            val original = loadedTransaction
                            val now = System.currentTimeMillis()
                                .coerceAtLeast(original?.createdAtEpochMs ?: 0L)
                            val transaction = LedgerTransaction(
                                id = original?.id ?: UUID.randomUUID().toString(),
                                type = type,
                                amountMinor = amountMinor,
                                categoryId = categoryId,
                                occurredOn = occurredOn,
                                note = note.trim(),
                                createdAtEpochMs = original?.createdAtEpochMs ?: now,
                                updatedAtEpochMs = now,
                            )
                            runCatching { repository.upsert(transaction) }
                                .onSuccess { onSaved() }
                                .onFailure { snackbarHostState.showSnackbar(it.message ?: "保存失败") }
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
                        Text(if (isEditing) "保存修改" else if (expenseOnly) "保存花销" else "保存账目")
                    }
                }
            }
        }
    }
}
