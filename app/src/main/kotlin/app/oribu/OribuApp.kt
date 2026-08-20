package app.oribu

import android.app.Application
import app.oribu.data.ApiKeyPreferences
import app.oribu.data.OnboardingPreferences
import app.oribu.data.PlatformPreferences
import app.oribu.data.StoragePreferences
import app.oribu.data.db.DB
import app.oribu.debug.DebugSeeder
import app.oribu.service.ApiServices
import app.oribu.service.GameDatasetImporter
import app.oribu.service.NotificationHelper
import app.oribu.ui.theme.AppThemeController
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
        StoragePreferences.init(this)
        ApiKeyPreferences.init(this)
        AppThemeController.init(this)
        appScope.launch {
            ApiServices.init(this@OribuApp)
            // Import GiantBomb dataset on first run (no-op if already done)
            GameDatasetImporter.importIfNeeded(this@OribuApp)
            // Dados fake para testes de usabilidade — só roda em build de debug e só se a
            // biblioteca estiver vazia.
            if (BuildConfig.DEBUG) DebugSeeder.seedIfEmpty(DB.repo)
        }
        CacheUpdateWorker.schedule(this)
    }
}
