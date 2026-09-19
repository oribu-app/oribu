package app.oribu.ui.screens.films

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.data.db.DB
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.service.ApiServices
import app.oribu.service.MediaCacheService
import app.oribu.service.TmdbMovieDetails
import app.oribu.ui.components.StatusOptionTile
import app.oribu.ui.navigation.Routes
import app.oribu.ui.theme.ColorFilme
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MoviePreviewViewModel : ViewModel() {
    var details by mutableStateOf<TmdbMovieDetails?>(null)
    var loading by mutableStateOf(true)
    var existingItem by mutableStateOf<MediaItem?>(null)
    var saving by mutableStateOf(false)

    fun load(tmdbId: Int) {
        viewModelScope.launch {
            loading = true
            existingItem = DB.repo.getByType(MediaType.MOVIE).firstOrNull { it.externalId == tmdbId.toString() }
            details =
                withContext(Dispatchers.IO) {
                    runCatching { ApiServices.tmdb.getMovieDetails(tmdbId) }.getOrNull()
                }
            loading = false
        }
    }

    fun add(
        status: MediaStatus,
        onDone: (MediaItem) -> Unit,
    ) {
        val d = details ?: return
        viewModelScope.launch {
            saving = true
            val item =
                MediaItem(
                    type = MediaType.MOVIE,
                    title = d.title,
                    status = status,
                    coverUrl = d.posterUrl,
                    addedDate = Date(),
                    externalId = d.id.toString(),
                    apiSource = "tmdb",
                    releaseDate = d.releaseDate,
                    genre = d.genres.firstOrNull(),
                )
            val newId = DB.repo.save(item)
            val saved = DB.repo.getById(newId)
            saved?.let { MediaCacheService.fetchAndPersist(it) }
            saving = false
            saved?.let { onDone(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviePreviewScreen(
    navController: NavController,
    tmdbId: Int,
    vm: MoviePreviewViewModel = viewModel(),
) {
    LaunchedEffect(tmdbId) { vm.load(tmdbId) }

    val details = vm.details
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.title ?: stringResource(R.string.movie_preview_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vm.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorFilme)
                    }
                }

                details == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.movie_preview_load_error),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                else -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                Modifier
                                    .width(110.dp)
                                    .aspectRatio(0.56f)
                                    .clip(RoundedCornerShape(8.dp)),
                            ) {
                                if (details.posterUrl != null) {
                                    AsyncImage(
                                        model = details.posterUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize().background(ColorFilme.copy(alpha = 0.15f)))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(details.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                val year = details.releaseDate?.let { java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(it) }
                                val meta = listOfNotNull(year, details.runtimeLabel.ifBlank { null }).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                                if (details.genres.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(details.genres.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = ColorFilme)
                                }
                            }
                        }

                        if (!details.synopsis.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.label_synopsis),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(details.synopsis, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(24.dp))

                        val existing = vm.existingItem
                        if (existing != null) {
                            Text(
                                stringResource(R.string.movie_preview_already_in_library),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    navController.currentBackStackEntry?.savedStateHandle?.set("item", existing)
                                    navController.navigate(Routes.FILMS_DETAIL)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorFilme),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.movie_preview_view_in_library), color = Color.White) }
                        } else if (showAdd) {
                            Text(
                                stringResource(R.string.label_status),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusOptionTile(
                                    icon = Icons.Default.CheckCircle,
                                    title = MediaStatus.WATCHED.label,
                                    subtitle = stringResource(R.string.add_film_option_watched_subtitle),
                                    selected = false,
                                    color = ColorFilme,
                                    onClick = { vm.add(MediaStatus.WATCHED) { navController.popBackStack() } },
                                )
                                StatusOptionTile(
                                    icon = Icons.Default.Queue,
                                    title = stringResource(R.string.films_tab_want_to_watch),
                                    subtitle = stringResource(R.string.add_film_option_queue_subtitle),
                                    selected = false,
                                    color = ColorFilme,
                                    onClick = {
                                        val status =
                                            if (details.releaseDate?.after(Date()) ==
                                                true
                                            ) {
                                                MediaStatus.WAITING_RELEASE
                                            } else {
                                                MediaStatus.QUEUED
                                            }
                                        vm.add(status) { navController.popBackStack() }
                                    },
                                )
                            }
                        } else {
                            Button(
                                onClick = { showAdd = true },
                                enabled = !vm.saving,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorFilme),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.movie_preview_add_to_library), color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}
