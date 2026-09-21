package com.example.simpleledger.ui.components

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.simpleledger.util.MoneyParser

private val chineseLocale = Locale.SIMPLIFIED_CHINESE
private val dayFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", chineseLocale)
private val editorDayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", chineseLocale)
private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年 M月", chineseLocale)

fun formatMoney(amountMinor: Long): String =
    NumberFormat.getCurrencyInstance(chineseLocale).format(BigDecimal.valueOf(amountMinor, 2))

fun formatSignedMoney(amountMinor: Long, positive: Boolean): String =
    (if (positive) "+" else "−") + formatMoney(kotlin.math.abs(amountMinor))

fun formatDay(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(dayFormatter)
}.getOrDefault(isoDate)

fun formatEditorDay(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(editorDayFormatter)
}.getOrDefault(isoDate)

fun formatMonth(month: YearMonth): String = month.format(monthFormatter)

fun parseAmountMinor(input: String): Long? {
    val normalized = input.trim().removePrefix("¥").removePrefix("￥")
    return MoneyParser.parse(normalized)
}

fun amountInput(amountMinor: Long): String =
    BigDecimal.valueOf(amountMinor, 2).stripTrailingZeros().toPlainString()
