package com.example.simpleledger.domain.model

enum class TransactionType(val wireValue: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromWireValue(value: String): TransactionType? =
            entries.firstOrNull { it.wireValue == value }
    }
}
