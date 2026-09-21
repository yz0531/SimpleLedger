package com.example.simpleledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, RecurringRuleEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringRuleDao(): RecurringRuleDao

    companion object {
        private const val DATABASE_NAME = "simple_ledger.db"

        @Volatile
        private var instance: LedgerDatabase? = null

        fun getInstance(context: Context): LedgerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LedgerDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_rules` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `nextExecutionDate` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_recurring_rules_isEnabled_nextExecutionDate`
                    ON `recurring_rules` (`isEnabled`, `nextExecutionDate`)
                    """.trimIndent(),
                )
            }
        }
    }
}
