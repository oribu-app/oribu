package app.oribu.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.model.MediaItem
import app.oribu.service.ApiServices
import app.oribu.service.SteamGridDbCover
import app.oribu.service.SteamGridDbGame
import app.oribu.service.SteamGridDbService
import app.oribu.ui.components.EmptyState
import app.oribu.ui.components.localizedApiErrorMessage
import app.oribu.ui.navigation.COVER_PICKER_RESTORE
import app.oribu.ui.navigation.Routes
import app.oribu.ui.navigation.consumeCoverPickerItem
import app.oribu.ui.navigation.returnCoverPickerResult
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoverPickerViewModel : ViewModel() {
    var games by mutableStateOf<List<SteamGridDbGame>>(emptyList())
    var selectedGameId by mutableStateOf<Int?>(null)
    var covers by mutableStateOf<List<SteamGridDbCover>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<Throwable?>(null)
    private var initialized = false

    fun init(item: MediaItem) {
        if (initialized || !ApiServices.steamGridDbAvailable) return
        initialized = true
        load {
            // Steam games can skip title matching entirely — look the grids up by app id first.
            val steamAppId = item.externalId?.toIntOrNull()?.takeIf { item.console?.isSteam == true }
            val bySteamId = steamAppId?.let { runCatching { ApiServices.steamGridDb.getCoversBySteamAppId(it) }.getOrNull() }
            games = ApiServices.steamGridDb.searchGames(item.title)
            if (!bySteamId.isNullOrEmpty()) {
                covers = bySteamId
            } else {
                selectedGameId = games.firstOrNull()?.id
                covers = selectedGameId?.let { ApiServices.steamGridDb.getCovers(it) } ?: emptyList()
            }
        }
    }

    fun selectGame(gameId: Int) {
        if (gameId == selectedGameId) return
        selectedGameId = gameId
        load { covers = ApiServices.steamGridDb.getCovers(gameId) }
    }

    private fun load(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            runCatching { withContext(Dispatchers.IO) { block() } }.onFailure { error = it }
            loading = false
        }
    }
}

/**
 * Lets the user replace a game's cover with a community cover from SteamGridDB. Returns the
 * picked URL (or [COVER_PICKER_RESTORE]) to [GameDetailScreen], which persists it.
 */
@Composable
fun CoverPickerScreen(
    navController: NavController,
    vm: CoverPickerViewModel = viewModel(),
) {
    val item = remember { navController.consumeCoverPickerItem() } ?: return
    LaunchedEffect(Unit) { vm.init(item) }

    fun pick(url: String) {
        navController.returnCoverPickerResult(url)
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.cover_picker_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (SteamGridDbService.isSteamGridDbUrl(item.coverUrl)) {
                    TextButton(onClick = { pick(COVER_PICKER_RESTORE) }) {
                        Text(stringResource(R.string.cover_picker_restore))
                    }
                }
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                !ApiServices.steamGridDbAvailable -> {
                    EmptyState(
                        title = stringResource(R.string.cover_picker_not_configured_title),
                        subtitle = stringResource(R.string.cover_picker_not_configured_subtitle),
                        buttonLabel = stringResource(R.string.settings_integrations_title),
                        onButton = { navController.navigate(Routes.SETTINGS_INTEGRATIONS) },
                    )
                }

                else -> {
                    if (vm.games.size > 1) {
                        GameMatchChips(vm.games, vm.selectedGameId, onSelect = vm::selectGame)
                    }
                    CoverPickerContent(vm, currentCoverUrl = item.coverUrl, onPick = ::pick)
                }
            }
        }
    }
}

/** When the title matches several SteamGridDB entries, lets the user pick the right one. */
@Composable
private fun GameMatchChips(
    games: List<SteamGridDbGame>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(games, key = { it.id }) { game ->
            FilterChip(
                selected = game.id == selectedId,
                onClick = { onSelect(game.id) },
                label = { Text(game.releaseYear?.let { "${game.name} ($it)" } ?: game.name, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun CoverPickerContent(
    vm: CoverPickerViewModel,
    currentCoverUrl: String?,
    onPick: (String) -> Unit,
) {
    val error = vm.error
    when {
        vm.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        error != null -> {
            EmptyState(title = localizedApiErrorMessage(error))
        }

        vm.covers.isEmpty() -> {
            EmptyState(title = stringResource(R.string.cover_picker_empty))
        }

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vm.covers, key = { it.id }) { cover ->
                    CoverTile(cover, selected = cover.url == currentCoverUrl, onClick = { onPick(cover.url) })
                }
            }
        }
    }
}

@Composable
private fun CoverTile(
    cover: SteamGridDbCover,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .then(if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape) else Modifier)
                .clickable(onClick = onClick),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = cover.thumbUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp),
                )
            }
        }
        cover.author?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
