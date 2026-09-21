package com.example.simpleledger.util

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateValidator {
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun isValidIsoLocalDate(value: String): Boolean {
        if (!datePattern.matches(value)) return false
        val year = value.substring(0, 4).toIntOrNull() ?: return false
        if (year !in 1900..2999) return false

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        return formatter.parse(value, position) != null && position.index == value.length
    }
}
