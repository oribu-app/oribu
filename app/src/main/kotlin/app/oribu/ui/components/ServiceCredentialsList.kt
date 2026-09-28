package app.oribu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import app.oribu.service.ApiServices
import app.oribu.service.GoogleBooksService
import app.oribu.service.HardcoverService
import app.oribu.service.IgdbAuthService
import app.oribu.service.ItadService
import app.oribu.service.RetroAchievementsService
import app.oribu.service.Secrets
import app.oribu.service.SteamGridDbService
import app.oribu.service.SteamService
import app.oribu.service.TmdbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lista de serviços externos com campo de credencial + "Testar conexão" + indicador de status,
 * no molde do tonkatsu_box. Componente puro (sem Scaffold própria) para poder ser embutido tanto
 * na tela de Configurações → Integrações quanto no passo de chaves de API do onboarding.
 *
 * Release builds ship app-level keys in `secrets.json` (written by CI), so search/metadata work
 * out of the box — a key typed here only replaces the built-in one. Per-user identifiers
 * (SteamID, RetroAchievements username, Hardcover token) are never bundled.
 */
@Composable
fun ServiceCredentialsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overrides by ApiKeyPreferences.overrides.collectAsState()
    val builtIn = remember { Secrets.load(context) }
    val scope = rememberCoroutineScope()

    /**
     * [user] = identifiers only the user can provide, [own] = the user's own key(s),
     * [bundled] = the built-in key(s) that stand in when [own] is empty.
     */
    fun statusFor(
        own: List<String?>,
        bundled: List<String?> = emptyList(),
        user: List<String?> = emptyList(),
    ): CredentialStatus =
        when {
            user.any { it.isNullOrBlank() } -> CredentialStatus.NOT_CONFIGURED
            own.none { it.isNullOrBlank() } -> CredentialStatus.CONFIGURED
            bundled.isNotEmpty() && bundled.none { it.isNullOrBlank() } -> CredentialStatus.BUILT_IN
            else -> CredentialStatus.NOT_CONFIGURED
        }

    fun effective(
        own: String?,
        bundled: String?,
    ): String = own?.takeIf { it.isNotBlank() } ?: bundled.orEmpty()

    var tmdbStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.tmdbApiKey), listOf(builtIn.tmdbBearerToken)))
    }
    var igdbStatus by remember {
        mutableStateOf(
            statusFor(
                listOf(overrides.igdbClientId, overrides.igdbClientSecret),
                listOf(builtIn.igdbClientId, builtIn.igdbClientSecret),
            ),
        )
    }
    var steamGridDbStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.steamGridDbApiKey), listOf(builtIn.steamGridDbApiKey)))
    }
    var itadStatus by remember { mutableStateOf(statusFor(listOf(overrides.itadApiKey), listOf(builtIn.itadApiKey))) }
    var googleBooksStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.googleBooksApiKey), listOf(builtIn.googleBooksApiKey)))
    }
    var steamStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.steamApiKey), listOf(builtIn.steamApiKey), user = listOf(overrides.steamId)))
    }
    var retroAchievementsStatus by remember {
        mutableStateOf(
            statusFor(
                listOf(overrides.retroAchievementsApiKey),
                listOf(builtIn.retroAchievementsApiKey),
                user = listOf(overrides.retroAchievementsUsername),
            ),
        )
    }
    var hardcoverStatus by remember { mutableStateOf(statusFor(listOf(overrides.hardcoverApiToken))) }

    fun reload() = scope.launch { ApiServices.reload(context) }

    fun test(
        setStatus: (CredentialStatus) -> Unit,
        check: () -> Unit,
    ) {
        setStatus(CredentialStatus.TESTING)
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { check() } }.isSuccess
            setStatus(if (ok) CredentialStatus.VALID else CredentialStatus.INVALID)
        }
    }

    Column(modifier) {
        SectionHeader(stringResource(R.string.service_section_metadata))

        ServiceCredentialCard(
            name = "TMDB",
            description = stringResource(R.string.service_tmdb_desc),
            status = tmdbStatus,
            usingBuiltIn = overrides.tmdbApiKey.isNullOrBlank() && !builtIn.tmdbBearerToken.isNullOrBlank(),
            onTest = {
                test({ tmdbStatus = it }) {
                    TmdbService(effective(overrides.tmdbApiKey, builtIn.tmdbBearerToken)).testConnection()
                }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_bearer_token),
                value = overrides.tmdbApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setTmdbApiKey(value)
                    tmdbStatus = statusFor(listOf(value), listOf(builtIn.tmdbBearerToken))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "IGDB",
            description = stringResource(R.string.service_igdb_desc),
            status = igdbStatus,
            usingBuiltIn =
                (overrides.igdbClientId.isNullOrBlank() || overrides.igdbClientSecret.isNullOrBlank()) &&
                    builtIn.igdbConfigurado,
            onTest = {
                test({ igdbStatus = it }) {
                    val ownComplete = !overrides.igdbClientId.isNullOrBlank() && !overrides.igdbClientSecret.isNullOrBlank()
                    IgdbAuthService.getAccessToken(
                        context,
                        if (ownComplete) overrides.igdbClientId.orEmpty() else builtIn.igdbClientId.orEmpty(),
                        if (ownComplete) overrides.igdbClientSecret.orEmpty() else builtIn.igdbClientSecret.orEmpty(),
                    )
                }
            },
        ) {
            val bundled = listOf(builtIn.igdbClientId, builtIn.igdbClientSecret)
            InlineKeyField(
                label = stringResource(R.string.credential_field_client_id),
                value = overrides.igdbClientId.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setIgdbClientId(value)
                    igdbStatus = statusFor(listOf(value, overrides.igdbClientSecret), bundled)
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = stringResource(R.string.credential_field_client_secret),
                value = overrides.igdbClientSecret.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setIgdbClientSecret(value)
                    igdbStatus = statusFor(listOf(overrides.igdbClientId, value), bundled)
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "SteamGridDB",
            description = stringResource(R.string.service_steamgriddb_desc),
            status = steamGridDbStatus,
            usingBuiltIn = overrides.steamGridDbApiKey.isNullOrBlank() && !builtIn.steamGridDbApiKey.isNullOrBlank(),
            onTest = {
                test({ steamGridDbStatus = it }) {
                    SteamGridDbService(effective(overrides.steamGridDbApiKey, builtIn.steamGridDbApiKey)).testConnection()
                }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.steamGridDbApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamGridDbApiKey(value)
                    steamGridDbStatus = statusFor(listOf(value), listOf(builtIn.steamGridDbApiKey))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "ITAD",
            description = stringResource(R.string.service_itad_desc),
            status = itadStatus,
            usingBuiltIn = overrides.itadApiKey.isNullOrBlank() && !builtIn.itadApiKey.isNullOrBlank(),
            onTest = {
                test({ itadStatus = it }) { ItadService(effective(overrides.itadApiKey, builtIn.itadApiKey)).testConnection() }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.itadApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setItadApiKey(value)
                    itadStatus = statusFor(listOf(value), listOf(builtIn.itadApiKey))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "Google Books",
            description = stringResource(R.string.service_google_books_desc),
            status = googleBooksStatus,
            usingBuiltIn = overrides.googleBooksApiKey.isNullOrBlank() && !builtIn.googleBooksApiKey.isNullOrBlank(),
            onTest = {
                test({ googleBooksStatus = it }) {
                    GoogleBooksService(effective(overrides.googleBooksApiKey, builtIn.googleBooksApiKey).ifBlank { null })
                        .testConnection()
                }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.googleBooksApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setGoogleBooksApiKey(value)
                    googleBooksStatus = statusFor(listOf(value), listOf(builtIn.googleBooksApiKey))
                    reload()
                },
            )
        }

        SectionHeader(stringResource(R.string.service_section_tracking))

        ServiceCredentialCard(
            name = "Steam",
            description = stringResource(R.string.service_steam_desc),
            status = steamStatus,
            usingBuiltIn = overrides.steamApiKey.isNullOrBlank() && !builtIn.steamApiKey.isNullOrBlank(),
            onTest = {
                test({ steamStatus = it }) {
                    SteamService(effective(overrides.steamApiKey, builtIn.steamApiKey), overrides.steamId.orEmpty()).testConnection()
                }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_steam_id_64),
                value = overrides.steamId.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamId(value)
                    steamStatus = statusFor(listOf(overrides.steamApiKey), listOf(builtIn.steamApiKey), user = listOf(value))
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = ownKeyLabel(hasBuiltIn = !builtIn.steamApiKey.isNullOrBlank()),
                value = overrides.steamApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamApiKey(value)
                    steamStatus = statusFor(listOf(value), listOf(builtIn.steamApiKey), user = listOf(overrides.steamId))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "RetroAchievements",
            description = stringResource(R.string.service_retroachievements_desc),
            status = retroAchievementsStatus,
            usingBuiltIn = overrides.retroAchievementsApiKey.isNullOrBlank() && !builtIn.retroAchievementsApiKey.isNullOrBlank(),
            onTest = {
                test({ retroAchievementsStatus = it }) {
                    RetroAchievementsService(
                        overrides.retroAchievementsUsername.orEmpty(),
                        effective(overrides.retroAchievementsApiKey, builtIn.retroAchievementsApiKey),
                    ).testConnection()
                }
            },
        ) {
            val bundled = listOf(builtIn.retroAchievementsApiKey)
            InlineKeyField(
                label = stringResource(R.string.credential_field_username),
                value = overrides.retroAchievementsUsername.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setRetroAchievementsUsername(value)
                    retroAchievementsStatus = statusFor(listOf(overrides.retroAchievementsApiKey), bundled, user = listOf(value))
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = ownKeyLabel(hasBuiltIn = !builtIn.retroAchievementsApiKey.isNullOrBlank()),
                value = overrides.retroAchievementsApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setRetroAchievementsApiKey(value)
                    retroAchievementsStatus =
                        statusFor(listOf(value), bundled, user = listOf(overrides.retroAchievementsUsername))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "Hardcover",
            description = stringResource(R.string.service_hardcover_desc),
            status = hardcoverStatus,
            usingBuiltIn = false,
            onTest = {
                test({ hardcoverStatus = it }) { HardcoverService(overrides.hardcoverApiToken.orEmpty()).testConnection() }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.hardcoverApiToken.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setHardcoverApiToken(value)
                    hardcoverStatus = statusFor(listOf(value))
                    reload()
                },
            )
        }

        Text(
            stringResource(R.string.service_anilist_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ownKeyLabel(hasBuiltIn: Boolean): String =
    stringResource(if (hasBuiltIn) R.string.credential_field_api_key_optional else R.string.credential_field_api_key)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ServiceCredentialCard(
    name: String,
    description: String,
    status: CredentialStatus,
    usingBuiltIn: Boolean,
    onTest: () -> Unit,
    fields: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                StatusDot(status)
            }
            if (usingBuiltIn) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.service_built_in_key_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            fields()
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onTest, enabled = status != CredentialStatus.TESTING) {
                Text(stringResource(R.string.service_test_connection))
            }
        }
    }
}
