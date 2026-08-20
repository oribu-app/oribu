package app.oribu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.storageDataStore by preferencesDataStore(name = "storage_prefs")

/**
 * Guarda a URI (com permissão persistente já concedida) da pasta escolhida no passo de
 * armazenamento do onboarding. Só a preferência é salva aqui — nenhuma função de backup usa
 * isso ainda.
 */
object StoragePreferences {
    private val FOLDER_URI_KEY = stringPreferencesKey("folder_uri")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _folderUri = MutableStateFlow<String?>(null)
    val folderUri: StateFlow<String?> = _folderUri

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.storageDataStore.data
                .map { prefs -> prefs[FOLDER_URI_KEY] }
                .collect { _folderUri.value = it }
        }
    }

    fun setFolder(uri: String) {
        _folderUri.value = uri
        scope.launch {
            appContext.storageDataStore.edit { prefs -> prefs[FOLDER_URI_KEY] = uri }
        }
    }
}
