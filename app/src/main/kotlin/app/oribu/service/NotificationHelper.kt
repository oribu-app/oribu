package app.oribu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object NotificationHelper {
    private const val CHANNEL_ID = "manga_status_changes"
    private const val UPDATE_CHANNEL_ID = "app_updates"
    const val UPDATE_NOTIFICATION_ID = -1

    /**
     * ID separado do UPDATE_NOTIFICATION_ID de propósito: esse é o ID promovido a foreground
     * service durante o download (ver AppUpdateInstallWorker.setForeground). Quando o worker
     * termina, o Android encerra o foreground service e cancela a notificação daquele ID
     * junto — mesmo que o conteúdo já tenha sido trocado pra "pronto pra instalar" um instante
     * antes. Resultado/erro final precisam de um ID próprio pra sobreviver a isso.
     */
    private const val UPDATE_RESULT_NOTIFICATION_ID = -2
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Atualizações de mangás", NotificationManager.IMPORTANCE_DEFAULT))
        manager.createNotificationChannel(
            NotificationChannel(UPDATE_CHANNEL_ID, "Atualizações do app", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private val manager get() = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun notifyStatusChange(
        itemId: Int,
        title: String,
        message: String,
    ) {
        if (!::appContext.isInitialized || !manager.areNotificationsEnabled()) return
        val notification =
            Notification
                .Builder(appContext, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
        manager.notify(itemId, notification)
    }

    /** Também usada pelo AppUpdateInstallWorker para promover o download a foreground service. */
    fun updateProgressNotification(
        context: Context,
        percent: Int,
    ): Notification =
        Notification
            .Builder(context.applicationContext, UPDATE_CHANNEL_ID)
            .setContentTitle("Baixando atualização")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .build()

    fun notifyUpdateProgress(percent: Int) {
        if (!::appContext.isInitialized || !manager.areNotificationsEnabled()) return
        manager.notify(UPDATE_NOTIFICATION_ID, updateProgressNotification(appContext, percent))
    }

    /**
     * Nunca dispara o instalador sozinha — só embrulha o intent numa notificação e deixa o
     * usuário tocar. Um Worker em background não consegue abrir a tela de confirmação de
     * instalação diretamente no Android 10+ (restrição de Background Activity Launch); só um
     * toque do usuário (PendingIntent de notificação) conta como lançamento em primeiro plano.
     */
    fun notifyUpdateReadyToInstall(
        context: Context,
        installIntent: Intent,
    ) {
        if (!::appContext.isInitialized || !manager.areNotificationsEnabled()) return
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification
                .Builder(appContext, UPDATE_CHANNEL_ID)
                .setContentTitle("Atualização pronta")
                .setContentText("Toque para instalar")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        manager.notify(UPDATE_RESULT_NOTIFICATION_ID, notification)
    }

    fun notifyUpdateError() {
        if (!::appContext.isInitialized || !manager.areNotificationsEnabled()) return
        val notification =
            Notification
                .Builder(appContext, UPDATE_CHANNEL_ID)
                .setContentTitle("Falha ao baixar atualização")
                .setContentText("Toque para tentar de novo pela tela Sobre")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build()
        manager.notify(UPDATE_RESULT_NOTIFICATION_ID, notification)
    }

    /** Disparada pela checagem silenciosa diária (AppUpdateCheckWorker) quando acha versão nova. */
    fun notifyUpdateAvailable(
        context: Context,
        release: GithubRelease,
    ) {
        if (!::appContext.isInitialized || !manager.areNotificationsEnabled()) return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification
                .Builder(appContext, UPDATE_CHANNEL_ID)
                .setContentTitle("Nova versão disponível")
                .setContentText("${release.tagName} — toque para abrir o Oribu e atualizar em Sobre")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        manager.notify(UPDATE_NOTIFICATION_ID, notification)
    }
}
