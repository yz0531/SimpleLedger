package com.example.simpleledger.data.repository

import androidx.room.withTransaction
import com.example.simpleledger.data.local.LedgerDatabase
import com.example.simpleledger.data.local.toDomain
import com.example.simpleledger.data.local.toEntity
import com.example.simpleledger.domain.model.ImportResult
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLedgerRepository(
    private val database: LedgerDatabase,
) : LedgerRepository {
    private val dao = database.transactionDao()

    override fun observeAll(): Flow<List<LedgerTransaction>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

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
