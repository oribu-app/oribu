package app.oribu.service.sync

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.oribu.R
import app.oribu.model.MediaItem
import app.oribu.model.MediaType
import app.oribu.service.RetroAchievementsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Hobby sections of Settings → Tracking. */
enum class TrackingHobby(
    @StringRes val labelRes: Int,
) {
    GAMES(R.string.games_title),
    ANIME_MANGA(R.string.tracking_section_manga),
    MOVIES_SERIES(R.string.tracking_section_movies_series),
    BOOKS(R.string.books_title),
}

/**
 * Account integrations that bring the user's own progress into the library. Each one says which
 * library items it touches; what it reads lives in
 * MediaCacheService (Steam/RetroAchievements achievements, AniList progress).
 */
enum class TrackingIntegration(
    val id: String,
    val hobby: TrackingHobby,
) {
    STEAM("steam", TrackingHobby.GAMES),
    RETROACHIEVEMENTS("retroachievements", TrackingHobby.GAMES),
    ANILIST("anilist", TrackingHobby.ANIME_MANGA),
    ;

    fun covers(item: MediaItem): Boolean =
        item.externalId != null &&
            when (this) {
                STEAM -> item.type == MediaType.GAME && item.console?.isSteam == true
                RETROACHIEVEMENTS -> item.type == MediaType.GAME && RetroAchievementsService.supports(item.console)
                ANILIST -> item.type in listOf(MediaType.MANGA, MediaType.WEBTOON, MediaType.ANIME) && item.apiSource == "anilist"
            }
}

private val Context.trackingDataStore by preferencesDataStore(name = "tracking_prefs")

/** Per-integration "update automatically" switch (on by default) and last manual/automatic sync. */
object TrackingPreferences {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _autoSync = MutableStateFlow(TrackingIntegration.entries.associateWith { true })
    val autoSync: StateFlow<Map<TrackingIntegration, Boolean>> = _autoSync

    private val _lastSyncMs = MutableStateFlow<Map<TrackingIntegration, Long>>(emptyMap())
    val lastSyncMs: StateFlow<Map<TrackingIntegration, Long>> = _lastSyncMs

    private fun autoKey(integration: TrackingIntegration) = booleanPreferencesKey("auto_${integration.id}")

    private fun lastKey(integration: TrackingIntegration) = longPreferencesKey("last_${integration.id}")

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.trackingDataStore.data.collect { prefs ->
                _autoSync.value = TrackingIntegration.entries.associateWith { prefs[autoKey(it)] ?: true }
                _lastSyncMs.value = TrackingIntegration.entries.mapNotNull { i -> prefs[lastKey(i)]?.let { i to it } }.toMap()
            }
        }
    }

    fun isAutoSync(integration: TrackingIntegration): Boolean = _autoSync.value[integration] ?: true

    fun setAutoSync(
        integration: TrackingIntegration,
        enabled: Boolean,
    ) {
        _autoSync.value = _autoSync.value + (integration to enabled)
        scope.launch { appContext.trackingDataStore.edit { it[autoKey(integration)] = enabled } }
    }

    fun markSynced(
        integration: TrackingIntegration,
        timeMs: Long,
    ) {
        _lastSyncMs.value = _lastSyncMs.value + (integration to timeMs)
        scope.launch { appContext.trackingDataStore.edit { it[lastKey(integration)] = timeMs } }
    }
}
