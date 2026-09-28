package app.oribu.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oribu.R
import app.oribu.data.StoragePreferences
import app.oribu.data.backup.BackupErrorReason
import app.oribu.data.backup.BackupException
import app.oribu.data.backup.BackupFrequency
import app.oribu.data.backup.BackupKind
import app.oribu.data.backup.BackupPreferences
import app.oribu.data.backup.BackupService
import app.oribu.ui.locale.formatDate
import kotlinx.coroutines.launch

/** Result of the last backup/restore action, shown in place of the row's subtitle. */
sealed interface BackupOutcome {
    data class Saved(
        val fileName: String,
    ) : BackupOutcome

    data class Restored(
        val items: Int,
    ) : BackupOutcome

    data class Failed(
        val reason: BackupErrorReason,
    ) : BackupOutcome
}

class BackupViewModel : ViewModel() {
    var busy by mutableStateOf(false)
    var outcome by mutableStateOf<BackupOutcome?>(null)

    fun backupNow(context: Context) =
        launchAction {
            BackupOutcome.Saved(BackupService.backupToFolder(context, BackupKind.MANUAL))
        }

    fun restore(
        context: Context,
        uri: Uri,
        onRestored: () -> Unit,
    ) = launchAction {
        BackupOutcome.Restored(BackupService.restore(context, uri)).also { onRestored() }
    }

    private fun launchAction(block: suspend () -> BackupOutcome) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            outcome =
                try {
                    block()
                } catch (e: BackupException) {
                    BackupOutcome.Failed(e.reason)
                }
            busy = false
        }
    }
}

/**
 * Settings → Data → Backup: folder, manual backup, automatic schedule and restore. Backups are
 * plain JSON files in the user's own folder — nothing leaves the device.
 */
@Composable
internal fun BackupSection(
    onRestored: () -> Unit,
    vm: BackupViewModel = viewModel(),
) {
    val context = LocalContext.current
    val folderUri by StoragePreferences.folderUri.collectAsState()
    val frequency by BackupPreferences.frequency.collectAsState()
    val lastBackupMs by BackupPreferences.lastBackupMs.collectAsState()
    var frequencyDialogOpen by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val pickFolder =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                StoragePreferences.setFolder(uri.toString())
            }
        }
    val pickBackupFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) pendingRestore = uri
        }

    SettingsSectionHeader(stringResource(R.string.backup_section), tinted = true)

    SettingsClickRow(
        title = stringResource(R.string.backup_folder),
        subtitle = folderUri?.let { readableFolder(it) } ?: stringResource(R.string.backup_folder_none),
        onClick = { pickFolder.launch(null) },
    )

    val outcome = vm.outcome
    SettingsClickRow(
        title = stringResource(R.string.backup_now),
        subtitle =
            when {
                vm.busy -> stringResource(R.string.backup_working)
                outcome is BackupOutcome.Saved -> stringResource(R.string.backup_saved, outcome.fileName)
                outcome is BackupOutcome.Restored -> stringResource(R.string.backup_restored, outcome.items)
                outcome is BackupOutcome.Failed -> backupErrorMessage(outcome.reason)
                lastBackupMs != null -> stringResource(R.string.backup_last, formatDate(lastBackupMs!!))
                else -> stringResource(R.string.backup_never)
            },
        onClick = { if (folderUri == null) pickFolder.launch(null) else vm.backupNow(context) },
        trailing = {
            if (vm.busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Backup, null)
            }
        },
    )

    SettingsClickRow(
        title = stringResource(R.string.backup_automatic),
        subtitle = stringResource(frequency.labelRes),
        onClick = { frequencyDialogOpen = true },
    )

    SettingsClickRow(
        title = stringResource(R.string.backup_restore),
        subtitle = stringResource(R.string.backup_restore_subtitle),
        onClick = { pickBackupFile.launch(arrayOf("application/json", "*/*")) },
        trailing = { Icon(Icons.Default.Restore, null) },
    )

    if (frequencyDialogOpen) {
        SingleChoiceDialog(
            title = stringResource(R.string.backup_automatic),
            options = BackupFrequency.entries.map { it to stringResource(it.labelRes) },
            selected = frequency,
            onSelect = { BackupPreferences.setFrequency(it) },
            onDismiss = { frequencyDialogOpen = false },
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            icon = { Icon(Icons.Default.Restore, null) },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    pendingRestore = null
                    vm.restore(context, uri, onRestored)
                }) { Text(stringResource(R.string.backup_restore_confirm_button)) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun backupErrorMessage(reason: BackupErrorReason): String =
    stringResource(
        when (reason) {
            BackupErrorReason.INVALID_FILE -> R.string.backup_error_invalid_file
            BackupErrorReason.NEWER_SCHEMA -> R.string.backup_error_newer_schema
            BackupErrorReason.NO_FOLDER -> R.string.backup_error_no_folder
            BackupErrorReason.WRITE_FAILED -> R.string.backup_error_write_failed
        },
    )

/** "content://…/tree/primary%3ADocuments%2FOribu" → "Documents/Oribu". */
private fun readableFolder(uri: String): String =
    Uri
        .parse(uri)
        .lastPathSegment
        ?.substringAfter(':')
        ?.ifBlank { null } ?: uri
