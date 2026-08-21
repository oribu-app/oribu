package app.oribu.ui.screens.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.data.db.DB
import app.oribu.data.db.entity.SeriesEpisodeEntity
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.ui.components.AppOverflowMenu
import app.oribu.ui.components.EmptyState
import app.oribu.ui.components.GenreFilterRow
import app.oribu.ui.components.ProportionalTabRow
import app.oribu.ui.navigation.Routes
import app.oribu.ui.theme.ColorSerie
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.items as lazyItems

class SeriesViewModel : ViewModel() {
    val allItems =
        DB.repo
            .watchByType(MediaType.SERIES)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEpisodes =
        DB.repo
            .watchAllEpisodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    navController: NavController,
    vm: SeriesViewModel = viewModel(),
) {
    val allItems by vm.allItems.collectAsStateWithLifecycle()
    val allEpisodes by vm.allEpisodes.collectAsStateWithLifecycle()

    val hoje = remember { Date() }
    val tabs = listOf("Todos", "Assistindo", "Quero Assistir", "Histórico", "Em Breve")
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    // Em Breve = WAITING_RELEASE ou WAITING_EPISODES (renovada, nova temporada a caminho)
    val upcoming =
        remember(allItems, hoje) {
            allItems
                .filter {
                    it.status == MediaStatus.WAITING_RELEASE ||
                        it.status == MediaStatus.WAITING_EPISODES
                }.sortedBy { it.releaseDate }
        }

    // Histórico = HISTORY ou CONCLUDED (cancelada/terminada)
    val history =
        remember(allItems) {
            allItems
                .filter {
                    it.status == MediaStatus.HISTORY ||
                        it.status == MediaStatus.CONCLUDED
                }.sortedByDescending { it.completionDate ?: it.addedDate }
        }

    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedPlatform by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val availableGenres = remember(allItems) { allItems.mapNotNull { it.genre }.distinct().sorted() }
    val availablePlatforms = remember(allItems) { allItems.mapNotNull { it.streamingPlatform }.distinct().sorted() }

    val filtered =
        remember(allItems, selectedTab) {
            when (selectedTab) {
                0 -> allItems
                1 -> allItems.filter { it.status == MediaStatus.WATCHING || it.status == MediaStatus.REWATCHING }
                2 -> allItems.filter { it.status == MediaStatus.QUEUED }.sortedBy { it.title }
                3 -> history
                4 -> upcoming
                else -> allItems
            }
        }

    // Histórico = lista plana de episódios assistidos (estilo SeriesGuide), mais
    // recentes primeiro, independente do status atual da série.
    val watchedEpisodesFlat =
        remember(allEpisodes, allItems) {
            val itemsById = allItems.associateBy { it.id }
            allEpisodes
                .mapNotNull { ep -> itemsById[ep.mediaItemId]?.let { series -> ep to series } }
                .sortedByDescending { it.first.watchedAtMs }
        }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Séries") },
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
                    selectedColor = ColorSerie,
                    onTabSelected = { selectedTab = it },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.SERIES_ADD) },
                containerColor = ColorSerie,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Adicionar série") },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // ── Histórico — lista plana de episódios assistidos (estilo SeriesGuide) ──
            if (selectedTab == 3) {
                if (watchedEpisodesFlat.isEmpty()) {
                    EmptyState(
                        "Histórico vazio",
                        "Episódios que você marcar como assistidos aparecerão aqui",
                        "Adicionar série",
                        onButton = { navController.navigate(Routes.SERIES_ADD) },
                    )
                } else {
                    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        lazyItems(
                            items = watchedEpisodesFlat,
                            key = { (ep, series) -> "ep_${series.id}_${ep.season}_${ep.episode}" },
                        ) { (ep, series) ->
                            EpisodeHistoryFlatRow(
                                ep = ep,
                                series = series,
                                dateFmt = dateFmt,
                                onClick = { navigateToDetail(navController, series) },
                            )
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                val (title, subtitle) =
                    when (selectedTab) {
                        0 -> "Nenhuma série na biblioteca" to "Adicione uma série para começar"
                        1 -> "Nenhuma série em andamento" to "Séries que você está assistindo aparecerão aqui"
                        2 -> "Nenhuma série na fila" to "Séries que você quer assistir aparecerão aqui"
                        else -> "Nenhum lançamento pendente" to "Séries aguardando estreia ou nova temporada aparecerão aqui"
                    }
                EmptyState(title, subtitle, "Adicionar série", onButton = { navController.navigate(Routes.SERIES_ADD) })
            } else {
                val genreFiltered =
                    if (selectedTab == 0) {
                        filtered
                            .filter { selectedGenre == null || it.genre == selectedGenre }
                            .filter { selectedPlatform == null || it.streamingPlatform == selectedPlatform }
                            .filter { !favoritesOnly || it.favorite }
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
                                label = { Text("Favoritos") },
                                leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ColorSerie.copy(alpha = 0.18f),
                                        selectedLabelColor = ColorSerie,
                                    ),
                            )
                        }
                        if (availableGenres.isNotEmpty()) {
                            GenreFilterRow(availableGenres, selectedGenre, ColorSerie) { selectedGenre = it }
                        }
                        if (availablePlatforms.isNotEmpty()) {
                            GenreFilterRow(availablePlatforms, selectedPlatform, ColorSerie) { selectedPlatform = it }
                        }
                    }
                    if (genreFiltered.isEmpty()) {
                        EmptyState("Nenhuma série com esse filtro", "Tente outro filtro")
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
}

@Composable
private fun EpisodeHistoryFlatRow(
    ep: SeriesEpisodeEntity,
    series: MediaItem,
    dateFmt: SimpleDateFormat,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (series.coverUrl != null) {
            AsyncImage(
                model = series.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(48.dp).height(68.dp).clip(RoundedCornerShape(4.dp)),
            )
        } else {
            Box(
                Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .background(ColorSerie.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Tv, null, tint = ColorSerie.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                dateFmt.format(Date(ep.watchedAtMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                series.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${ep.season}x${ep.episode}${if (!ep.episodeName.isNullOrBlank()) " ${ep.episodeName}" else ""}",
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
                    Modifier.fillMaxSize().background(ColorSerie.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Tv, null, tint = ColorSerie.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
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
    navController.navigate(Routes.SERIES_DETAIL)
}
