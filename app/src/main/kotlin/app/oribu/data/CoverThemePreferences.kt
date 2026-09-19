package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** [styleName] is a `com.materialkolor.PaletteStyle` enum name, or null for Rokku's "Legacy theme". */
data class SavedCoverTheme(
    val enabled: Boolean = false,
    val styleName: String? = null,
)

private val Context.coverThemeDataStore by preferencesDataStore(name = "cover_theme_prefs")

/** Persistência de `CoverThemeController` (Settings > Aparência > Página de detalhes). */
object CoverThemePreferences {
    private val ENABLED_KEY = booleanPreferencesKey("theme_based_on_cover")
    private val STYLE_KEY = stringPreferencesKey("cover_theme_style")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _saved = MutableStateFlow(SavedCoverTheme())
    val saved: StateFlow<SavedCoverTheme> = _saved

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.coverThemeDataStore.data.collect { prefs ->
                _saved.value =
                    SavedCoverTheme(
                        enabled = prefs[ENABLED_KEY] ?: false,
                        styleName = prefs[STYLE_KEY],
                    )
            }
        }
    }

    fun persist(value: SavedCoverTheme) {
        scope.launch {
            appContext.coverThemeDataStore.edit { prefs ->
                prefs[ENABLED_KEY] = value.enabled
                if (value.styleName == null) {
                    prefs.remove(STYLE_KEY)
                } else {
                    prefs[STYLE_KEY] = value.styleName
                }
            }
        }
    }
}
