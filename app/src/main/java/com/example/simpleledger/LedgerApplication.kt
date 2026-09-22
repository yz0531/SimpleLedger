package com.example.simpleledger

import android.app.Application
import android.content.Context
import com.example.simpleledger.data.backup.NutstoreBackupManager
import com.example.simpleledger.data.backup.NutstoreCredentialStore
import com.example.simpleledger.data.local.LedgerDatabase
import com.example.simpleledger.data.preferences.LedgerPreferences
import com.example.simpleledger.data.recurring.RecurringTransactionProcessor
import com.example.simpleledger.data.repository.OfflineLedgerRepository
import com.example.simpleledger.data.repository.OfflineRecurringRuleRepository
import com.example.simpleledger.data.transfer.DataTransferManager
import com.example.simpleledger.data.transfer.LedgerBackupStore
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.domain.repository.LedgerRepository
import com.example.simpleledger.domain.repository.RecurringRuleRepository
import com.example.simpleledger.work.RecurringWorkScheduler

class AppContainer(context: Context) {
    private val database = LedgerDatabase.getInstance(context)
    private val backupStore = LedgerBackupStore(database)

    val repository: LedgerRepository = OfflineLedgerRepository(database)
    val recurringRepository: RecurringRuleRepository = OfflineRecurringRuleRepository(database)
    val recurringProcessor: RecurringTransactionProcessor = RecurringTransactionProcessor.create(database)
    val preferences = LedgerPreferences(context)
    val transferManager = DataTransferManager(
        context = context,
        repository = repository,
        backupStore = backupStore,
    )
    private val nutstoreCredentialStore = NutstoreCredentialStore(context)
    val nutstoreBackupManager = NutstoreBackupManager(
        backupStore = backupStore,
        credentialStore = nutstoreCredentialStore,
        postRestoreMaintenance = ::maintainImportedData,
    )

    suspend fun maintainImportedData() {
        val mode = preferences.currentMode
        if (mode == LedgerMode.EXPENSE_ONLY) {
            recurringRepository.disableIncomeRules()
        }
        recurringProcessor.processDue(
            includeIncome = mode == LedgerMode.INCOME_AND_EXPENSE,
        )
    }
}

class LedgerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RecurringWorkScheduler.scheduleDaily(this)
    }
}
