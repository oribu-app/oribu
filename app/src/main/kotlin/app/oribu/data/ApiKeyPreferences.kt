package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ApiKeyOverrides(
    val tmdbApiKey: String? = null,
    val igdbClientId: String? = null,
    val igdbClientSecret: String? = null,
    val googleBooksApiKey: String? = null,
    val steamApiKey: String? = null,
    val steamId: String? = null,
    val itadApiKey: String? = null,
)

private val Context.apiKeyDataStore by preferencesDataStore(name = "api_key_prefs")

/**
 * Chaves de API cadastradas pelo próprio usuário (onboarding ou Configurações → Integrações).
 * Têm prioridade sobre o `secrets.json` embutido no APK — ver `Secrets.merge`.
 */
object ApiKeyPreferences {
    private val TMDB_KEY = stringPreferencesKey("tmdb_api_key")
    private val IGDB_CLIENT_ID_KEY = stringPreferencesKey("igdb_client_id")
    private val IGDB_CLIENT_SECRET_KEY = stringPreferencesKey("igdb_client_secret")
    private val GOOGLE_BOOKS_KEY = stringPreferencesKey("google_books_api_key")
    private val STEAM_API_KEY = stringPreferencesKey("steam_api_key")
    private val STEAM_ID_KEY = stringPreferencesKey("steam_id")
    private val ITAD_KEY = stringPreferencesKey("itad_api_key")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _overrides = MutableStateFlow(ApiKeyOverrides())
    val overrides: StateFlow<ApiKeyOverrides> = _overrides

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.apiKeyDataStore.data.collect { prefs ->
                _overrides.value =
                    ApiKeyOverrides(
                        tmdbApiKey = prefs[TMDB_KEY],
                        igdbClientId = prefs[IGDB_CLIENT_ID_KEY],
                        igdbClientSecret = prefs[IGDB_CLIENT_SECRET_KEY],
                        googleBooksApiKey = prefs[GOOGLE_BOOKS_KEY],
                        steamApiKey = prefs[STEAM_API_KEY],
                        steamId = prefs[STEAM_ID_KEY],
                        itadApiKey = prefs[ITAD_KEY],
                    )
            }
        }
    }

    /**
     * Lê o valor mais atual direto do DataStore, sem depender do `StateFlow` já ter emitido —
     * usada por `ApiServices` para não correr risco de aplicar chaves salvas em uma sessão
     * anterior usando o valor padrão (vazio) por causa de uma corrida com a leitura assíncrona.
     */
    suspend fun awaitOverrides(): ApiKeyOverrides {
        val prefs = appContext.apiKeyDataStore.data.first()
        return ApiKeyOverrides(
            tmdbApiKey = prefs[TMDB_KEY],
            igdbClientId = prefs[IGDB_CLIENT_ID_KEY],
            igdbClientSecret = prefs[IGDB_CLIENT_SECRET_KEY],
            googleBooksApiKey = prefs[GOOGLE_BOOKS_KEY],
            steamApiKey = prefs[STEAM_API_KEY],
            steamId = prefs[STEAM_ID_KEY],
            itadApiKey = prefs[ITAD_KEY],
        )
    }

    fun setTmdbApiKey(value: String) = setValue(TMDB_KEY, value)

    fun setIgdbClientId(value: String) = setValue(IGDB_CLIENT_ID_KEY, value)

    fun setIgdbClientSecret(value: String) = setValue(IGDB_CLIENT_SECRET_KEY, value)

    fun setGoogleBooksApiKey(value: String) = setValue(GOOGLE_BOOKS_KEY, value)

    fun setSteamApiKey(value: String) = setValue(STEAM_API_KEY, value)

    fun setSteamId(value: String) = setValue(STEAM_ID_KEY, value)

    fun setItadApiKey(value: String) = setValue(ITAD_KEY, value)

    private fun setValue(
        key: Preferences.Key<String>,
        value: String,
    ) {
        scope.launch {
            appContext.apiKeyDataStore.edit { prefs ->
                if (value.isBlank()) prefs.remove(key) else prefs[key] = value
            }
        }
    }
}
