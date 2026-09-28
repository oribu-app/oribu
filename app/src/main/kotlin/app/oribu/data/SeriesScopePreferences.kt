package app.oribu.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.oribu.R
import app.oribu.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the Series tab covers. Each item keeps its own type, so tracking always follows the item. */
enum class SeriesScope(
    val prefValue: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val types: List<MediaType>,
) {
    SERIES("series", R.string.series_scope_series, R.string.series_scope_series_desc, listOf(MediaType.SERIES)),
    ANIME("anime", R.string.series_scope_anime, R.string.series_scope_anime_desc, listOf(MediaType.ANIME)),
    BOTH("both", R.string.series_scope_both, R.string.series_scope_both_desc, listOf(MediaType.SERIES, MediaType.ANIME)),
    ;

    companion object {
        fun fromPref(value: String?): SeriesScope? = entries.firstOrNull { it.prefValue == value }
    }
}

private val Context.seriesScopeDataStore by preferencesDataStore(name = "series_scope_prefs")

/**
 * The Series tab scope. `null` after loading means the user hasn't chosen yet — the tab asks on
 * first access (and points to Settings → General to change it later) instead of assuming.
 */
object SeriesScopePreferences {
    private val SCOPE_KEY = stringPreferencesKey("series_scope")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _seriesScope = MutableStateFlow<SeriesScope?>(null)
    val seriesScope: StateFlow<SeriesScope?> = _seriesScope

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.seriesScopeDataStore.data.collect { prefs ->
                _seriesScope.value = SeriesScope.fromPref(prefs[SCOPE_KEY])
                _loaded.value = true
            }
        }
    }

    fun set(value: SeriesScope) {
        _seriesScope.value = value
        scope.launch {
            appContext.seriesScopeDataStore.edit { prefs -> prefs[SCOPE_KEY] = value.prefValue }
        }
    }
}
