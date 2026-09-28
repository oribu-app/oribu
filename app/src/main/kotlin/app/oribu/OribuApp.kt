package app.oribu

import android.app.Application
import app.oribu.data.ApiKeyPreferences
import app.oribu.data.OnboardingPreferences
import app.oribu.data.PlatformPreferences
import app.oribu.data.SeriesScopePreferences
import app.oribu.data.StoragePreferences
import app.oribu.data.backup.BackupPreferences
import app.oribu.data.db.DB
import app.oribu.service.ApiServices
import app.oribu.service.GameDatasetImporter
import app.oribu.service.NotificationHelper
import app.oribu.service.sync.TrackingPreferences
import app.oribu.ui.locale.AppLocaleController
import app.oribu.ui.theme.AppThemeController
import app.oribu.ui.theme.CoverThemeController
import app.oribu.worker.AppUpdateCheckWorker
import app.oribu.worker.BackupWorker
import app.oribu.worker.CacheUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OribuApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DB.init(this)
        NotificationHelper.init(this)
        PlatformPreferences.init(this)
        OnboardingPreferences.init(this)
        SeriesScopePreferences.init(this)
        StoragePreferences.init(this)
        BackupPreferences.init(this)
        TrackingPreferences.init(this)
        ApiKeyPreferences.init(this)
        AppThemeController.init(this)
        AppLocaleController.init(this)
        CoverThemeController.init(this)
        appScope.launch {
            ApiServices.init(this@OribuApp)
            // Import GiantBomb dataset on first run (no-op if already done)
            GameDatasetImporter.importIfNeeded(this@OribuApp)
        }
        CacheUpdateWorker.schedule(this)
        AppUpdateCheckWorker.schedule(this)
        // Re-schedules (or cancels) the automatic backup whenever the chosen frequency changes.
        appScope.launch {
            BackupPreferences.frequency.collect { BackupWorker.schedule(this@OribuApp, it) }
        }
    }
}
