package com.example.simpleledger.data.backup

import com.example.simpleledger.data.transfer.LedgerBackup
import com.example.simpleledger.domain.model.Categories
import com.example.simpleledger.domain.model.LedgerTransaction
import com.example.simpleledger.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutstoreBackupPolicyTest {
    private val lunch = LedgerTransaction(
        id = "lunch",
        type = TransactionType.EXPENSE,
        amountMinor = 2_800L,
        categoryId = Categories.expenseFood.id,
        occurredOn = "2026-09-21",
        note = "午餐",
        createdAtEpochMs = 100L,
        updatedAtEpochMs = 100L,
    )

    @Test
    fun identicalContentHasStableFingerprintRegardlessOfListOrder() {
        val taxi = lunch.copy(id = "taxi", categoryId = Categories.expenseTransport.id)
        val first = BackupFingerprint.calculate(LedgerBackup(listOf(lunch, taxi), emptyList()))
        val second = BackupFingerprint.calculate(LedgerBackup(listOf(taxi, lunch), emptyList()))

        assertEquals(first, second)
    }

    @Test
    fun changedLedgerContentChangesFingerprint() {
        val original = BackupFingerprint.calculate(LedgerBackup(listOf(lunch), emptyList()))
        val changed = BackupFingerprint.calculate(
            LedgerBackup(listOf(lunch.copy(amountMinor = lunch.amountMinor + 100L)), emptyList()),
        )

        assertNotEquals(original, changed)
    }

    @Test
    fun automaticRetentionKeepsOnlyThreeNewestFiles() {
        val files = listOf(
            "auto_2026-09-18_08-00-00-000.json",
            "auto_2026-09-21_08-00-00-000.json",
            "manual_2026-09-01_08-00-00-000.json",
            "auto_2026-09-19_08-00-00-000.json",
            "notes.txt",
            "auto_2026-09-20_08-00-00-000.json",
        )

        assertEquals(
            listOf("auto_2026-09-18_08-00-00-000.json"),
            NutstoreBackupFiles.filesToDelete(files),
        )
    }

    @Test
    fun mostRecentBackupComparesTimestampsAcrossManualAndAutomaticFiles() {
        assertEquals(
            "manual_2026-09-21_09-10-11-120.json",
            NutstoreBackupFiles.mostRecent(
                listOf(
                    "manual_2026-09-21_09-10-11-120.json",
                    "auto_2026-09-21_09-10-11-119.json",
                    "unrelated.json",
                ),
            ),
        )
    }

    @Test
    fun generatedNamesAreSortableAndManualFilesAreNeverAutoPruned() {
        val earlier = Instant.parse("2026-09-20T01:02:03Z").toEpochMilli()
        val later = Instant.parse("2026-09-21T01:02:03Z").toEpochMilli()
        val earlyName = NutstoreBackupFiles.automaticName(earlier, ZoneOffset.UTC)
        val lateName = NutstoreBackupFiles.automaticName(later, ZoneOffset.UTC)
        val manualName = NutstoreBackupFiles.manualName(earlier, ZoneOffset.UTC)

        assertTrue(lateName > earlyName)
        assertFalse(NutstoreBackupFiles.filesToDelete(listOf(manualName), keep = 0).contains(manualName))
    }

    @Test
    fun webDavListingExtractsOnlyJsonFiles() {
        val xml = """
            <d:multistatus xmlns:d="DAV:">
              <d:response><d:href>/dav/SimpleLedger/backup/</d:href></d:response>
              <d:response><d:href>/dav/SimpleLedger/backup/auto_2026-09-21_01-02-03-000.json</d:href></d:response>
              <d:response><d:href>/dav/SimpleLedger/backup/%E5%A4%87%E6%B3%A8.txt</d:href></d:response>
            </d:multistatus>
        """.trimIndent()

        assertEquals(
            listOf("auto_2026-09-21_01-02-03-000.json"),
            WebDavListingParser.fileNames(xml),
        )
    }
}
