package com.hobbiesvault.ui.screens.films

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.hobbiesvault.data.db.DB
import com.hobbiesvault.model.MediaItem
import com.hobbiesvault.model.MediaStatus
import com.hobbiesvault.model.MediaType
import com.hobbiesvault.service.ApiServices
import com.hobbiesvault.service.MediaCacheService
import com.hobbiesvault.service.TmdbMovieDetails
import com.hobbiesvault.ui.components.StatusOptionTile
import com.hobbiesvault.ui.navigation.Routes
import com.hobbiesvault.ui.theme.ColorFilme
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
            details = withContext(Dispatchers.IO) {
                runCatching { ApiServices.tmdb.getMovieDetails(tmdbId) }.getOrNull()
            }
            loading = false
        }
    }

    fun add(status: MediaStatus, onDone: (MediaItem) -> Unit) {
        val d = details ?: return
        viewModelScope.launch {
            saving = true
            val item = MediaItem(
                type        = MediaType.MOVIE,
                title       = d.title,
                status      = status,
                coverUrl    = d.posterUrl,
                addedDate   = Date(),
                externalId  = d.id.toString(),
                apiSource   = "tmdb",
                releaseDate = d.releaseDate,
                genre       = d.genres.firstOrNull(),
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
                title = { Text(details?.title ?: "Filme") },
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
                        Text("Não foi possível carregar este filme", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                                        model              = details.posterUrl,
                                        contentDescription = null,
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier.fillMaxSize(),
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
                                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                if (details.genres.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(details.genres.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = ColorFilme)
                                }
                            }
                        }

                        if (!details.synopsis.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Sinopse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(details.synopsis, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(24.dp))

                        val existing = vm.existingItem
                        if (existing != null) {
                            Text(
                                "Já está na sua biblioteca",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick  = {
                                    navController.currentBackStackEntry?.savedStateHandle?.set("item", existing)
                                    navController.navigate(Routes.FILMS_DETAIL)
                                },
                                colors   = ButtonDefaults.buttonColors(containerColor = ColorFilme),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Ver na biblioteca", color = Color.White) }
                        } else if (showAdd) {
                            Text("Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusOptionTile(
                                    icon     = Icons.Default.CheckCircle,
                                    title    = "Assistido",
                                    subtitle = "Já assisti este filme",
                                    selected = false,
                                    color    = ColorFilme,
                                    onClick  = { vm.add(MediaStatus.WATCHED) { navController.popBackStack() } },
                                )
                                StatusOptionTile(
                                    icon     = Icons.Default.Queue,
                                    title    = "Quero Assistir",
                                    subtitle = "Adicionar à fila",
                                    selected = false,
                                    color    = ColorFilme,
                                    onClick  = {
                                        val status = if (details.releaseDate?.after(Date()) == true) MediaStatus.WAITING_RELEASE else MediaStatus.QUEUED
                                        vm.add(status) { navController.popBackStack() }
                                    },
                                )
                            }
                        } else {
                            Button(
                                onClick  = { showAdd = true },
                                enabled  = !vm.saving,
                                colors   = ButtonDefaults.buttonColors(containerColor = ColorFilme),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Adicionar à biblioteca", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}
