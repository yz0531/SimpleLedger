package com.example.simpleledger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.simpleledger.data.preferences.LedgerPreferences
import com.example.simpleledger.data.recurring.RecurringTransactionProcessor
import com.example.simpleledger.domain.model.LedgerMode
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        val includeIncome = LedgerPreferences(applicationContext).mode.value == LedgerMode.INCOME_AND_EXPENSE
        val result = RecurringTransactionProcessor.create(applicationContext).processDue(
            includeIncome = includeIncome,
        )
        Result.success(
            workDataOf(
                OUTPUT_PROCESSED_RULES to result.processedRuleCount,
                OUTPUT_CREATED_TRANSACTIONS to result.createdTransactionCount,
                OUTPUT_SKIPPED_DUPLICATES to result.skippedDuplicateCount,
            ),
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IllegalArgumentException) {
        Result.failure(workDataOf(OUTPUT_ERROR to (exception.message ?: "周期规则无效")))
    } catch (exception: Exception) {
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
            Result.retry()
        } else {
            Result.failure(workDataOf(OUTPUT_ERROR to (exception.message ?: "周期记账执行失败")))
        }
    }

    companion object {
        const val OUTPUT_PROCESSED_RULES = "processed_rule_count"
        const val OUTPUT_CREATED_TRANSACTIONS = "created_transaction_count"
        const val OUTPUT_SKIPPED_DUPLICATES = "skipped_duplicate_count"
        const val OUTPUT_ERROR = "error"

        private const val MAX_RETRY_ATTEMPTS = 3
    }
}

object RecurringWorkScheduler {
    private const val UNIQUE_WORK_NAME = "simple-ledger-recurring-posting"

    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            1,
            TimeUnit.DAYS,
        ).build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
