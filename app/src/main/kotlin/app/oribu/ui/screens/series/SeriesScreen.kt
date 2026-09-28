package app.oribu.ui.screens.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.data.SeriesScope
import app.oribu.data.SeriesScopePreferences
import app.oribu.data.db.DB
import app.oribu.data.db.entity.SeriesEpisodeEntity
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.ui.components.AppOverflowMenu
import app.oribu.ui.components.EmptyState
import app.oribu.ui.components.GenreFilterRow
import app.oribu.ui.components.ProportionalTabRow
import app.oribu.ui.components.swipeNavigation
import app.oribu.ui.locale.formatDate
import app.oribu.ui.navigation.Routes
import app.oribu.ui.navigation.detailRoute
import app.oribu.ui.theme.ColorAnime
import app.oribu.ui.theme.ColorSerie
import coil.compose.AsyncImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.*
import androidx.compose.foundation.lazy.items as lazyItems

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesViewModel : ViewModel() {
    /** Items of every type the chosen scope covers (series only until the user picks one). */
    val allItems =
        SeriesScopePreferences.seriesScope
            .flatMapLatest { scope -> DB.repo.watchByTypes((scope ?: SeriesScope.SERIES).types) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEpisodes =
        DB.repo
            .watchAllEpisodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** One row of the History tab: a watched series episode or a finished anime, ordered by date. */
private sealed interface HistoryEntry {
    val dateMs: Long
    val item: MediaItem

    data class Episode(
        val ep: SeriesEpisodeEntity,
        override val item: MediaItem,
    ) : HistoryEntry {
        override val dateMs get() = ep.watchedAtMs
    }

    data class FinishedAnime(
        override val item: MediaItem,
        override val dateMs: Long,
    ) : HistoryEntry
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    navController: NavController,
    vm: SeriesViewModel = viewModel(),
    onSwipeToNextHobby: () -> Unit = {},
    onSwipeToPrevHobby: () -> Unit = {},
) {
    val allItems by vm.allItems.collectAsStateWithLifecycle()
    val allEpisodes by vm.allEpisodes.collectAsStateWithLifecycle()
    val scopeLoaded by SeriesScopePreferences.loaded.collectAsStateWithLifecycle()
    val chosenScope by SeriesScopePreferences.seriesScope.collectAsStateWithLifecycle()
    val scope = chosenScope ?: SeriesScope.SERIES
    val accent = if (scope == SeriesScope.ANIME) ColorAnime else ColorSerie

    val hoje = remember { Date() }
    val tabs =
        listOf(
            stringResource(R.string.films_tab_all),
            stringResource(R.string.series_tab_watching),
            stringResource(R.string.films_tab_want_to_watch),
            stringResource(R.string.series_tab_history),
            stringResource(R.string.label_coming_soon),
        )
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddChooser by remember { mutableStateOf(false) }

    // With both scopes, the type chips narrow every tab to series or anime.
    var typeFilter by remember { mutableStateOf<MediaType?>(null) }
    LaunchedEffect(scope) { if (scope != SeriesScope.BOTH) typeFilter = null }
    val scopedItems = remember(allItems, typeFilter) { allItems.filter { typeFilter == null || it.type == typeFilter } }

    // Em Breve = WAITING_RELEASE ou WAITING_EPISODES (renovada, nova temporada a caminho)
    val upcoming =
        remember(scopedItems, hoje) {
            scopedItems
                .filter {
                    it.status == MediaStatus.WAITING_RELEASE ||
                        it.status == MediaStatus.WAITING_EPISODES
                }.sortedBy { it.releaseDate }
        }

    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedPlatform by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var animationOnly by remember { mutableStateOf(false) }
    val availableGenres = remember(scopedItems) { scopedItems.mapNotNull { it.genre }.distinct().sorted() }
    val availablePlatforms = remember(scopedItems) { scopedItems.mapNotNull { it.streamingPlatform }.distinct().sorted() }
    val hasAnimation = remember(scopedItems) { scopedItems.any { it.isAnimation } }

    val filtered =
        remember(scopedItems, selectedTab) {
            when (selectedTab) {
                0 -> scopedItems
                1 -> scopedItems.filter { it.status == MediaStatus.WATCHING || it.status == MediaStatus.REWATCHING }
                2 -> scopedItems.filter { it.status == MediaStatus.QUEUED }.sortedBy { it.title }
                4 -> upcoming
                else -> scopedItems
            }
        }

    // Histórico = linha do tempo (estilo SeriesGuide) com os episódios de série assistidos e os
    // animes concluídos, mais recentes primeiro, independente do status atual.
    val historyEntries =
        remember(allEpisodes, scopedItems) {
            val itemsById = scopedItems.associateBy { it.id }
            val episodes =
                allEpisodes.mapNotNull { ep -> itemsById[ep.mediaItemId]?.let { HistoryEntry.Episode(ep, it) } }
            val finishedAnime =
                scopedItems
                    .filter { it.type == MediaType.ANIME && it.status == MediaStatus.WATCHED }
                    .map { HistoryEntry.FinishedAnime(it, (it.completionDate ?: it.addedDate).time) }
            (episodes + finishedAnime).sortedByDescending { it.dateMs }
        }

    fun onAdd() {
        when (scope) {
            SeriesScope.SERIES -> navController.navigate(Routes.SERIES_ADD)
            SeriesScope.ANIME -> navController.navigate(Routes.ANIME_ADD)
            SeriesScope.BOTH -> showAddChooser = true
        }
    }

    val titleRes =
        when (scope) {
            SeriesScope.SERIES -> R.string.series_title
            SeriesScope.ANIME -> R.string.anime_title
            SeriesScope.BOTH -> R.string.series_scope_both
        }
    val addLabel = stringResource(if (scope == SeriesScope.ANIME) R.string.anime_add_button else R.string.series_add_button)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(titleRes)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigate(Routes.HOME) { launchSingleTop = true }
                        }) {
                            Icon(Icons.Outlined.Home, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        AppOverflowMenu(navController = navController, expanded = showMenu, onDismissRequest = { showMenu = false })
                    },
                )
                ProportionalTabRow(
                    selectedTabIndex = selectedTab,
                    tabs = tabs,
                    selectedColor = accent,
                    onTabSelected = { selectedTab = it },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = ::onAdd,
                containerColor = accent,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addLabel) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .swipeNavigation(
                    onSwipeLeft = {
                        if (selectedTab < tabs.lastIndex) selectedTab++ else onSwipeToNextHobby()
                    },
                    onSwipeRight = {
                        if (selectedTab > 0) selectedTab-- else onSwipeToPrevHobby()
                    },
                ),
        ) {
            if (scope == SeriesScope.BOTH) {
                TypeFilterRow(typeFilter, onSelect = { typeFilter = it })
            }
            // ── Histórico — linha do tempo de episódios e animes concluídos ──
            if (selectedTab == 3) {
                if (historyEntries.isEmpty()) {
                    EmptyState(
                        stringResource(R.string.series_empty_history_title),
                        stringResource(R.string.series_empty_history_subtitle),
                        addLabel,
                        onButton = ::onAdd,
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        lazyItems(
                            items = historyEntries,
                            key = { entry ->
                                when (entry) {
                                    is HistoryEntry.Episode -> "ep_${entry.item.id}_${entry.ep.season}_${entry.ep.episode}"
                                    is HistoryEntry.FinishedAnime -> "anime_${entry.item.id}"
                                }
                            },
                        ) { entry ->
                            HistoryRow(entry = entry, onClick = { navigateToDetail(navController, entry.item) })
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                val (title, subtitle) =
                    when (selectedTab) {
                        0 -> {
                            stringResource(R.string.series_empty_all_title) to stringResource(R.string.series_empty_all_subtitle)
                        }

                        1 -> {
                            stringResource(R.string.series_empty_watching_title) to
                                stringResource(R.string.series_empty_watching_subtitle)
                        }

                        2 -> {
                            stringResource(R.string.series_empty_queued_title) to stringResource(R.string.series_empty_queued_subtitle)
                        }

                        else -> {
                            stringResource(R.string.series_empty_upcoming_title) to
                                stringResource(R.string.series_empty_upcoming_subtitle)
                        }
                    }
                EmptyState(title, subtitle, addLabel, onButton = ::onAdd)
            } else {
                val genreFiltered =
                    if (selectedTab == 0) {
                        filtered
                            .filter { selectedGenre == null || it.genre == selectedGenre }
                            .filter { selectedPlatform == null || it.streamingPlatform == selectedPlatform }
                            .filter { !favoritesOnly || it.favorite }
                            .filter { !animationOnly || it.isAnimation }
                    } else {
                        filtered
                    }

                Column(Modifier.fillMaxSize()) {
                    if (selectedTab == 0) {
                        Row(
                            Modifier.padding(start = 12.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = favoritesOnly,
                                onClick = { favoritesOnly = !favoritesOnly },
                                label = { Text(stringResource(R.string.label_favorites)) },
                                leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accent.copy(alpha = 0.18f),
                                        selectedLabelColor = accent,
                                    ),
                            )
                            if (hasAnimation) {
                                FilterChip(
                                    selected = animationOnly,
                                    onClick = { animationOnly = !animationOnly },
                                    label = { Text(stringResource(R.string.label_animation)) },
                                    leadingIcon = { Icon(Icons.Default.Animation, null, modifier = Modifier.size(16.dp)) },
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accent.copy(alpha = 0.18f),
                                            selectedLabelColor = accent,
                                        ),
                                )
                            }
                        }
                        if (availableGenres.isNotEmpty()) {
                            GenreFilterRow(availableGenres, selectedGenre, accent) { selectedGenre = it }
                        }
                        if (availablePlatforms.isNotEmpty()) {
                            GenreFilterRow(availablePlatforms, selectedPlatform, accent) { selectedPlatform = it }
                        }
                    }
                    if (genreFiltered.isEmpty()) {
                        EmptyState(
                            stringResource(R.string.series_empty_filtered_title),
                            stringResource(R.string.films_empty_filtered_subtitle),
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(genreFiltered) { item ->
                                SeriesCard(item = item, onTap = { navigateToDetail(navController, item) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (scopeLoaded && chosenScope == null) {
        SeriesScopeFirstRunDialog(onChoose = { SeriesScopePreferences.set(it) })
    }

    if (showAddChooser) {
        AlertDialog(
            onDismissRequest = { showAddChooser = false },
            title = { Text(stringResource(R.string.series_add_chooser_title)) },
            text = {
                Column {
                    listOf(
                        R.string.series_add_button to Routes.SERIES_ADD,
                        R.string.anime_add_button to Routes.ANIME_ADD,
                    ).forEach { (labelRes, route) ->
                        TextButton(
                            onClick = {
                                showAddChooser = false
                                navController.navigate(route)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(labelRes)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddChooser = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/**
 * Asked on the Series tab's first access — which media the tab covers. Can't be dismissed
 * without a choice (there's no sensible default to fall back to); the footnote points to where it
 * can be changed later.
 */
@Composable
private fun SeriesScopeFirstRunDialog(onChoose: (SeriesScope) -> Unit) {
    var selected by remember { mutableStateOf(SeriesScope.BOTH) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.series_scope_first_run_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SeriesScope.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = { selected = option })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(option.labelRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(option.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.series_scope_first_run_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onChoose(selected) }) { Text(stringResource(R.string.action_confirm)) } },
    )
}

@Composable
private fun TypeFilterRow(
    selected: MediaType?,
    onSelect: (MediaType?) -> Unit,
) {
    val options =
        listOf(
            null to stringResource(R.string.films_tab_all),
            MediaType.SERIES to stringResource(R.string.series_title),
            MediaType.ANIME to stringResource(R.string.anime_title),
        )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        lazyItems(options) { (type, label) ->
            val color = if (type == MediaType.ANIME) ColorAnime else ColorSerie
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.18f),
                        selectedLabelColor = color,
                    ),
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
) {
    val item = entry.item
    val accent = if (item.type == MediaType.ANIME) ColorAnime else ColorSerie
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (item.coverUrl != null) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(48.dp).height(68.dp).clip(RoundedCornerShape(4.dp)),
            )
        } else {
            Box(
                Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Tv, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                formatDate(entry.dateMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail =
                when (entry) {
                    is HistoryEntry.Episode -> {
                        val ep = entry.ep
                        "${ep.season}x${ep.episode}${if (!ep.episodeName.isNullOrBlank()) " ${ep.episodeName}" else ""}"
                    }

                    is HistoryEntry.FinishedAnime -> {
                        stringResource(R.string.series_history_anime_finished)
                    }
                }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeriesCard(
    item: MediaItem,
    onTap: () -> Unit,
) {
    val accent = if (item.type == MediaType.ANIME) ColorAnime else ColorSerie
    Column(
        Modifier.clickable(onClick = onTap),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.56f)) {
            if (item.coverUrl != null) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Tv, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                }
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(34.dp),
        )
    }
}

private fun navigateToDetail(
    navController: NavController,
    item: MediaItem,
) {
    navController.currentBackStackEntry?.savedStateHandle?.set("item", item)
    navController.navigate(item.type.detailRoute)
}
