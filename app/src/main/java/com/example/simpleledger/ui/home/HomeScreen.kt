package com.example.simpleledger.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.TransactionType
import com.example.simpleledger.ui.components.MonthSummaryCard
import com.example.simpleledger.ui.components.TransactionRow
import com.example.simpleledger.ui.components.formatDay
import com.example.simpleledger.ui.components.formatMonth
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private const val MONTH_PAGE_COUNT = 2401
private const val CURRENT_MONTH_PAGE = MONTH_PAGE_COUNT / 2

private enum class LedgerFilter(val label: String) {
    ALL("全部"),
    EXPENSE("支出"),
    INCOME("收入"),
}

@Composable
fun HomeScreen(
    transactions: List<LedgerTransaction>,
    mode: LedgerMode,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(
        initialPage = CURRENT_MONTH_PAGE,
        pageCount = { MONTH_PAGE_COUNT },
    )
    val scope = rememberCoroutineScope()
    var filterName by rememberSaveable { mutableStateOf(LedgerFilter.ALL.name) }
    var monthPickerTarget by remember { mutableStateOf<YearMonth?>(null) }
    val filter = remember(filterName) { LedgerFilter.valueOf(filterName) }
    val expenseOnly = mode == LedgerMode.EXPENSE_ONLY

    LaunchedEffect(expenseOnly) {
        if (expenseOnly) filterName = LedgerFilter.ALL.name
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("记一笔") },
                shape = RoundedCornerShape(20.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> page },
            ) { page ->
                val month = baseMonth.plusMonths((page - CURRENT_MONTH_PAGE).toLong())
                MonthLedgerPage(
                    month = month,
                    transactions = transactions,
                    expenseOnly = expenseOnly,
                    filter = filter,
                    onFilterChanged = { filterName = it.name },
                    onEdit = onEdit,
                    canGoPrevious = page > 0,
                    canGoNext = page < MONTH_PAGE_COUNT - 1,
                    onPreviousMonth = {
                        scope.launch {
                            pagerState.animateScrollToPage(page - 1, animationSpec = tween(210))
                        }
                    },
                    onNextMonth = {
                        scope.launch {
                            pagerState.animateScrollToPage(page + 1, animationSpec = tween(210))
                        }
                    },
                    onChooseMonth = { monthPickerTarget = month },
                )
            }
        }
    }

    monthPickerTarget?.let { initialMonth ->
        MonthPickerDialog(
            initialMonth = initialMonth,
            currentMonth = baseMonth,
            onDismiss = { monthPickerTarget = null },
            onMonthSelected = { chosenMonth ->
                val monthOffset = ChronoUnit.MONTHS.between(baseMonth, chosenMonth).toInt()
                val targetPage = CURRENT_MONTH_PAGE + monthOffset
                monthPickerTarget = null
                if (targetPage in 0 until MONTH_PAGE_COUNT) {
                    scope.launch {
                        pagerState.animateScrollToPage(targetPage, animationSpec = tween(210))
                    }
                }
            },
        )
    }
}

@Composable
private fun MonthLedgerPage(
    month: YearMonth,
    transactions: List<LedgerTransaction>,
    expenseOnly: Boolean,
    filter: LedgerFilter,
    onFilterChanged: (LedgerFilter) -> Unit,
    onEdit: (String) -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onChooseMonth: () -> Unit,
) {
    val allMonthTransactions = remember(transactions, month) {
        transactions
            .filter { it.occurredOn.startsWith(month.toString()) }
            .sortedWith(
                compareByDescending<LedgerTransaction> { it.occurredOn }
                    .thenByDescending { it.createdAtEpochMs },
            )
    }
    val modeTransactions = remember(allMonthTransactions, expenseOnly) {
        if (expenseOnly) {
            allMonthTransactions.filter { it.type == TransactionType.EXPENSE }
        } else {
            allMonthTransactions
        }
    }
    val visibleTransactions = remember(modeTransactions, filter, expenseOnly) {
        if (expenseOnly) {
            modeTransactions
        } else {
            when (filter) {
                LedgerFilter.ALL -> modeTransactions
                LedgerFilter.EXPENSE -> modeTransactions.filter { it.type == TransactionType.EXPENSE }
                LedgerFilter.INCOME -> modeTransactions.filter { it.type == TransactionType.INCOME }
            }
        }
    }
    val incomeMinor = remember(allMonthTransactions) {
        allMonthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    }
    val expenseMinor = remember(allMonthTransactions) {
        allMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            MonthSummaryCard(
                monthLabel = formatMonth(month),
                balanceMinor = incomeMinor - expenseMinor,
                incomeMinor = incomeMinor,
                expenseMinor = expenseMinor,
                expenseOnly = expenseOnly,
                canGoPrevious = canGoPrevious,
                canGoNext = canGoNext,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onChooseMonth = onChooseMonth,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )
        }
        if (!expenseOnly) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LedgerFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { onFilterChanged(option) },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = if (expenseOnly) "本月支出" else "本月明细",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (visibleTransactions.isEmpty()) {
            item {
                EmptyLedger(
                    hasMonthTransactions = modeTransactions.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 42.dp),
                )
            }
        } else {
            visibleTransactions.groupBy { it.occurredOn }.forEach { (date, dayTransactions) ->
                item(key = "date:$date") {
                    Text(
                        text = formatDay(date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
                items(dayTransactions, key = { "transaction:${it.id}" }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        expenseOnly = expenseOnly,
                        onClick = { onEdit(transaction.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPickerDialog(
    initialMonth: YearMonth,
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
) {
    var displayedYear by remember(initialMonth) { mutableIntStateOf(initialMonth.year) }
    val minimumYear = currentMonth.year - 100
    val maximumYear = currentMonth.year + 100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择年月", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { displayedYear -= 1 },
                        enabled = displayedYear > minimumYear,
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "上一年")
                    }
                    Text(
                        text = "${displayedYear}年",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { displayedYear += 1 },
                        enabled = displayedYear < maximumYear,
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "下一年")
                    }
                }

                (1..12).chunked(3).forEach { monthRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        monthRow.forEach { monthNumber ->
                            val selected = displayedYear == initialMonth.year &&
                                monthNumber == initialMonth.monthValue
                            Surface(
                                onClick = {
                                    onMonthSelected(YearMonth.of(displayedYear, monthNumber))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ) {
                                Text(
                                    text = "${monthNumber}月",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onMonthSelected(currentMonth)
            }) {
                Text("回到本月")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EmptyLedger(
    hasMonthTransactions: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = if (hasMonthTransactions) "当前筛选下没有记录" else "这个月还没有账目",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (hasMonthTransactions) "换个筛选条件看看" else "点一下“记一笔”，从今天开始",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
