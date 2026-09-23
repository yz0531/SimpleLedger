package com.example.simpleledger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE occurredOn >= :startInclusive AND occurredOn < :endExclusive
        ORDER BY occurredOn DESC, createdAtEpochMs DESC, id DESC
        """,
    )
    fun observeDateRange(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT CAST(substr(occurredOn, 6, 2) AS INTEGER) AS month,
               COALESCE(SUM(amountMinor), 0) AS amountMinor
        FROM transactions
        WHERE type = 'expense'
          AND occurredOn >= :startInclusive
          AND occurredOn < :endExclusive
        GROUP BY substr(occurredOn, 6, 2)
        ORDER BY month ASC
        """,
    )
    fun observeMonthlyExpenseTotals(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<MonthlyExpenseTotal>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY occurredOn DESC, createdAtEpochMs DESC, id DESC
        """,
    )
    suspend fun getAllSnapshot(): List<TransactionEntity>

    @Query("SELECT id FROM transactions")
    suspend fun getAllIds(): List<String>
}

data class MonthlyExpenseTotal(
    val month: Int,
    val amountMinor: Long,
)
