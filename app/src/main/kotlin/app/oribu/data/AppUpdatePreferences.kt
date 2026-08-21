package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appUpdateDataStore by preferencesDataStore(name = "app_update_prefs")

/** Só guarda o timestamp da última checagem silenciosa, pra aplicar o throttle de 24h. */
object AppUpdatePreferences {
    private val LAST_CHECK_KEY = longPreferencesKey("last_update_check")

    suspend fun getLastCheck(context: Context): Long = context.appUpdateDataStore.data.first()[LAST_CHECK_KEY] ?: 0L

    suspend fun setLastCheck(
        context: Context,
        timestampMs: Long,
    ) {
        context.appUpdateDataStore.edit { it[LAST_CHECK_KEY] = timestampMs }
    }
}
