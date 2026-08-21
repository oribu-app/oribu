package app.oribu.worker

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.oribu.service.NotificationHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Baixa o APK de uma release e deixa pronto pra instalar — ver NotificationHelper.notifyUpdateReadyToInstall. */
class AppUpdateInstallWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val assetName = inputData.getString(KEY_ASSET_NAME) ?: "update.apk"
        val apkFile = File(applicationContext.externalCacheDir, assetName)

        return try {
            downloadTo(url, apkFile)

            val apkUri = FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", apkFile)
            val installIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            NotificationHelper.notifyUpdateReadyToInstall(applicationContext, installIntent)
            Result.success()
        } catch (e: Exception) {
            NotificationHelper.notifyUpdateError()
            Result.failure()
        }
    }

    private fun downloadTo(
        url: String,
        apkFile: File,
    ) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Download HTTP ${response.code}")
            val body = response.body ?: throw Exception("Resposta vazia")
            val total = body.contentLength()
            var downloaded = 0L
            var lastReportedPercent = -1
            apkFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                NotificationHelper.notifyUpdateProgress(percent)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val WORK_NAME = "app_update_install"
        private const val KEY_URL = "url"
        private const val KEY_ASSET_NAME = "assetName"

        fun enqueue(
            context: Context,
            url: String,
            assetName: String,
        ) {
            val request =
                OneTimeWorkRequestBuilder<AppUpdateInstallWorker>()
                    .setInputData(workDataOf(KEY_URL to url, KEY_ASSET_NAME to assetName))
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
