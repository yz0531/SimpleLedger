package com.example.simpleledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = colors.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.84f),
                            colors.tertiary.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onChooseMonth)
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "选择年月",
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp),
                        tint = colors.onPrimary.copy(alpha = 0.82f),
                    )
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
                    top = 4.dp,
                    bottom = if (expenseOnly) 6.dp else 14.dp,
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
    val colors = MaterialTheme.colorScheme
    val typeContainer = if (isIncome) colors.primaryContainer else colors.errorContainer
    val rowBrush = Brush.horizontalGradient(
        listOf(
            typeContainer.copy(alpha = 0.82f),
            colors.surface.copy(alpha = 0.70f),
            colors.surfaceContainerLow.copy(alpha = 0.58f),
        ),
    )
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, typeContainer.copy(alpha = 0.62f)),
    ) {
        Row(
            modifier = Modifier
                .background(rowBrush)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isIncome) {
                            colors.primaryContainer
                        } else {
                            colors.errorContainer.copy(alpha = 0.86f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.categoryId),
                    contentDescription = category?.label,
                    tint = if (isIncome) {
                        colors.onPrimaryContainer
                    } else {
                        colors.onErrorContainer
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
                        color = colors.onSurfaceVariant,
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
                color = if (isIncome) colors.primary else colors.error,
            )
        }
    }
}
