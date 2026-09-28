package app.oribu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.oribu.R
import app.oribu.data.ApiKeyPreferences
import app.oribu.data.backup.BackupException
import app.oribu.service.AniListService
import app.oribu.service.ApiServices
import app.oribu.service.RetroAchievementsService
import app.oribu.service.SteamService
import app.oribu.service.sync.SyncResult
import app.oribu.service.sync.SyncService
import app.oribu.service.sync.TrackingHobby
import app.oribu.service.sync.TrackingIntegration
import app.oribu.service.sync.TrackingPreferences
import app.oribu.ui.locale.formatDate
import kotlinx.coroutines.launch

/**
 * Accounts that sync the user's own progress (Settings → Tracking), grouped by hobby. Same
 * model as Tonkatsu Box's import/sync screens: these always use the user's own credentials,
 * never a built-in key. A credential that a search also needs (Hardcover) stays in
 * [ServiceCredentialsList] and is only reused from here.
 *
 * Each connected integration can update automatically (with the daily refresh) or only when the
 * user taps "Sync now", which takes a backup first when a backup folder is set.
 */
@Composable
fun TrackingAccountsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overrides by ApiKeyPreferences.overrides.collectAsState()
    val scope = rememberCoroutineScope()

    fun reload() = scope.launch { ApiServices.reload(context) }

    var steamStatus by remember { mutableStateOf(credentialStatusFor(listOf(overrides.steamId, overrides.steamApiKey))) }
    var retroAchievementsStatus by remember {
        mutableStateOf(credentialStatusFor(listOf(overrides.retroAchievementsUsername, overrides.retroAchievementsApiKey)))
    }
    var aniListStatus by remember { mutableStateOf(credentialStatusFor(listOf(overrides.anilistUsername))) }

    Column(modifier) {
        CredentialSectionHeader(stringResource(TrackingHobby.GAMES.labelRes))

        ServiceCredentialCard(
            name = "Steam",
            description = stringResource(R.string.service_steam_desc),
            logo = ServiceLogo.STEAM,
            status = steamStatus,
            onTest = {
                scope.testCredential({ steamStatus = it }) {
                    SteamService(overrides.steamApiKey.orEmpty(), overrides.steamId.orEmpty()).testConnection()
                }
            },
            footer = { SyncControls(TrackingIntegration.STEAM, connected = steamStatus.isConnected) },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_steam_id_64),
                secret = false,
                value = overrides.steamId.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamId(value)
                    steamStatus = credentialStatusFor(listOf(value, overrides.steamApiKey))
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.steamApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamApiKey(value)
                    steamStatus = credentialStatusFor(listOf(overrides.steamId, value))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "RetroAchievements",
            description = stringResource(R.string.service_retroachievements_desc),
            logo = ServiceLogo.RETROACHIEVEMENTS,
            status = retroAchievementsStatus,
            onTest = {
                scope.testCredential({ retroAchievementsStatus = it }) {
                    RetroAchievementsService(
                        overrides.retroAchievementsUsername.orEmpty(),
                        overrides.retroAchievementsApiKey.orEmpty(),
                    ).testConnection()
                }
            },
            footer = { SyncControls(TrackingIntegration.RETROACHIEVEMENTS, connected = retroAchievementsStatus.isConnected) },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_username),
                secret = false,
                value = overrides.retroAchievementsUsername.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setRetroAchievementsUsername(value)
                    retroAchievementsStatus = credentialStatusFor(listOf(value, overrides.retroAchievementsApiKey))
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.retroAchievementsApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setRetroAchievementsApiKey(value)
                    retroAchievementsStatus = credentialStatusFor(listOf(overrides.retroAchievementsUsername, value))
                    reload()
                },
            )
        }

        CredentialSectionHeader(stringResource(TrackingHobby.ANIME_MANGA.labelRes))

        ServiceCredentialCard(
            name = "AniList",
            description = stringResource(R.string.service_anilist_tracking_desc),
            logo = ServiceLogo.ANILIST,
            status = aniListStatus,
            onTest = {
                scope.testCredential({ aniListStatus = it }) { AniListService().testUser(overrides.anilistUsername.orEmpty()) }
            },
            footer = { SyncControls(TrackingIntegration.ANILIST, connected = aniListStatus.isConnected) },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_username),
                secret = false,
                value = overrides.anilistUsername.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setAnilistUsername(value)
                    aniListStatus = credentialStatusFor(listOf(value))
                    reload()
                },
            )
        }

        CredentialSectionHeader(stringResource(TrackingHobby.MOVIES_SERIES.labelRes))
        NoIntegrationsYet()

        CredentialSectionHeader(stringResource(TrackingHobby.BOOKS.labelRes))
        NoIntegrationsYet()
    }
}

private val CredentialStatus.isConnected
    get() = this == CredentialStatus.CONFIGURED || this == CredentialStatus.VALID

/** Result of the last "Sync now" on a card, shown instead of the last-sync time. */
private sealed interface SyncOutcome {
    data class Done(
        val result: SyncResult,
    ) : SyncOutcome

    data object Failed : SyncOutcome
}

@Composable
private fun ColumnScope.SyncControls(
    integration: TrackingIntegration,
    connected: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoSync by TrackingPreferences.autoSync.collectAsState()
    val lastSyncMs by TrackingPreferences.lastSyncMs.collectAsState()
    var syncing by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<SyncOutcome?>(null) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.tracking_auto_sync),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = autoSync[integration] ?: true,
            onCheckedChange = { TrackingPreferences.setAutoSync(integration, it) },
            enabled = connected,
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val current = outcome
        val lastSync = lastSyncMs[integration]
        Text(
            when {
                syncing -> {
                    stringResource(R.string.tracking_syncing)
                }

                current is SyncOutcome.Done && !current.result.backupTaken -> {
                    stringResource(R.string.tracking_synced_without_backup, current.result.itemsSynced)
                }

                current is SyncOutcome.Done -> {
                    stringResource(R.string.tracking_synced, current.result.itemsSynced)
                }

                current is SyncOutcome.Failed -> {
                    stringResource(R.string.tracking_sync_failed)
                }

                lastSync != null -> {
                    stringResource(R.string.tracking_last_sync, formatDate(lastSync))
                }

                else -> {
                    stringResource(R.string.tracking_never_synced)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
        )
        if (syncing) {
            CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(18.dp), strokeWidth = 2.dp)
        } else {
            TextButton(
                enabled = connected,
                onClick = {
                    syncing = true
                    scope.launch {
                        outcome =
                            try {
                                SyncOutcome.Done(SyncService.syncNow(context, integration))
                            } catch (_: BackupException) {
                                SyncOutcome.Failed
                            }
                        syncing = false
                    }
                },
            ) { Text(stringResource(R.string.tracking_sync_now)) }
        }
    }
}

@Composable
private fun NoIntegrationsYet() {
    Text(
        stringResource(R.string.tracking_no_integrations_yet),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
