package com.example.simpleledger.util

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyParser {
    /** ¥999,999,999.99; keeping this bound also makes aggregate overflow unrealistic. */
    const val MAX_AMOUNT_MINOR: Long = 99_999_999_999L

    private val amountPattern = Regex("^(?:0|[1-9]\\d{0,8})(?:[.,]\\d{1,2})?$")

    fun parse(input: String): Long? {
        val normalized = input
            .trim()
            .replace('。', '.')
            .replace('，', ',')

        if (!amountPattern.matches(normalized)) return null

        return try {
            val amount = BigDecimal(normalized.replace(',', '.'))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
            amount.takeIf { it in 1..MAX_AMOUNT_MINOR }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }
}
