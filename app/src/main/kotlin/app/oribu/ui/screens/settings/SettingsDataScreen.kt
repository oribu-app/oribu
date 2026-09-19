package app.oribu.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.data.db.DB
import app.oribu.service.MediaCacheService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsDataViewModel : ViewModel() {
    var updatingCache by mutableStateOf(false)
    var updateDone by mutableStateOf(false)

    fun updateAllCache() {
        if (updatingCache) return
        viewModelScope.launch {
            updatingCache = true
            updateDone = false
            val items = withContext(Dispatchers.IO) { DB.repo.getAll() }
            items.filter { it.externalId != null }.forEach { item ->
                MediaCacheService.fetchAndPersist(item)
            }
            updatingCache = false
            updateDone = true
        }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            DB.repo.clearAll()
            DB.cache.deleteAll()
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}

@Composable
fun SettingsDataScreen(
    navController: NavController,
    vm: SettingsDataViewModel = viewModel(),
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    SettingsScaffold(stringResource(R.string.settings_data_title), navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SettingsClickRow(
                title = stringResource(R.string.settings_data_update_all),
                subtitle =
                    when {
                        vm.updatingCache -> stringResource(R.string.settings_data_updating)
                        vm.updateDone -> stringResource(R.string.settings_data_update_success)
                        else -> stringResource(R.string.settings_data_sync_subtitle)
                    },
                onClick = { vm.updateAllCache() },
                trailing = {
                    if (vm.updatingCache) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { vm.updateAllCache() }, enabled = !vm.updatingCache) {
                            Icon(Icons.Default.Sync, null)
                        }
                    }
                },
            )

            SettingsClickRow(
                title = stringResource(R.string.settings_data_delete_all),
                subtitle = stringResource(R.string.settings_data_delete_all_subtitle),
                onClick = { showDeleteDialog = true },
                titleColor = MaterialTheme.colorScheme.error,
                trailing = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.settings_data_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.settings_data_delete_confirm_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        vm.clearAllData { navController.popBackStack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.settings_data_delete_confirm_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
