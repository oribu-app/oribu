package app.oribu.ui.screens.manga

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.service.ApiServices
import app.oribu.service.MediaCacheService
import app.oribu.ui.components.MediaGridCard
import app.oribu.ui.components.StatusOptionTile
import app.oribu.ui.components.localizedApiErrorMessage
import app.oribu.ui.theme.ColorManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AddMangaViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<ApiSearchResult>>(emptyList())
    val results = _results.asStateFlow()
    private val _searchError = MutableStateFlow<Throwable?>(null)
    val searchError = _searchError.asStateFlow()
    private val _existingIds = MutableStateFlow<Set<String>>(emptySet())
    val existingIds = _existingIds.asStateFlow()
    var loading by mutableStateOf(false)

    init {
        viewModelScope.launch {
            val items = DB.repo.getByType(MediaType.MANGA)
            _existingIds.value = items.mapNotNull { it.externalId }.toSet()
        }
    }

    fun search(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch {
            loading = true
            _searchError.value = null
            _results.value =
                runCatching {
                    withContext(Dispatchers.IO) { ApiServices.mangaSearch.search(q) }
                }.fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        _searchError.value = e
                        emptyList()
                    },
                )
            loading = false
        }
    }

    fun clear() {
        _results.value = emptyList()
        _searchError.value = null
    }

    fun add(
        result: ApiSearchResult,
        status: MediaStatus,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val item =
                MediaItem(
                    type = MediaType.MANGA,
                    title = result.title,
                    status = status,
                    coverUrl = result.coverUrl,
                    addedDate = Date(),
                    externalId = result.externalId,
                    apiSource = result.apiSource,
                    totalProgress = result.chapters,
                )
            val newId = DB.repo.save(item)
            _existingIds.value = _existingIds.value + setOfNotNull(result.externalId.ifBlank { null })
            onDone()
            MediaCacheService.fetchAndPersist(item.copy(id = newId))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMangaScreen(
    navController: NavController,
    vm: AddMangaViewModel = viewModel(),
) {
    val results by vm.results.collectAsStateWithLifecycle()
    val searchError by vm.searchError.collectAsStateWithLifecycle()
    val existingIds by vm.existingIds.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf<ApiSearchResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manga_add_button)) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.add_manga_search_placeholder)) },
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
            if (vm.loading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            if (searchError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        localizedApiErrorMessage(searchError!!),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results) { r ->
                    val inLibrary = r.externalId in existingIds
                    MediaGridCard(
                        title = r.title,
                        coverUrl = r.coverUrl,
                        inLibrary = inLibrary,
                        onAddClick = if (inLibrary) null else ({ showSheet = r }),
                    )
                }
            }
        }
    }
    showSheet?.let { result ->
        val statuses = MediaStatus.forMangaAdd()
        ModalBottomSheet(onDismissRequest = { showSheet = null }) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp)) {
                Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                statuses.forEachIndexed { index, status ->
                    val (icon, subtitle) = mangaStatusInfo(status)
                    StatusOptionTile(
                        icon = icon,
                        title = mangaStatusLabel(status),
                        subtitle = subtitle,
                        selected = false,
                        color = ColorManga,
                        onClick = {
                            vm.add(result, status) { navController.popBackStack() }
                            showSheet = null
                        },
                    )
                    if (index != statuses.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun mangaStatusInfo(status: MediaStatus): Pair<ImageVector, String> =
    when (status) {
        MediaStatus.READING -> Icons.Default.MenuBook to stringResource(R.string.add_manga_status_reading_subtitle)
        MediaStatus.REREADING -> Icons.Default.Replay to stringResource(R.string.add_manga_status_rereading_subtitle)
        MediaStatus.QUEUED -> Icons.Default.Bookmark to stringResource(R.string.add_manga_status_queued_subtitle)
        else -> Icons.Default.Bookmark to ""
    }
