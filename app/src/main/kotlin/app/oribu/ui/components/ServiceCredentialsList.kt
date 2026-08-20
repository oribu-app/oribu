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
import androidx.compose.ui.unit.dp
import app.oribu.data.ApiKeyPreferences
import app.oribu.service.ApiServices
import app.oribu.service.GoogleBooksService
import app.oribu.service.IgdbAuthService
import app.oribu.service.ItadService
import app.oribu.service.SteamService
import app.oribu.service.TmdbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lista de serviços externos (TMDB, IGDB, Google Books, Steam, ITAD) com campo de credencial +
 * "Testar conexão" + indicador de status, no molde do tonkatsu_box. Componente puro (sem
 * Scaffold própria) para poder ser embutido tanto na tela de Configurações → Integrações quanto
 * no passo de chaves de API do onboarding.
 */
@Composable
fun ServiceCredentialsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overrides by ApiKeyPreferences.overrides.collectAsState()
    val scope = rememberCoroutineScope()

    fun statusFor(vararg values: String?): CredentialStatus =
        if (values.any { it.isNullOrBlank() }) {
            CredentialStatus.NOT_CONFIGURED
        } else {
            CredentialStatus.CONFIGURED
        }

    var tmdbStatus by remember { mutableStateOf(statusFor(overrides.tmdbApiKey)) }
    var igdbStatus by remember { mutableStateOf(statusFor(overrides.igdbClientId, overrides.igdbClientSecret)) }
    var googleBooksStatus by remember { mutableStateOf(statusFor(overrides.googleBooksApiKey)) }
    var steamStatus by remember { mutableStateOf(statusFor(overrides.steamApiKey, overrides.steamId)) }
    var itadStatus by remember { mutableStateOf(statusFor(overrides.itadApiKey)) }

    fun reload() = scope.launch { ApiServices.reload(context) }

    Column(modifier) {
        ServiceCredentialCard(
            name = "TMDB",
            description = "Filmes e séries",
            status = tmdbStatus,
            onTest = {
                tmdbStatus = CredentialStatus.TESTING
                scope.launch {
                    val ok =
                        withContext(Dispatchers.IO) {
                            runCatching { TmdbService(overrides.tmdbApiKey.orEmpty()).testConnection() }
                        }.isSuccess
                    tmdbStatus = if (ok) CredentialStatus.VALID else CredentialStatus.INVALID
                }
            },
        ) {
            InlineKeyField(
                label = "Bearer Token",
                value = overrides.tmdbApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setTmdbApiKey(value)
                    tmdbStatus = statusFor(value)
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "IGDB",
            description = "Jogos (requer app no Twitch Developer Console)",
            status = igdbStatus,
            onTest = {
                igdbStatus = CredentialStatus.TESTING
                scope.launch {
                    val ok =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                IgdbAuthService.getAccessToken(
                                    context,
                                    overrides.igdbClientId.orEmpty(),
                                    overrides.igdbClientSecret.orEmpty(),
                                )
                            }
                        }.isSuccess
                    igdbStatus = if (ok) CredentialStatus.VALID else CredentialStatus.INVALID
                }
            },
        ) {
            InlineKeyField(
                label = "Client ID",
                value = overrides.igdbClientId.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setIgdbClientId(value)
                    igdbStatus = statusFor(value, overrides.igdbClientSecret)
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = "Client Secret",
                value = overrides.igdbClientSecret.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setIgdbClientSecret(value)
                    igdbStatus = statusFor(overrides.igdbClientId, value)
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "Google Books",
            description = "Livros (opcional — a busca já funciona sem chave)",
            status = googleBooksStatus,
            onTest = {
                googleBooksStatus = CredentialStatus.TESTING
                scope.launch {
                    val ok =
                        withContext(Dispatchers.IO) {
                            runCatching { GoogleBooksService(overrides.googleBooksApiKey).testConnection() }
                        }.isSuccess
                    googleBooksStatus = if (ok) CredentialStatus.VALID else CredentialStatus.INVALID
                }
            },
        ) {
            InlineKeyField(
                label = "API Key",
                value = overrides.googleBooksApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setGoogleBooksApiKey(value)
                    googleBooksStatus = statusFor(value)
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "Steam",
            description = "Biblioteca e conquistas",
            status = steamStatus,
            onTest = {
                steamStatus = CredentialStatus.TESTING
                scope.launch {
                    val ok =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                SteamService(overrides.steamApiKey.orEmpty(), overrides.steamId.orEmpty()).testConnection()
                            }
                        }.isSuccess
                    steamStatus = if (ok) CredentialStatus.VALID else CredentialStatus.INVALID
                }
            },
        ) {
            InlineKeyField(
                label = "API Key",
                value = overrides.steamApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamApiKey(value)
                    steamStatus = statusFor(value, overrides.steamId)
                    reload()
                },
            )
            Spacer(Modifier.height(8.dp))
            InlineKeyField(
                label = "SteamID64",
                value = overrides.steamId.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setSteamId(value)
                    steamStatus = statusFor(overrides.steamApiKey, value)
                    reload()
                },
            )
        }

        ServiceCredentialCard(
            name = "ITAD",
            description = "Preços e histórico de descontos (IsThereAnyDeal)",
            status = itadStatus,
            onTest = {
                itadStatus = CredentialStatus.TESTING
                scope.launch {
                    val ok =
                        withContext(Dispatchers.IO) {
                            runCatching { ItadService(overrides.itadApiKey.orEmpty()).testConnection() }
                        }.isSuccess
                    itadStatus = if (ok) CredentialStatus.VALID else CredentialStatus.INVALID
                }
            },
        ) {
            InlineKeyField(
                label = "API Key",
                value = overrides.itadApiKey.orEmpty(),
                onSave = { value ->
                    ApiKeyPreferences.setItadApiKey(value)
                    itadStatus = statusFor(value)
                    reload()
                },
            )
        }

        Text(
            "AniList não exige chave própria — a busca de mangás e webtoons já funciona sem configuração.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ServiceCredentialCard(
    name: String,
    description: String,
    status: CredentialStatus,
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
            Spacer(Modifier.height(12.dp))
            fields()
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onTest, enabled = status != CredentialStatus.TESTING) {
                Text("Testar conexão")
            }
        }
    }
}
