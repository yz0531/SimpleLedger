package com.example.simpleledger.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object MoneyParser {
    /** ¥999,999,999.99; keeping this bound also makes aggregate overflow unrealistic. */
    const val MAX_AMOUNT_MINOR: Long = 99_999_999_999L
    const val MAX_EXPRESSION_LENGTH: Int = 40

    private val amountPattern = Regex("^(?:0|[1-9]\\d{0,8})(?:[.,]\\d{1,2})?$")
    private val expressionNumberPattern = Regex("(?:0|[1-9]\\d{0,8})(?:\\.\\d{1,2})?")

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

    fun parseExpression(input: String): Long? {
        val normalized = input
            .trim()
            .replace('。', '.')
            .replace('，', '.')
            .replace(',', '.')
            .replace('×', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('÷', '/')
            .replace('／', '/')
            .replace('−', '-')
            .replace(" ", "")

        if (normalized.isEmpty() || normalized.length > MAX_EXPRESSION_LENGTH) return null

        return try {
            val value = ExpressionParser(normalized).parse() ?: return null
            val amountMinor = value
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
            amountMinor.takeIf { it in 1..MAX_AMOUNT_MINOR }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun isExpressionInput(value: String): Boolean =
        value.length <= MAX_EXPRESSION_LENGTH && value.all { character ->
            character.isDigit() || character in ".,。，+-*×xX/÷／−"
        }

    private class ExpressionParser(private val expression: String) {
        private var index = 0

        fun parse(): BigDecimal? {
            var total = BigDecimal.ZERO
            var additiveOperator = '+'
            var term = readNumber() ?: return null

            while (index < expression.length) {
                when (val operator = expression[index++]) {
                    '*' -> term = term.multiply(readNumber() ?: return null)
                    '/' -> {
                        val divisor = readNumber() ?: return null
                        if (divisor.compareTo(BigDecimal.ZERO) == 0) return null
                        term = term.divide(divisor, MathContext.DECIMAL128)
                    }
                    '+', '-' -> {
                        total = applyAdditiveOperator(total, term, additiveOperator)
                        additiveOperator = operator
                        term = readNumber() ?: return null
                    }
                    else -> return null
                }
            }

            return applyAdditiveOperator(total, term, additiveOperator)
        }

        private fun readNumber(): BigDecimal? {
            val start = index
            while (index < expression.length && expression[index] !in "+-*/") {
                index += 1
            }
            val token = expression.substring(start, index)
            if (!expressionNumberPattern.matches(token)) return null
            return token.toBigDecimalOrNull()
        }

        private fun applyAdditiveOperator(
            total: BigDecimal,
            term: BigDecimal,
            operator: Char,
        ): BigDecimal = if (operator == '+') total.add(term) else total.subtract(term)
    }
}
