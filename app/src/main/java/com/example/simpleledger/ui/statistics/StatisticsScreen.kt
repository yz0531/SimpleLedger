package com.example.simpleledger.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.MonthlyExpense
import com.example.simpleledger.domain.model.YearlyExpenseStatistics
import com.example.simpleledger.ui.components.formatMoney
import com.example.simpleledger.ui.components.CompactTopBar
import java.time.Year

@Composable
fun StatisticsScreen(
    transactions: List<LedgerTransaction>,
    modifier: Modifier = Modifier,
    initialYear: Int = Year.now().value,
) {
    var selectedYear by rememberSaveable { mutableIntStateOf(initialYear) }
    val statistics = remember(transactions, selectedYear) {
        YearlyExpenseStatistics.from(transactions, selectedYear)
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            CompactTopBar(
                title = "年度统计",
                subtitle = "看清每个月的花销变化",
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                YearPicker(
                    year = selectedYear,
                    onPrevious = { selectedYear -= 1 },
                    onNext = { selectedYear += 1 },
                )
            }

            if (statistics.isEmpty) {
                item { EmptyStatistics(year = selectedYear) }
            } else {
                item { AnnualSummaryCard(statistics = statistics) }
                item { MonthlyExpenseChart(statistics = statistics) }
            }
        }
    }
}

@Composable
private fun YearPicker(
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrevious) { Text("‹  上一年") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$year 年",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "全年支出概览",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onNext) { Text("下一年  ›") }
    }
}

@Composable
private fun AnnualSummaryCard(statistics: YearlyExpenseStatistics) {
    val colors = MaterialTheme.colorScheme
    val highestMonth = requireNotNull(statistics.highestExpenseMonth)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(colors.primary, colors.tertiary),
                    ),
                )
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = "年度总支出",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimary.copy(alpha = 0.76f),
            )
            Text(
                text = formatMoney(statistics.totalExpenseMinor),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "月均支出",
                    value = formatMoney(statistics.averageMonthlyExpenseMinor),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "最高月份",
                    value = "${highestMonth.month}月 · ${formatMoney(highestMonth.amountMinor)}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthlyExpenseChart(statistics: YearlyExpenseStatistics) {
    val colors = MaterialTheme.colorScheme
    val highestMonth = requireNotNull(statistics.highestExpenseMonth)
    val description = remember(statistics) {
        buildString {
            append(statistics.year)
            append("年年度支出柱状图。")
            append(
                statistics.monthlyExpenses.joinToString("，") { month ->
                    "${month.month}月${formatMoney(month.amountMinor)}"
                },
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("月度趋势", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "柱高按最高月份等比显示",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = "峰值 ${highestMonth.month}月",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }

            Spacer(Modifier.height(24.dp))
            ExpenseBars(
                monthlyExpenses = statistics.monthlyExpenses,
                highestMonth = highestMonth.month,
                contentDescription = description,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                statistics.monthlyExpenses.forEach { month ->
                    Text(
                        text = month.month.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (month.month == highestMonth.month) colors.primary else colors.onSurfaceVariant,
                        fontWeight = if (month.month == highestMonth.month) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                text = "月份",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun ExpenseBars(
    monthlyExpenses: List<MonthlyExpense>,
    highestMonth: Int,
    contentDescription: String,
) {
    val colors = MaterialTheme.colorScheme
    val maximum = monthlyExpenses.maxOf(MonthlyExpense::amountMinor)
    val gridColor = colors.outlineVariant.copy(alpha = 0.55f)
    val barTopColor = colors.primary
    val barBottomColor = colors.secondary
    val peakTopColor = colors.tertiary
    val peakBottomColor = colors.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val gridStroke = 1.dp.toPx()
        repeat(5) { line ->
            val y = size.height * line / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = gridStroke,
            )
        }

        val slotWidth = size.width / monthlyExpenses.size
        val barWidth = slotWidth * 0.54f
        val minimumVisibleHeight = 4.dp.toPx()

        monthlyExpenses.forEachIndexed { index, month ->
            if (month.amountMinor == 0L) return@forEachIndexed

            val ratio = (month.amountMinor.toDouble() / maximum.toDouble()).toFloat()
            val barHeight = (size.height * ratio).coerceAtLeast(minimumVisibleHeight)
            val left = (slotWidth * index) + ((slotWidth - barWidth) / 2f)
            val top = size.height - barHeight
            val cornerRadius = minOf(barWidth / 2f, barHeight / 2f)
            val brush = if (month.month == highestMonth) {
                Brush.verticalGradient(listOf(peakTopColor, peakBottomColor), startY = top, endY = size.height)
            } else {
                Brush.verticalGradient(listOf(barTopColor, barBottomColor), startY = top, endY = size.height)
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            )
        }
    }
}

@Composable
private fun EmptyStatistics(year: Int) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$year 年暂无支出数据" },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = colors.primaryContainer.copy(alpha = 0.62f),
                        radius = size.minDimension / 2f,
                    )
                    drawRoundRect(
                        color = colors.primary.copy(alpha = 0.32f),
                        topLeft = Offset(size.width * 0.22f, size.height * 0.56f),
                        size = Size(size.width * 0.13f, size.height * 0.22f),
                        cornerRadius = CornerRadius(size.width * 0.04f),
                    )
                    drawRoundRect(
                        color = colors.primary.copy(alpha = 0.52f),
                        topLeft = Offset(size.width * 0.43f, size.height * 0.42f),
                        size = Size(size.width * 0.13f, size.height * 0.36f),
                        cornerRadius = CornerRadius(size.width * 0.04f),
                    )
                    drawRoundRect(
                        color = colors.primary.copy(alpha = 0.78f),
                        topLeft = Offset(size.width * 0.64f, size.height * 0.27f),
                        size = Size(size.width * 0.13f, size.height * 0.51f),
                        cornerRadius = CornerRadius(size.width * 0.04f),
                    )
                }
                Text("✨", fontSize = 24.sp, modifier = Modifier.align(Alignment.TopEnd))
            }
            Text(
                text = "这一年还没有支出",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = "记录支出后，这里会自动生成月度趋势",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
