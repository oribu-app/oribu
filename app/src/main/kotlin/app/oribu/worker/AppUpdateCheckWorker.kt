package app.oribu.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.oribu.service.AppUpdateChecker
import app.oribu.service.AppUpdateResult
import app.oribu.service.NotificationHelper
import java.util.concurrent.TimeUnit

/** Checagem silenciosa diária — notifica só quando encontra versão nova (throttle de 24h no próprio AppUpdateChecker). */
class AppUpdateCheckWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result =
        try {
            val result = AppUpdateChecker.checkForUpdate(applicationContext, isUserPrompt = false)
            if (result is AppUpdateResult.NewUpdate) {
                NotificationHelper.notifyUpdateAvailable(applicationContext, result.release)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }

    companion object {
        private const val WORK_NAME = "app_update_check_daily"

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(24, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
