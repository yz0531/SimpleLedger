package com.example.simpleledger.util

import java.time.LocalDate

object DateValidator {
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun isValidIsoLocalDate(value: String): Boolean {
        if (!datePattern.matches(value)) return false
        val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return false
        return date.year in 1900..2999
    }
}
