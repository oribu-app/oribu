package app.oribu.data.backup

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

/** Rokku's backup frequency list minus its 6h/12h options — at most one automatic backup a day. */
enum class BackupFrequency(
    val prefValue: String,
    @StringRes val labelRes: Int,
    val intervalHours: Long?,
) {
    MANUAL("manual", R.string.backup_frequency_manual, null),
    DAILY("daily", R.string.backup_frequency_daily, 24),
    EVERY_2_DAYS("every_2_days", R.string.backup_frequency_every_2_days, 48),
    WEEKLY("weekly", R.string.backup_frequency_weekly, 24 * 7),
    ;

    companion object {
        fun fromPref(value: String?): BackupFrequency = entries.firstOrNull { it.prefValue == value } ?: WEEKLY
    }
}

private val Context.backupDataStore by preferencesDataStore(name = "backup_prefs")

/** Automatic backup schedule, how many automatic backups to keep, and when the last ones ran. */
object BackupPreferences {
    /** Rokku's "Max automatic backups" range. */
    val MAX_AUTOMATIC_CHOICES = 1..5

    private val FREQUENCY_KEY = stringPreferencesKey("frequency")
    private val MAX_AUTOMATIC_KEY = intPreferencesKey("max_automatic")
    private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_ms")
    private val LAST_AUTO_BACKUP_KEY = longPreferencesKey("last_auto_backup_ms")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _frequency = MutableStateFlow(BackupFrequency.WEEKLY)
    val frequency: StateFlow<BackupFrequency> = _frequency

    private val _maxAutomatic = MutableStateFlow(MAX_AUTOMATIC_CHOICES.last)
    val maxAutomatic: StateFlow<Int> = _maxAutomatic

    private val _lastBackupMs = MutableStateFlow<Long?>(null)
    val lastBackupMs: StateFlow<Long?> = _lastBackupMs

    private val _lastAutoBackupMs = MutableStateFlow<Long?>(null)
    val lastAutoBackupMs: StateFlow<Long?> = _lastAutoBackupMs

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.backupDataStore.data.collect { prefs ->
                _frequency.value = BackupFrequency.fromPref(prefs[FREQUENCY_KEY])
                _maxAutomatic.value = (prefs[MAX_AUTOMATIC_KEY] ?: MAX_AUTOMATIC_CHOICES.last).coerceIn(MAX_AUTOMATIC_CHOICES)
                _lastBackupMs.value = prefs[LAST_BACKUP_KEY]
                _lastAutoBackupMs.value = prefs[LAST_AUTO_BACKUP_KEY]
            }
        }
    }

    fun setFrequency(value: BackupFrequency) {
        _frequency.value = value
        scope.launch { appContext.backupDataStore.edit { it[FREQUENCY_KEY] = value.prefValue } }
    }

    fun setMaxAutomatic(value: Int) {
        _maxAutomatic.value = value
        scope.launch { appContext.backupDataStore.edit { it[MAX_AUTOMATIC_KEY] = value } }
    }

    fun markBackup(
        timeMs: Long,
        automatic: Boolean,
    ) {
        _lastBackupMs.value = timeMs
        if (automatic) _lastAutoBackupMs.value = timeMs
        scope.launch {
            appContext.backupDataStore.edit {
                it[LAST_BACKUP_KEY] = timeMs
                if (automatic) it[LAST_AUTO_BACKUP_KEY] = timeMs
            }
        }
    }
}
