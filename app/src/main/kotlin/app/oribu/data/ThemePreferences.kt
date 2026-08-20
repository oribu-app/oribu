package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.oribu.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SavedTheme(
    val lightThemeId: String = "neko",
    val darkThemeId: String = "neko",
    val themeMode: ThemeMode = ThemeMode.DARK,
)

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/** Persistência de `AppThemeController` — sem isso a escolha de tema se perdia a cada reinício. */
object ThemePreferences {
    private val LIGHT_THEME_KEY = stringPreferencesKey("light_theme_id")
    private val DARK_THEME_KEY = stringPreferencesKey("dark_theme_id")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _saved = MutableStateFlow(SavedTheme())
    val saved: StateFlow<SavedTheme> = _saved

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.themeDataStore.data.collect { prefs ->
                _saved.value =
                    SavedTheme(
                        lightThemeId = prefs[LIGHT_THEME_KEY] ?: "neko",
                        darkThemeId = prefs[DARK_THEME_KEY] ?: "neko",
                        themeMode =
                            prefs[THEME_MODE_KEY]
                                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                                ?: ThemeMode.DARK,
                    )
            }
        }
    }

    fun persist(theme: SavedTheme) {
        scope.launch {
            appContext.themeDataStore.edit { prefs ->
                prefs[LIGHT_THEME_KEY] = theme.lightThemeId
                prefs[DARK_THEME_KEY] = theme.darkThemeId
                prefs[THEME_MODE_KEY] = theme.themeMode.name
            }
        }
    }
}
