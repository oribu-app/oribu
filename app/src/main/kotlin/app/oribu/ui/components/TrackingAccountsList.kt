package app.oribu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.oribu.R
import app.oribu.data.ApiKeyPreferences
import app.oribu.service.AniListService
import app.oribu.service.ApiServices
import app.oribu.service.RetroAchievementsService
import app.oribu.service.SteamService
import kotlinx.coroutines.launch

/**
 * Accounts that sync the user's own progress (Settings → Tracking), grouped by hobby. Same
 * model as Tonkatsu Box's import/sync screens: these always use the user's own credentials,
 * never a built-in key. A credential that a search also needs (Hardcover) stays in
 * [ServiceCredentialsList] and is only reused from here.
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
        CredentialSectionHeader(stringResource(R.string.games_title))

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

        CredentialSectionHeader(stringResource(R.string.tracking_section_manga))

        ServiceCredentialCard(
            name = "AniList",
            description = stringResource(R.string.service_anilist_tracking_desc),
            logo = ServiceLogo.ANILIST,
            status = aniListStatus,
            onTest = {
                scope.testCredential({ aniListStatus = it }) { AniListService().testUser(overrides.anilistUsername.orEmpty()) }
            },
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
    }
}
