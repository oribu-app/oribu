package app.oribu.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.model.MediaType
import app.oribu.model.labelPlural
import app.oribu.service.AppUpdateChecker
import app.oribu.ui.components.ServiceLogo
import app.oribu.ui.components.ServiceLogoBadge
import app.oribu.ui.navigation.Routes

/** One data provider credited on the screen. */
private data class DataProvider(
    val name: String,
    val logo: ServiceLogo,
    val covers: List<MediaType>,
    @StringRes val attribution: Int,
    val url: String,
)

/**
 * Every source the app pulls data from, in Tonkatsu Box's credits format. TMDB's line is the exact
 * notice its API terms require (kept in English in every language, as TMDB words it).
 */
private val providers =
    listOf(
        DataProvider(
            "TMDB",
            ServiceLogo.TMDB,
            listOf(MediaType.MOVIE, MediaType.SERIES),
            R.string.credits_tmdb,
            "https://www.themoviedb.org/",
        ),
        DataProvider("IGDB", ServiceLogo.IGDB, listOf(MediaType.GAME), R.string.credits_igdb, "https://www.igdb.com/"),
        DataProvider("HowLongToBeat", ServiceLogo.HLTB, listOf(MediaType.GAME), R.string.credits_hltb, "https://howlongtobeat.com/"),
        DataProvider("IsThereAnyDeal", ServiceLogo.ITAD, listOf(MediaType.GAME), R.string.credits_itad, "https://isthereanydeal.com/"),
        DataProvider(
            "SteamGridDB",
            ServiceLogo.STEAMGRIDDB,
            listOf(MediaType.GAME),
            R.string.credits_steamgriddb,
            "https://www.steamgriddb.com/",
        ),
        DataProvider("Steam", ServiceLogo.STEAM, listOf(MediaType.GAME), R.string.credits_steam, "https://store.steampowered.com/"),
        DataProvider(
            "RetroAchievements",
            ServiceLogo.RETROACHIEVEMENTS,
            listOf(MediaType.GAME),
            R.string.credits_retroachievements,
            "https://retroachievements.org/",
        ),
        DataProvider(
            "AniList",
            ServiceLogo.ANILIST,
            listOf(MediaType.ANIME, MediaType.MANGA),
            R.string.credits_anilist,
            "https://anilist.co/",
        ),
        DataProvider("Kitsu", ServiceLogo.KITSU, listOf(MediaType.ANIME), R.string.credits_kitsu, "https://kitsu.app/"),
        DataProvider("MangaDex", ServiceLogo.MANGADEX, listOf(MediaType.MANGA), R.string.credits_mangadex, "https://mangadex.org/"),
        DataProvider("MangaBaka", ServiceLogo.MANGABAKA, listOf(MediaType.MANGA), R.string.credits_mangabaka, "https://mangabaka.org/"),
        DataProvider(
            "Google Books",
            ServiceLogo.GOOGLE_BOOKS,
            listOf(MediaType.BOOK),
            R.string.credits_google_books,
            "https://books.google.com/",
        ),
        DataProvider(
            "Open Library",
            ServiceLogo.OPEN_LIBRARY,
            listOf(MediaType.BOOK),
            R.string.credits_open_library,
            "https://openlibrary.org/",
        ),
        DataProvider("Hardcover", ServiceLogo.HARDCOVER, listOf(MediaType.BOOK), R.string.credits_hardcover, "https://hardcover.app/"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_credits_licenses)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionTitle(stringResource(R.string.credits_data_providers)) }
            items(providers, key = { it.name }) { provider ->
                ProviderCard(provider, onClick = { uriHandler.openUri(provider.url) })
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.credits_open_source))
                Text(
                    stringResource(R.string.credits_open_source_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(AppUpdateChecker.repoUrl) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "GitHub",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        AppUpdateChecker.repoUrl.removePrefix("https://github.com/"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                OutlinedButton(onClick = { navController.navigate(Routes.ABOUT_LICENSES) }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Outlined.Description, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_open_source_licenses))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

/** Branded attribution card: logo, name, link, the hobbies it covers and the attribution line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderCard(
    provider: DataProvider,
    onClick: () -> Unit,
) {
    val accent = provider.logo.background.takeIf { it != Color.White } ?: MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceLogoBadge(provider.logo, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(provider.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = accent, modifier = Modifier.size(14.dp))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                provider.covers.forEach { type ->
                    Surface(shape = RoundedCornerShape(50), color = type.color.copy(alpha = 0.15f)) {
                        Text(
                            type.labelPlural,
                            style = MaterialTheme.typography.labelSmall,
                            color = type.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                stringResource(provider.attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
