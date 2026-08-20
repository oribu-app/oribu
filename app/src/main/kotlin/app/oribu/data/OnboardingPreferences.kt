package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

/**
 * Se o fluxo de primeiro acesso já foi concluído. `completed` começa em `null` (ainda
 * carregando do disco) — a splash screen usa isso para não liberar a tela antes de saber
 * se deve abrir o onboarding ou ir direto para Home.
 */
object OnboardingPreferences {
    private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _completed = MutableStateFlow<Boolean?>(null)
    val completed: StateFlow<Boolean?> = _completed

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.onboardingDataStore.data
                .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
                .collect { _completed.value = it }
        }
    }

    fun setCompleted() {
        _completed.value = true
        scope.launch {
            appContext.onboardingDataStore.edit { prefs -> prefs[ONBOARDING_COMPLETE_KEY] = true }
        }
    }
}
