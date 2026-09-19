package app.oribu.ui.screens.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.data.db.DB
import app.oribu.model.ApiSearchResult
import app.oribu.model.GameConsole
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.service.ApiServices
import app.oribu.service.MediaCacheService
import app.oribu.ui.components.MediaGridCard
import app.oribu.ui.components.StatusOptionTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AddGameViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<ApiSearchResult>>(emptyList())
    val results = _results.asStateFlow()
    var loading by mutableStateOf(false)

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            loading = true
            _results.value =
                runCatching {
                    withContext(Dispatchers.IO) { ApiServices.gameSearch.search(query) }
                }.getOrElse { emptyList() }
            loading = false
        }
    }

    fun clear() {
        _results.value = emptyList()
    }

    fun add(
        result: ApiSearchResult,
        console: GameConsole,
        status: MediaStatus,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            // "Waiting Release" isn't a selectable option — the game falls into it
            // automatically when Backlog is picked but it hasn't come out yet.
            val finalStatus =
                if (status == MediaStatus.QUEUED && result.releaseDate?.after(Date()) == true) {
                    MediaStatus.WAITING_RELEASE
                } else {
                    status
                }
            val item =
                MediaItem(
                    type = MediaType.GAME,
                    title = result.title,
                    status = finalStatus,
                    coverUrl = result.coverUrl,
                    addedDate = Date(),
                    console = console,
                    externalId = result.externalId,
                    apiSource = result.apiSource,
                    genre = result.genre,
                    developer = result.developer,
                    releaseDate = result.releaseDate,
                )
            val newId = DB.repo.save(item)
            onDone()
            MediaCacheService.fetchAndPersist(item.copy(id = newId))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    navController: NavController,
    vm: AddGameViewModel = viewModel(),
) {
    val results by vm.results.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showConsoleSheet by remember { mutableStateOf<ApiSearchResult?>(null) }
    var showStatusSheet by remember { mutableStateOf<Pair<ApiSearchResult, GameConsole>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_game_title)) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.add_game_search_placeholder)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            vm.clear()
                        }) { Icon(Icons.Default.Clear, null) }
                    } else {
                        IconButton(onClick = { vm.search(query) }) { Icon(Icons.Default.Search, null) }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            if (vm.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results) { result ->
                    MediaGridCard(
                        title = result.title,
                        coverUrl = result.coverUrl,
                        onAddClick = {
                            val consoles = consolesForPlatforms(result.platforms)
                            if (consoles.size == 1) {
                                val console = consoles.first()
                                if (isUnreleased(result)) {
                                    vm.add(result, console, MediaStatus.WAITING_RELEASE) { navController.popBackStack() }
                                } else {
                                    showStatusSheet = Pair(result, console)
                                }
                            } else {
                                showConsoleSheet = result
                            }
                        },
                    )
                }
            }
        }
    }

    showConsoleSheet?.let { result ->
        val availableConsoles = consolesForPlatforms(result.platforms)
        ModalBottomSheet(onDismissRequest = { showConsoleSheet = null }) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.label_platform), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                availableConsoles.forEach { console ->
                    ListItem(
                        headlineContent = { Text(console.label) },
                        modifier =
                            Modifier.clickable {
                                showConsoleSheet = null
                                if (isUnreleased(result)) {
                                    vm.add(result, console, MediaStatus.WAITING_RELEASE) { navController.popBackStack() }
                                } else {
                                    showStatusSheet = Pair(result, console)
                                }
                            },
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    showStatusSheet?.let { (result, console) ->
        val statuses =
            when {
                console.isSteam || console == GameConsole.PC -> MediaStatus.forSteam()
                console.isPlayStation -> MediaStatus.forPlayStation()
                console.isNintendo -> MediaStatus.forNintendo()
                else -> MediaStatus.forOtherGames()
            }
        val accentColor = console.color
        ModalBottomSheet(onDismissRequest = { showStatusSheet = null }) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp)) {
                Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                statuses.forEachIndexed { index, status ->
                    val (icon, title, subtitle) = gameStatusInfo(status)
                    StatusOptionTile(
                        icon = icon,
                        title = title,
                        subtitle = subtitle,
                        selected = false,
                        color = accentColor,
                        onClick = {
                            vm.add(result, console, status) { navController.popBackStack() }
                            showStatusSheet = null
                        },
                    )
                    if (index != statuses.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun isUnreleased(result: ApiSearchResult): Boolean = result.releaseDate?.after(Date()) == true

@Composable
private fun gameStatusInfo(status: MediaStatus): Triple<ImageVector, String, String> =
    when (status) {
        MediaStatus.PLAYING -> {
            Triple(Icons.Default.PlayArrow, status.label, stringResource(R.string.add_game_status_playing_subtitle))
        }

        MediaStatus.REPLAYING -> {
            Triple(Icons.Default.Replay, status.label, stringResource(R.string.add_game_status_replaying_subtitle))
        }

        MediaStatus.QUEUED -> {
            Triple(
                Icons.Default.Queue,
                stringResource(R.string.games_tab_backlog),
                stringResource(R.string.add_game_status_queued_subtitle),
            )
        }

        MediaStatus.FINISHED -> {
            Triple(Icons.Default.MenuBook, status.label, stringResource(R.string.add_game_status_finished_subtitle))
        }

        MediaStatus.COMPLETED -> {
            Triple(Icons.Default.AutoAwesome, status.label, stringResource(R.string.add_game_status_completed_subtitle))
        }

        MediaStatus.PLATINUM -> {
            Triple(Icons.Default.EmojiEvents, status.label, stringResource(R.string.add_game_status_platinum_subtitle))
        }

        else -> {
            Triple(Icons.Default.Queue, status.label, "")
        }
    }

private fun consolesForPlatforms(platforms: List<String>?): List<GameConsole> {
    if (platforms.isNullOrEmpty()) return GameConsole.entries.toList()
    val mapped =
        platforms
            .mapNotNull { name ->
                when (name) {
                    // PC and Steam count as the same platform — we use STEAM to already enable
                    // achievement tracking (no support for separate stores like Epic Games).
                    "PC" -> GameConsole.STEAM

                    "Steam" -> GameConsole.STEAM

                    "Switch" -> GameConsole.NS

                    "Switch 2" -> GameConsole.NS2

                    "PS5" -> GameConsole.PS5

                    "PS4" -> GameConsole.PS4

                    "PS3" -> GameConsole.PS3

                    "PS2" -> GameConsole.PS2

                    "PS1" -> GameConsole.PS1

                    "PS Vita" -> GameConsole.PS_VITA

                    "PSP" -> GameConsole.PSP

                    "Series X/S" -> GameConsole.XSX

                    "Xbox One" -> GameConsole.X_ONE

                    "Xbox 360" -> GameConsole.X360

                    "Xbox" -> GameConsole.XBOX

                    "3DS" -> GameConsole.N3DS

                    "Wii U" -> GameConsole.WII_U

                    "Wii" -> GameConsole.WII

                    "GCN" -> GameConsole.GCN

                    "DS" -> GameConsole.DS

                    "GBA" -> GameConsole.GBA

                    "N64" -> GameConsole.N64

                    "SNES" -> GameConsole.SNES

                    "NES" -> GameConsole.NES

                    "iOS", "Android", "Mobile" -> GameConsole.MOBILE

                    else -> null
                }
            }.distinct()
    return mapped.ifEmpty { GameConsole.entries.toList() }
}
