package app.oribu.ui.screens.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.oribu.data.db.DB
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.ui.components.AppOverflowMenu
import app.oribu.ui.components.EmptyState
import app.oribu.ui.components.ProportionalTabRow
import app.oribu.ui.navigation.Routes
import app.oribu.ui.theme.ColorManga
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class MangaViewModel : ViewModel() {
    val allItems =
        DB.repo
            .watchByTypes(listOf(MediaType.MANGA, MediaType.WEBTOON))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allItems.collect { items ->
                val now = Date()
                val expired =
                    items.filter {
                        it.status == MediaStatus.WAITING_RELEASE &&
                            it.releaseDate != null &&
                            !it.releaseDate.after(now)
                    }
                if (expired.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        expired.forEach { item -> DB.repo.update(item.copy(status = MediaStatus.QUEUED)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaScreen(
    navController: NavController,
    vm: MangaViewModel = viewModel(),
) {
    val allItems by vm.allItems.collectAsStateWithLifecycle()

    val tabs =
        listOf(
            stringResource(R.string.films_tab_all),
            stringResource(R.string.manga_tab_reading),
            stringResource(R.string.manga_tab_read),
            stringResource(R.string.manga_tab_on_hold),
            stringResource(R.string.manga_tab_want_to_read),
        )
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val filtered =
        remember(allItems, selectedTab) {
            when (selectedTab) {
                0 -> {
                    allItems
                }

                1 -> {
                    allItems.filter { it.status == MediaStatus.READING || it.status == MediaStatus.REREADING }
                }

                2 -> {
                    allItems
                        .filter { it.status == MediaStatus.READ }
                        .sortedByDescending { it.completionDate ?: it.addedDate }
                }

                3 -> {
                    allItems.filter { it.status == MediaStatus.ON_HOLD }
                }

                4 -> {
                    allItems
                        .filter { it.status == MediaStatus.QUEUED || it.status == MediaStatus.WAITING_RELEASE }
                        .sortedBy { it.title }
                }

                else -> {
                    allItems
                }
            }
        }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.manga_title)) },
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
                    selectedColor = ColorManga,
                    onTabSelected = { selectedTab = it },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.MANGA_ADD) },
                containerColor = ColorManga,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.manga_add_button)) },
            )
        },
    ) { padding ->
        val addMangaLabel = stringResource(R.string.manga_add_button)
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (filtered.isEmpty()) {
                val (title, subtitle) =
                    when (selectedTab) {
                        0 -> {
                            stringResource(R.string.manga_empty_all_title) to stringResource(R.string.manga_empty_all_subtitle)
                        }

                        1 -> {
                            stringResource(R.string.manga_empty_reading_title) to
                                stringResource(R.string.manga_empty_reading_subtitle)
                        }

                        2 -> {
                            stringResource(R.string.manga_empty_read_title) to stringResource(R.string.manga_empty_read_subtitle)
                        }

                        3 -> {
                            stringResource(R.string.manga_empty_on_hold_title) to stringResource(R.string.manga_empty_on_hold_subtitle)
                        }

                        else -> {
                            stringResource(R.string.manga_empty_want_title) to stringResource(R.string.manga_empty_want_subtitle)
                        }
                    }
                EmptyState(title, subtitle, addMangaLabel, onButton = { navController.navigate(Routes.MANGA_ADD) })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered) { item ->
                        MangaCard(item = item, onTap = { navigateToDetail(navController, item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaCard(
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
                    Modifier.fillMaxSize().background(ColorManga.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MenuBook, null, tint = ColorManga.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
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
    navController.navigate(Routes.MANGA_DETAIL)
}
