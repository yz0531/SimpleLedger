package com.example.simpleledger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Query(
        """
        SELECT * FROM recurring_rules
        ORDER BY isEnabled DESC, nextExecutionDate ASC, createdAtEpochMs DESC, id DESC
        """,
    )
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query(
        """
        SELECT * FROM recurring_rules
        ORDER BY isEnabled DESC, nextExecutionDate ASC, createdAtEpochMs DESC, id DESC
        """,
    )
    suspend fun getAllSnapshot(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringRuleEntity?

    @Query(
        """
        SELECT * FROM recurring_rules
        WHERE isEnabled = 1 AND nextExecutionDate <= :throughDate
        ORDER BY nextExecutionDate ASC, createdAtEpochMs ASC, id ASC
        """,
    )
    suspend fun getDueEnabled(throughDate: String): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<RecurringRuleEntity>)

    @Query("SELECT id FROM recurring_rules")
    suspend fun getAllIds(): List<String>

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        UPDATE recurring_rules
        SET isEnabled = 0,
            updatedAtEpochMs = CASE
                WHEN updatedAtEpochMs > :updatedAtEpochMs THEN updatedAtEpochMs
                ELSE :updatedAtEpochMs
            END
        WHERE type = 'income' AND isEnabled = 1
        """,
    )
    suspend fun disableIncomeRules(updatedAtEpochMs: Long): Int

    @Query(
        """
        UPDATE recurring_rules
        SET isEnabled = :isEnabled,
            nextExecutionDate = :nextExecutionDate,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :id
        """,
    )
    suspend fun setEnabled(
        id: String,
        isEnabled: Boolean,
        nextExecutionDate: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        """
        UPDATE recurring_rules
        SET nextExecutionDate = :nextExecutionDate, updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :id
        """,
    )
    suspend fun updateAfterProcessing(
        id: String,
        nextExecutionDate: String,
        updatedAtEpochMs: Long,
    ): Int
}
