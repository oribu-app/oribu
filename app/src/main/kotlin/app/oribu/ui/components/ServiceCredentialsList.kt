package app.oribu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import app.oribu.service.ApiServices
import app.oribu.service.GoogleBooksService
import app.oribu.service.HardcoverService
import app.oribu.service.IgdbAuthService
import app.oribu.service.ItadService
import app.oribu.service.Secrets
import app.oribu.service.SteamGridDbService
import app.oribu.service.TmdbService
import kotlinx.coroutines.launch

/**
 * Keys used for search and metadata (Settings → Integrations and the onboarding keys step), in
 * Tonkatsu Box's mold: release builds ship app-level keys (written by CI) that are never shown —
 * the field stays empty with a "built-in key" placeholder, a key typed here replaces it and the
 * reset button goes back to it. A credential lives here whenever any search uses it, even if an
 * account feature reuses it later (Hardcover); account-only credentials live in
 * [TrackingAccountsList].
 */
@Composable
fun ServiceCredentialsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overrides by ApiKeyPreferences.overrides.collectAsState()
    val builtIn = remember { Secrets.load(context) }
    val scope = rememberCoroutineScope()

    fun effective(
        own: String?,
        bundled: String?,
    ): String = own?.takeIf { it.isNotBlank() } ?: bundled.orEmpty()

    fun isBuiltInInUse(
        own: List<String?>,
        bundled: List<String?>,
    ) = own.any { it.isNullOrBlank() } && bundled.none { it.isNullOrBlank() }

    fun canResetToBuiltIn(
        own: List<String?>,
        bundled: List<String?>,
    ) = own.any { !it.isNullOrBlank() } && bundled.none { it.isNullOrBlank() }

    fun reload() = scope.launch { ApiServices.reload(context) }

    val tmdbBundled = listOf(builtIn.tmdbBearerToken)
    val igdbBundled = listOf(builtIn.igdbClientId, builtIn.igdbClientSecret)
    val steamGridDbBundled = listOf(builtIn.steamGridDbApiKey)
    val itadBundled = listOf(builtIn.itadApiKey)
    val googleBooksBundled = listOf(builtIn.googleBooksApiKey)

    var tmdbStatus by remember { mutableStateOf(credentialStatusFor(listOf(overrides.tmdbApiKey), tmdbBundled)) }
    var igdbStatus by remember {
        mutableStateOf(credentialStatusFor(listOf(overrides.igdbClientId, overrides.igdbClientSecret), igdbBundled))
    }
    var steamGridDbStatus by remember {
        mutableStateOf(credentialStatusFor(listOf(overrides.steamGridDbApiKey), steamGridDbBundled))
    }
    var itadStatus by remember { mutableStateOf(credentialStatusFor(listOf(overrides.itadApiKey), itadBundled)) }
    var googleBooksStatus by remember {
        mutableStateOf(credentialStatusFor(listOf(overrides.googleBooksApiKey), googleBooksBundled))
    }
    var hardcoverStatus by remember { mutableStateOf(credentialStatusFor(listOf(overrides.hardcoverApiToken))) }

    Column(modifier) {
        val tmdbOwn = listOf(overrides.tmdbApiKey)
        ServiceCredentialCard(
            name = "TMDB",
            description = stringResource(R.string.service_tmdb_desc),
            logo = ServiceLogo.TMDB,
            status = tmdbStatus,
            usingBuiltIn = isBuiltInInUse(tmdbOwn, tmdbBundled),
            onTest = {
                scope.testCredential({ tmdbStatus = it }) {
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
                    tmdbStatus = credentialStatusFor(listOf(value), tmdbBundled)
                    reload()
                },
            )
        }

        val igdbOwn = listOf(overrides.igdbClientId, overrides.igdbClientSecret)
        ServiceCredentialCard(
            name = "IGDB",
            description = stringResource(R.string.service_igdb_desc),
            logo = ServiceLogo.IGDB,
            status = igdbStatus,
            usingBuiltIn = isBuiltInInUse(igdbOwn, igdbBundled),
            onTest = {
                scope.testCredential({ igdbStatus = it }) {
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
                    igdbStatus = credentialStatusFor(listOf(value, overrides.igdbClientSecret), igdbBundled)
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
                    igdbStatus = credentialStatusFor(listOf(overrides.igdbClientId, value), igdbBundled)
                    reload()
                },
            )
        }

        val steamGridDbOwn = listOf(overrides.steamGridDbApiKey)
        ServiceCredentialCard(
            name = "SteamGridDB",
            description = stringResource(R.string.service_steamgriddb_desc),
            logo = ServiceLogo.STEAMGRIDDB,
            status = steamGridDbStatus,
            usingBuiltIn = isBuiltInInUse(steamGridDbOwn, steamGridDbBundled),
            onTest = {
                scope.testCredential({ steamGridDbStatus = it }) {
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
                    steamGridDbStatus = credentialStatusFor(listOf(value), steamGridDbBundled)
                    reload()
                },
            )
        }

        val itadOwn = listOf(overrides.itadApiKey)
        ServiceCredentialCard(
            name = "ITAD",
            description = stringResource(R.string.service_itad_desc),
            logo = ServiceLogo.ITAD,
            status = itadStatus,
            usingBuiltIn = isBuiltInInUse(itadOwn, itadBundled),
            onTest = {
                scope.testCredential({ itadStatus = it }) {
                    ItadService(effective(overrides.itadApiKey, builtIn.itadApiKey)).testConnection()
                }
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
                    itadStatus = credentialStatusFor(listOf(value), itadBundled)
                    reload()
                },
            )
        }

        val googleBooksOwn = listOf(overrides.googleBooksApiKey)
        ServiceCredentialCard(
            name = "Google Books",
            description = stringResource(R.string.service_google_books_desc),
            logo = ServiceLogo.GOOGLE_BOOKS,
            status = googleBooksStatus,
            usingBuiltIn = isBuiltInInUse(googleBooksOwn, googleBooksBundled),
            // Google Books also answers without any key, so the test is always available.
            testAlwaysAvailable = true,
            onTest = {
                scope.testCredential({ googleBooksStatus = it }) {
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
                    googleBooksStatus = credentialStatusFor(listOf(value), googleBooksBundled)
                    reload()
                },
            )
        }

        // Personal token, never bundled (Hardcover asks not to ship tokens in client apps).
        ServiceCredentialCard(
            name = "Hardcover",
            description = stringResource(R.string.service_hardcover_desc),
            logo = ServiceLogo.HARDCOVER,
            status = hardcoverStatus,
            onTest = {
                scope.testCredential({ hardcoverStatus = it }) {
                    HardcoverService(overrides.hardcoverApiToken.orEmpty()).testConnection()
                }
            },
        ) {
            InlineKeyField(
                label = stringResource(R.string.credential_field_api_key),
                value = overrides.hardcoverApiToken.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setHardcoverApiToken(value)
                    hardcoverStatus = credentialStatusFor(listOf(value))
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
