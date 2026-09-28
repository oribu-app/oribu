package app.oribu.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import app.oribu.data.backup.BackupOptions
import app.oribu.data.backup.BackupPreferences
import app.oribu.data.backup.BackupService
import app.oribu.data.backup.BackupSummary
import app.oribu.data.backup.RestoreResult
import app.oribu.ui.locale.formatDate
import kotlinx.coroutines.launch

/** Result of the last backup/restore action, shown in place of the row's subtitle. */
sealed interface BackupOutcome {
    data class Saved(
        val fileName: String,
    ) : BackupOutcome

    data class Restored(
        val result: RestoreResult,
    ) : BackupOutcome

    data class Failed(
        val reason: BackupErrorReason,
    ) : BackupOutcome
}

class BackupViewModel : ViewModel() {
    var busy by mutableStateOf(false)
    var outcome by mutableStateOf<BackupOutcome?>(null)

    /** Backup file picked for restore, with what it holds, waiting for the user's confirmation. */
    var pendingRestore by mutableStateOf<Pair<Uri, BackupSummary>?>(null)

    fun backupNow(
        context: Context,
        options: BackupOptions,
    ) = launchAction {
        outcome = BackupOutcome.Saved(BackupService.backupToFolder(context, BackupKind.MANUAL, options))
    }

    fun inspect(
        context: Context,
        uri: Uri,
    ) = launchAction { pendingRestore = uri to BackupService.inspect(context, uri) }

    fun restore(
        context: Context,
        uri: Uri,
        onLibraryRestored: () -> Unit,
    ) = launchAction {
        val result = BackupService.restore(context, uri)
        outcome = BackupOutcome.Restored(result)
        if (!result.settingsRestored) onLibraryRestored()
    }

    private fun launchAction(block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                block()
            } catch (e: BackupException) {
                outcome = BackupOutcome.Failed(e.reason)
            }
            busy = false
        }
    }
}

/**
 * Settings → Data → Backup, modelled on Rokku's "Backup and restore": pick what goes into a manual
 * backup, restore from a file (with its contents shown first), and schedule automatic backups
 * (manual only, daily, every 2 days or weekly — never more than once a day) keeping up to 5.
 * Backups are plain JSON files in the user's own folder; nothing leaves the device.
 */
@Composable
internal fun BackupSection(
    onLibraryRestored: () -> Unit,
    vm: BackupViewModel = viewModel(),
) {
    val context = LocalContext.current
    val folderUri by StoragePreferences.folderUri.collectAsState()
    val frequency by BackupPreferences.frequency.collectAsState()
    val maxAutomatic by BackupPreferences.maxAutomatic.collectAsState()
    val lastAutoBackupMs by BackupPreferences.lastAutoBackupMs.collectAsState()
    var createDialogOpen by remember { mutableStateOf(false) }
    var frequencyDialogOpen by remember { mutableStateOf(false) }
    var maxDialogOpen by remember { mutableStateOf(false) }

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
            if (uri != null) vm.inspect(context, uri)
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
                outcome is BackupOutcome.Restored -> stringResource(R.string.backup_restored, outcome.result.itemCount)
                outcome is BackupOutcome.Failed -> backupErrorMessage(outcome.reason)
                else -> stringResource(R.string.backup_now_subtitle)
            },
        onClick = { if (folderUri == null) pickFolder.launch(null) else createDialogOpen = true },
        trailing = {
            if (vm.busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Backup, null)
            }
        },
    )

    SettingsClickRow(
        title = stringResource(R.string.backup_restore),
        subtitle = stringResource(R.string.backup_restore_subtitle),
        onClick = { pickBackupFile.launch(arrayOf("application/json", "*/*")) },
        trailing = { Icon(Icons.Default.Restore, null) },
    )

    SettingsClickRow(
        title = stringResource(R.string.backup_automatic),
        subtitle = stringResource(frequency.labelRes),
        onClick = { frequencyDialogOpen = true },
    )
    if (frequency != BackupFrequency.MANUAL) {
        SettingsClickRow(
            title = stringResource(R.string.backup_max_automatic),
            subtitle = maxAutomatic.toString(),
            onClick = { maxDialogOpen = true },
        )
    }

    Text(
        stringResource(R.string.backup_info) + "\n\n" +
            (
                lastAutoBackupMs?.let { stringResource(R.string.backup_last_automatic, formatDate(it)) }
                    ?: stringResource(R.string.backup_no_automatic_yet)
            ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    if (createDialogOpen) {
        CreateBackupDialog(
            onConfirm = { options ->
                createDialogOpen = false
                vm.backupNow(context, options)
            },
            onDismiss = { createDialogOpen = false },
        )
    }
    if (frequencyDialogOpen) {
        SingleChoiceDialog(
            title = stringResource(R.string.backup_automatic),
            options = BackupFrequency.entries.map { it to stringResource(it.labelRes) },
            selected = frequency,
            onSelect = { BackupPreferences.setFrequency(it) },
            onDismiss = { frequencyDialogOpen = false },
        )
    }
    if (maxDialogOpen) {
        SingleChoiceDialog(
            title = stringResource(R.string.backup_max_automatic),
            options = BackupPreferences.MAX_AUTOMATIC_CHOICES.map { it to it.toString() },
            selected = maxAutomatic,
            onSelect = { BackupPreferences.setMaxAutomatic(it) },
            onDismiss = { maxDialogOpen = false },
        )
    }

    vm.pendingRestore?.let { (uri, summary) ->
        RestoreConfirmDialog(
            summary = summary,
            onConfirm = {
                vm.pendingRestore = null
                vm.restore(context, uri, onLibraryRestored)
            },
            onDismiss = { vm.pendingRestore = null },
        )
    }

    if (outcome is BackupOutcome.Restored && outcome.result.settingsRestored) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Restore, null) },
            title = { Text(stringResource(R.string.backup_restart_title)) },
            text = { Text(stringResource(R.string.backup_restart_message)) },
            confirmButton = {
                Button(onClick = { BackupService.restartApp(context) }) { Text(stringResource(R.string.backup_restart_now)) }
            },
        )
    }
}

/** Rokku's "What do you want to back up?" checklist; entries tied to the library follow it. */
@Composable
private fun CreateBackupDialog(
    onConfirm: (BackupOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var options by remember { mutableStateOf(BackupOptions()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_create_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BackupOptions.entries.forEach { entry ->
                    val enabled = entry.enabled(options)
                    val checked = entry.getter(options) && enabled
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                enabled = enabled,
                                role = Role.Checkbox,
                                onValueChange = { options = entry.setter(options, it) },
                            ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(entry.label),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = options.library || options.appSettings || options.sensitive,
                onClick = { onConfirm(options) },
            ) { Text(stringResource(R.string.backup_create_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RestoreConfirmDialog(
    summary: BackupSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restore, null) },
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.backup_restore_summary, formatDate(summary.createdAtMs), summary.itemCount))
                if (summary.includesSettings) {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(stringResource(R.string.backup_restore_summary_settings))
                }
                if (summary.includesSensitive) {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(stringResource(R.string.backup_restore_summary_sensitive))
                }
                Spacer(Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.backup_restore_confirm_message))
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.backup_restore_confirm_button)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
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
