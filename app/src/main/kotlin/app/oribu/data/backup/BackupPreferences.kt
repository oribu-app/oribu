package app.oribu.data.backup

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.oribu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class BackupFrequency(
    val prefValue: String,
    @StringRes val labelRes: Int,
    val intervalHours: Long?,
) {
    OFF("off", R.string.backup_frequency_off, null),
    DAILY("daily", R.string.backup_frequency_daily, 24),
    WEEKLY("weekly", R.string.backup_frequency_weekly, 24 * 7),
    ;

    companion object {
        fun fromPref(value: String?): BackupFrequency = entries.firstOrNull { it.prefValue == value } ?: WEEKLY
    }
}

private val Context.backupDataStore by preferencesDataStore(name = "backup_prefs")

/** Automatic backup schedule and when the last backup (of any kind) was written. */
object BackupPreferences {
    private val FREQUENCY_KEY = stringPreferencesKey("frequency")
    private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_ms")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _frequency = MutableStateFlow(BackupFrequency.WEEKLY)
    val frequency: StateFlow<BackupFrequency> = _frequency

    private val _lastBackupMs = MutableStateFlow<Long?>(null)
    val lastBackupMs: StateFlow<Long?> = _lastBackupMs

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.backupDataStore.data.collect { prefs ->
                _frequency.value = BackupFrequency.fromPref(prefs[FREQUENCY_KEY])
                _lastBackupMs.value = prefs[LAST_BACKUP_KEY]
            }
        }
    }

    fun setFrequency(value: BackupFrequency) {
        _frequency.value = value
        scope.launch { appContext.backupDataStore.edit { it[FREQUENCY_KEY] = value.prefValue } }
    }

    fun markBackup(timeMs: Long) {
        _lastBackupMs.value = timeMs
        scope.launch { appContext.backupDataStore.edit { it[LAST_BACKUP_KEY] = timeMs } }
    }
}
