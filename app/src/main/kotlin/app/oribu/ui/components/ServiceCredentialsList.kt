package app.oribu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * Lista de serviços externos com campo de credencial, status e "Testar", no molde do
 * tonkatsu_box. Componente puro (sem Scaffold própria) para poder ser embutido tanto na tela de
 * Configurações → Integrações quanto no passo de chaves de API do onboarding.
 *
 * Same model as Tonkatsu Box: release builds ship app-level keys for search/metadata (written
 * by CI), which are never shown — the field stays empty with a "built-in key" placeholder, a
 * key typed here replaces it and the reset button goes back to it. Account integrations
 * (Steam, RetroAchievements, Hardcover) always use the user's own credentials.
 */
@Composable
fun ServiceCredentialsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overrides by ApiKeyPreferences.overrides.collectAsState()
    val builtIn = remember { Secrets.load(context) }
    val scope = rememberCoroutineScope()

    /** [own] = the user's key(s); [bundled] = the built-in key(s) that stand in when [own] is empty. */
    fun statusFor(
        own: List<String?>,
        bundled: List<String?> = emptyList(),
    ): CredentialStatus =
        when {
            own.none { it.isNullOrBlank() } -> CredentialStatus.CONFIGURED
            bundled.isNotEmpty() && bundled.none { it.isNullOrBlank() } -> CredentialStatus.BUILT_IN
            else -> CredentialStatus.NOT_CONFIGURED
        }

    fun effective(
        own: String?,
        bundled: String?,
    ): String = own?.takeIf { it.isNotBlank() } ?: bundled.orEmpty()

    val tmdbBundled = listOf(builtIn.tmdbBearerToken)
    val igdbBundled = listOf(builtIn.igdbClientId, builtIn.igdbClientSecret)
    val steamGridDbBundled = listOf(builtIn.steamGridDbApiKey)
    val itadBundled = listOf(builtIn.itadApiKey)
    val googleBooksBundled = listOf(builtIn.googleBooksApiKey)

    var tmdbStatus by remember { mutableStateOf(statusFor(listOf(overrides.tmdbApiKey), tmdbBundled)) }
    var igdbStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.igdbClientId, overrides.igdbClientSecret), igdbBundled))
    }
    var steamGridDbStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.steamGridDbApiKey), steamGridDbBundled))
    }
    var itadStatus by remember { mutableStateOf(statusFor(listOf(overrides.itadApiKey), itadBundled)) }
    var googleBooksStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.googleBooksApiKey), googleBooksBundled))
    }
    var steamStatus by remember { mutableStateOf(statusFor(listOf(overrides.steamApiKey, overrides.steamId))) }
    var retroAchievementsStatus by remember {
        mutableStateOf(statusFor(listOf(overrides.retroAchievementsUsername, overrides.retroAchievementsApiKey)))
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

    fun isBuiltInInUse(
        own: List<String?>,
        bundled: List<String?>,
    ) = own.any { it.isNullOrBlank() } && bundled.none { it.isNullOrBlank() }

    fun canResetToBuiltIn(
        own: List<String?>,
        bundled: List<String?>,
    ) = own.any { !it.isNullOrBlank() } && bundled.none { it.isNullOrBlank() }

    Column(modifier) {
        SectionHeader(stringResource(R.string.service_section_metadata))

        val tmdbOwn = listOf(overrides.tmdbApiKey)
        ServiceCredentialCard(
            name = "TMDB",
            description = stringResource(R.string.service_tmdb_desc),
            status = tmdbStatus,
            usingBuiltIn = isBuiltInInUse(tmdbOwn, tmdbBundled),
            onTest = {
                test({ tmdbStatus = it }) {
                    TmdbService(effective(overrides.tmdbApiKey, builtIn.tmdbBearerToken)).testConnection()
                }
            },
            onReset =
                if (canResetToBuiltIn(tmdbOwn, tmdbBundled)) {
                    {
                        ApiKeyPreferences.setTmdbApiKey("")
                        tmdbStatus = CredentialStatus.BUILT_IN
                        reload()
                    }
                } else {
                    null
                },
        ) { usingBuiltIn ->
            InlineKeyField(
                label = stringResource(R.string.credential_field_bearer_token),
                value = overrides.tmdbApiKey.orEmpty(),
                placeholder = builtInPlaceholder(usingBuiltIn),
                onSave = { value ->
                    ApiKeyPreferences.setTmdbApiKey(value)
                    tmdbStatus = statusFor(listOf(value), tmdbBundled)
                    reload()
                },
            )
        }

        val igdbOwn = listOf(overrides.igdbClientId, overrides.igdbClientSecret)
        ServiceCredentialCard(
            name = "IGDB",
            description = stringResource(R.string.service_igdb_desc),
            status = igdbStatus,
            usingBuiltIn = isBuiltInInUse(igdbOwn, igdbBundled),
            onTest = {
                test({ igdbStatus = it }) {
                    val ownComplete = igdbOwn.none { it.isNullOrBlank() }
                    IgdbAuthService.getAccessToken(
                        context,
                        if (ownComplete) overrides.igdbClientId.orEmpty() else builtIn.igdbClientId.orEmpty(),
                        if (ownComplete) overrides.igdbClientSecret.orEmpty() else builtIn.igdbClientSecret.orEmpty(),
                    )
                }
            },
            onReset =
                if (canResetToBuiltIn(igdbOwn, igdbBundled)) {
                    {
                        ApiKeyPreferences.setIgdbClientId("")
                        ApiKeyPreferences.setIgdbClientSecret("")
                        igdbStatus = CredentialStatus.BUILT_IN
                        reload()
                    }
                } else {
                    null
                },
        ) { usingBuiltIn ->
            InlineKeyField(
                label = stringResource(R.string.credential_field_client_id),
                value = overrides.igdbClientId.orEmpty(),
                placeholder = builtInPlaceholder(usingBuiltIn),
                onSave = { value ->
                    ApiKeyPreferences.setIgdbClientId(value)
                    igdbStatus = statusFor(listOf(value, overrides.igdbClientSecret), igdbBundled)
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = stringResource(R.string.credential_field_client_secret),
                value = overrides.igdbClientSecret.orEmpty(),
                placeholder = builtInPlaceholder(usingBuiltIn),
                onSave = { value ->
                    ApiKeyPreferences.setIgdbClientSecret(value)
                    igdbStatus = statusFor(listOf(overrides.igdbClientId, value), igdbBundled)
                    reload()
                },
            )
        }

        val steamGridDbOwn = listOf(overrides.steamGridDbApiKey)
        ServiceCredentialCard(
            name = "SteamGridDB",
            description = stringResource(R.string.service_steamgriddb_desc),
            status = steamGridDbStatus,
            usingBuiltIn = isBuiltInInUse(steamGridDbOwn, steamGridDbBundled),
            onTest = {
                test({ steamGridDbStatus = it }) {
                    SteamGridDbService(effective(overrides.steamGridDbApiKey, builtIn.steamGridDbApiKey)).testConnection()
                }
            },
            onReset =
                if (canResetToBuiltIn(steamGridDbOwn, steamGridDbBundled)) {
                    {
                        ApiKeyPreferences.setSteamGridDbApiKey("")
                        steamGridDbStatus = CredentialStatus.BUILT_IN
                        reload()
                    }
                } else {
                    null
                },
        ) { usingBuiltIn ->
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.steamGridDbApiKey.orEmpty(),
                placeholder = builtInPlaceholder(usingBuiltIn),
                onSave = { value ->
                    ApiKeyPreferences.setSteamGridDbApiKey(value)
                    steamGridDbStatus = statusFor(listOf(value), steamGridDbBundled)
                    reload()
                },
            )
        }

        val itadOwn = listOf(overrides.itadApiKey)
        ServiceCredentialCard(
            name = "ITAD",
            description = stringResource(R.string.service_itad_desc),
            status = itadStatus,
            usingBuiltIn = isBuiltInInUse(itadOwn, itadBundled),
            onTest = {
                test({ itadStatus = it }) { ItadService(effective(overrides.itadApiKey, builtIn.itadApiKey)).testConnection() }
            },
            onReset =
                if (canResetToBuiltIn(itadOwn, itadBundled)) {
                    {
                        ApiKeyPreferences.setItadApiKey("")
                        itadStatus = CredentialStatus.BUILT_IN
                        reload()
                    }
                } else {
                    null
                },
        ) { usingBuiltIn ->
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.itadApiKey.orEmpty(),
                placeholder = builtInPlaceholder(usingBuiltIn),
                onSave = { value ->
                    ApiKeyPreferences.setItadApiKey(value)
                    itadStatus = statusFor(listOf(value), itadBundled)
                    reload()
                },
            )
        }

        val googleBooksOwn = listOf(overrides.googleBooksApiKey)
        ServiceCredentialCard(
            name = "Google Books",
            description = stringResource(R.string.service_google_books_desc),
            status = googleBooksStatus,
            usingBuiltIn = isBuiltInInUse(googleBooksOwn, googleBooksBundled),
            // Google Books also answers without any key, so the test is always available.
            testAlwaysAvailable = true,
            onTest = {
                test({ googleBooksStatus = it }) {
                    GoogleBooksService(effective(overrides.googleBooksApiKey, builtIn.googleBooksApiKey).ifBlank { null })
                        .testConnection()
                }
            },
            onReset =
                if (canResetToBuiltIn(googleBooksOwn, googleBooksBundled)) {
                    {
                        ApiKeyPreferences.setGoogleBooksApiKey("")
                        googleBooksStatus = CredentialStatus.BUILT_IN
                        reload()
                    }
                } else {
                    null
                },
        ) { usingBuiltIn ->
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.googleBooksApiKey.orEmpty(),
                placeholder = builtInPlaceholder(usingBuiltIn),
                onSave = { value ->
                    ApiKeyPreferences.setGoogleBooksApiKey(value)
                    googleBooksStatus = statusFor(listOf(value), googleBooksBundled)
                    reload()
                },
            )
        }

        SectionHeader(stringResource(R.string.service_section_tracking))

        ServiceCredentialCard(
            name = "Steam",
            description = stringResource(R.string.service_steam_desc),
            status = steamStatus,
            usingBuiltIn = false,
            onTest = {
                test({ steamStatus = it }) {
                    SteamService(overrides.steamApiKey.orEmpty(), overrides.steamId.orEmpty()).testConnection()
                }
            },
            onReset = null,
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_steam_id_64),
                value = overrides.steamId.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamId(value)
                    steamStatus = statusFor(listOf(overrides.steamApiKey, value))
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.steamApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamApiKey(value)
                    steamStatus = statusFor(listOf(value, overrides.steamId))
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "RetroAchievements",
            description = stringResource(R.string.service_retroachievements_desc),
            status = retroAchievementsStatus,
            usingBuiltIn = false,
            onTest = {
                test({ retroAchievementsStatus = it }) {
                    RetroAchievementsService(
                        overrides.retroAchievementsUsername.orEmpty(),
                        overrides.retroAchievementsApiKey.orEmpty(),
                    ).testConnection()
                }
            },
            onReset = null,
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_username),
                value = overrides.retroAchievementsUsername.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setRetroAchievementsUsername(value)
                    retroAchievementsStatus = statusFor(listOf(value, overrides.retroAchievementsApiKey))
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.retroAchievementsApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setRetroAchievementsApiKey(value)
                    retroAchievementsStatus = statusFor(listOf(overrides.retroAchievementsUsername, value))
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
            onReset = null,
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
private fun builtInPlaceholder(usingBuiltIn: Boolean): String? =
    if (usingBuiltIn) stringResource(R.string.credential_built_in_placeholder) else null

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
    onReset: (() -> Unit)?,
    testAlwaysAvailable: Boolean = false,
    fields: @Composable ColumnScope.(usingBuiltIn: Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))
            fields(usingBuiltIn)
            if (usingBuiltIn) OwnKeyHint()
            Spacer(Modifier.height(8.dp))
            CredentialStatusRow(
                status = status,
                testEnabled = testAlwaysAvailable || status != CredentialStatus.NOT_CONFIGURED,
                onTest = onTest,
                onReset = onReset,
            )
        }
    }
}

/** "For better rate limits we recommend using your own API key" — same hint as Tonkatsu Box. */
@Composable
private fun OwnKeyHint() {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.credential_own_key_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun CredentialStatusRow(
    status: CredentialStatus,
    testEnabled: Boolean,
    onTest: () -> Unit,
    onReset: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(status)
        Text(
            stringResource(status.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        if (onReset != null) {
            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.credential_reset_to_built_in),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(
            onClick = onTest,
            enabled = testEnabled && status != CredentialStatus.TESTING,
            modifier = Modifier.size(32.dp),
        ) {
            if (status == CredentialStatus.TESTING) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = stringResource(R.string.service_test_connection),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
