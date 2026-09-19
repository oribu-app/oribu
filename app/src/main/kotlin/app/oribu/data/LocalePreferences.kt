package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.oribu.ui.locale.DateFormatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SavedLocale(
    val dateFormatMode: DateFormatMode = DateFormatMode.SYSTEM,
)

private val Context.localeDataStore by preferencesDataStore(name = "locale_prefs")

/**
 * Persistence for `AppLocaleController.dateFormatMode`. The language itself isn't stored here —
 * AppCompat persists it on its own via the `AppLocalesMetadataHolderService` entry declared in
 * the manifest.
 */
object LocalePreferences {
    private val DATE_FORMAT_KEY = stringPreferencesKey("date_format_mode")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _saved = MutableStateFlow(SavedLocale())
    val saved: StateFlow<SavedLocale> = _saved

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.localeDataStore.data.collect { prefs ->
                _saved.value =
                    SavedLocale(
                        dateFormatMode =
                            prefs[DATE_FORMAT_KEY]
                                ?.let { runCatching { DateFormatMode.valueOf(it) }.getOrNull() }
                                ?: DateFormatMode.SYSTEM,
                    )
            }
        }
    }

    fun persist(locale: SavedLocale) {
        scope.launch {
            appContext.localeDataStore.edit { prefs ->
                prefs[DATE_FORMAT_KEY] = locale.dateFormatMode.name
            }
        }
    }
}
