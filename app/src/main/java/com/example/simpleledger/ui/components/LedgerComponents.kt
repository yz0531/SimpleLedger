package com.example.simpleledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.TransactionType

@Composable
fun MonthSummaryCard(
    monthLabel: String,
    balanceMinor: Long,
    incomeMinor: Long,
    expenseMinor: Long,
    expenseOnly: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onChooseMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(colors.primary, colors.tertiary),
                    ),
                )
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth, enabled = canGoPrevious) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = "上个月",
                        tint = colors.onPrimary,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onChooseMonth)
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.onPrimary.copy(alpha = 0.76f),
                        )
                        Text(
                            text = if (expenseOnly) "本月花销 · 点击选择年月" else "本月结余 · 点击选择年月",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPrimary.copy(alpha = 0.76f),
                            modifier = Modifier.padding(start = 5.dp),
                        )
                    }
                }
                IconButton(onClick = onNextMonth, enabled = canGoNext) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "下个月",
                        tint = colors.onPrimary,
                    )
                }
            }
            Text(
                text = formatMoney(if (expenseOnly) expenseMinor else balanceMinor),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onPrimary,
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 8.dp,
                    bottom = if (expenseOnly) 10.dp else 20.dp,
                ),
            )
            if (!expenseOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryValue(
                        label = "收入",
                        value = formatMoney(incomeMinor),
                        dotColor = colors.secondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryValue(
                        label = "支出",
                        value = formatMoney(expenseMinor),
                        dotColor = colors.errorContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryValue(
    label: String,
    value: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Column(Modifier.padding(start = 8.dp)) {
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
}

@Composable
fun TransactionRow(
    transaction: LedgerTransaction,
    expenseOnly: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val category = Categories.find(transaction.categoryId)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isIncome) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.categoryId),
                    contentDescription = category?.label,
                    tint = if (isIncome) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = category?.label ?: "其他",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = if (expenseOnly && !isIncome) {
                    formatMoney(transaction.amountMinor)
                } else {
                    formatSignedMoney(transaction.amountMinor, isIncome)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
