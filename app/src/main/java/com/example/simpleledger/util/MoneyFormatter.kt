package com.example.simpleledger.util

import java.math.BigDecimal

object MoneyFormatter {
    fun toPlainAmount(amountMinor: Long): String =
        BigDecimal.valueOf(amountMinor, 2).toPlainString()
}
