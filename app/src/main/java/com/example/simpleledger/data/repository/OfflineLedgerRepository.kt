package com.example.simpleledger.data.repository

import androidx.room.withTransaction
import com.example.simpleledger.data.local.LedgerDatabase
import com.example.simpleledger.data.local.toDomain
import com.example.simpleledger.data.local.toEntity
import com.example.simpleledger.domain.model.ImportResult
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.MonthlyExpense
import com.example.simpleledger.domain.model.YearlyExpenseStatistics
import com.example.simpleledger.domain.repository.LedgerRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLedgerRepository(
    private val database: LedgerDatabase,
) : LedgerRepository {
    private val dao = database.transactionDao()

    override fun observeDateRange(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<LedgerTransaction>> = dao.observeDateRange(startInclusive, endExclusive)
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeYearlyExpenseStatistics(year: Int): Flow<YearlyExpenseStatistics> {
        val start = LocalDate.of(year, 1, 1).toString()
        val end = LocalDate.of(year + 1, 1, 1).toString()
        return dao.observeMonthlyExpenseTotals(start, end).map { totals ->
            val amountsByMonth = totals.associate { it.month to it.amountMinor }
            YearlyExpenseStatistics(
                year = year,
                monthlyExpenses = (1..12).map { month ->
                    MonthlyExpense(month = month, amountMinor = amountsByMonth[month] ?: 0L)
                },
            )
        }
    }

    override suspend fun getById(id: String): LedgerTransaction? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(transaction: LedgerTransaction) {
        dao.upsert(transaction.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun getAllSnapshot(): List<LedgerTransaction> =
        dao.getAllSnapshot().map { it.toDomain() }

    override suspend fun importTransactions(
        transactions: List<LedgerTransaction>,
    ): ImportResult = database.withTransaction {
        require(transactions.map { it.id }.toSet().size == transactions.size) {
            "导入数据中存在重复 ID"
        }

        val existingIds = dao.getAllIds().toHashSet()
        val updatedCount = transactions.count { it.id in existingIds }
        val insertedCount = transactions.size - updatedCount
        dao.upsertAll(transactions.map { it.toEntity() })

        ImportResult(
            importedCount = transactions.size,
            insertedCount = insertedCount,
            updatedCount = updatedCount,
        )
    }
}
