package app.oribu.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.oribu.data.backup.BackupErrorReason
import app.oribu.data.backup.BackupException
import app.oribu.data.backup.BackupFrequency
import app.oribu.data.backup.BackupKind
import app.oribu.data.backup.BackupService
import java.util.concurrent.TimeUnit

/** Scheduled library backup into the user's backup folder (Settings → Data → Automatic backup). */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        try {
            BackupService.backupToFolder(applicationContext, BackupKind.SCHEDULED)
            Result.success()
        } catch (e: BackupException) {
            Log.w("BackupWorker", "Scheduled backup skipped: ${e.reason}", e)
            // No folder chosen yet isn't worth retrying until the user picks one.
            if (e.reason == BackupErrorReason.NO_FOLDER) Result.success() else Result.retry()
        }

    companion object {
        private const val WORK_NAME = "library_backup"

        fun schedule(
            context: Context,
            frequency: BackupFrequency,
        ) {
            val workManager = WorkManager.getInstance(context)
            val hours = frequency.intervalHours
            if (hours == null) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<BackupWorker>(hours, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
