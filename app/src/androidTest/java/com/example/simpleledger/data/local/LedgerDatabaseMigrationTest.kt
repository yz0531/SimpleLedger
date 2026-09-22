package com.example.simpleledger.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class LedgerDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LedgerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2PreservesTransactionsAndCreatesRecurringRules() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO transactions (
                    id, type, amountMinor, categoryId, occurredOn, note,
                    createdAtEpochMs, updatedAtEpochMs
                ) VALUES ('legacy', 'expense', 2500, 'expense.food', '2026-09-21', '午餐', 10, 10)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            2,
            true,
            LedgerDatabase.MIGRATION_1_2,
        ).use { database ->
            database.query("SELECT COUNT(*) FROM transactions").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM recurring_rules").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun dateRangeAndMonthlyAggregationStayInsideRequestedPeriod() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LedgerDatabase::class.java,
        ).build()
        try {
            val dao = database.transactionDao()
            dao.upsertAll(
                listOf(
                    transaction("jan", "2026-01-03", 1_200L),
                    transaction("feb", "2026-02-03", 2_300L),
                    transaction("other-year", "2025-02-03", 9_900L),
                ),
            )

            val february = dao.observeDateRange("2026-02-01", "2026-03-01").first()
            assertEquals(listOf("feb"), february.map { it.id })

            val totals = dao.observeMonthlyExpenseTotals("2026-01-01", "2027-01-01").first()
            assertEquals(listOf(1, 2), totals.map { it.month })
            assertEquals(listOf(1_200L, 2_300L), totals.map { it.amountMinor })
        } finally {
            database.close()
        }
    }

    private fun transaction(id: String, date: String, amountMinor: Long) = TransactionEntity(
        id = id,
        type = "expense",
        amountMinor = amountMinor,
        categoryId = "expense.food",
        occurredOn = date,
        note = "",
        createdAtEpochMs = 10L,
        updatedAtEpochMs = 10L,
    )

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
    }
}
