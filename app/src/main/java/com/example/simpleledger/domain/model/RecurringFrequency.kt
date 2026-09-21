package com.example.simpleledger.domain.model

enum class RecurringFrequency(val wireValue: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    BIWEEKLY("biweekly"),
    MONTHLY("monthly");

    companion object {
        fun fromWireValue(value: String): RecurringFrequency? =
            entries.firstOrNull { it.wireValue == value }
    }
}
