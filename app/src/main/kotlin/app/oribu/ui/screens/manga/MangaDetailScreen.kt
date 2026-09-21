package app.oribu.ui.screens.manga

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.data.db.DB
import app.oribu.model.MangaReview
import app.oribu.model.MediaItem
import app.oribu.model.MediaStatus
import app.oribu.service.MediaCacheService
import app.oribu.ui.components.AnotacoesSection
import app.oribu.ui.components.StarRatingDisplay
import app.oribu.ui.components.StarRatingPicker
import app.oribu.ui.locale.formatDate
import app.oribu.ui.navigation.navigateToAnotacoes
import app.oribu.ui.navigation.rememberAnotacoesResult
import app.oribu.ui.theme.ColorManga
import app.oribu.ui.theme.CoverThemedSurface
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MangaDetailViewModel : ViewModel() {
    var mediaItem by mutableStateOf<MediaItem?>(null)
    var cache by mutableStateOf<Map<String, Any?>?>(null)
    var loadingCache by mutableStateOf(true)
    var reviewHistory by mutableStateOf<List<MangaReview>>(emptyList())
    private var initialized = false

    fun init(initial: MediaItem) {
        if (initialized) return
        initialized = true
        mediaItem = initial
        viewModelScope.launch {
            cache = MediaCacheService.load(initial)
            loadingCache = false
        }
        initial.id?.let { id ->
            viewModelScope.launch { reviewHistory = DB.repo.mangaReviewHistory(id) }
        }
        MediaCacheService.doubleCheck(initial) {
            val updated = MediaCacheService.load(initial)
            if (updated != null) cache = updated
        }
    }

    fun setStatus(newStatus: MediaStatus) {
        val current = mediaItem ?: return
        viewModelScope.launch {
            // Ao começar uma releitura, a avaliação/resenha anterior vira histórico —
            // o usuário está formando um novo julgamento, não editando o antigo.
            if (newStatus == MediaStatus.REREADING && current.status == MediaStatus.READ) {
                current.id?.let { id ->
                    DB.repo.archiveMangaReview(
                        mediaItemId = id,
                        rating = current.rating,
                        reviewTitle = current.reviewTitle,
                        reviewText = current.notes,
                        completedAt = current.completionDate ?: java.util.Date(),
                    )
                    reviewHistory = DB.repo.mangaReviewHistory(id)
                }
            }
            val clearingForReread = newStatus == MediaStatus.REREADING && current.status == MediaStatus.READ
            val updated =
                current.copy(
                    status = newStatus,
                    completionDate =
                        if (newStatus ==
                            MediaStatus.READ
                        ) {
                            current.completionDate ?: java.util.Date()
                        } else {
                            current.completionDate
                        },
                    readingStartDate =
                        if (newStatus ==
                            MediaStatus.READING
                        ) {
                            current.readingStartDate ?: java.util.Date()
                        } else {
                            current.readingStartDate
                        },
                    rating = if (clearingForReread) null else current.rating,
                    reviewTitle = if (clearingForReread) null else current.reviewTitle,
                    notes = if (clearingForReread) null else current.notes,
                    rereadingDate = if (clearingForReread) java.util.Date() else current.rereadingDate,
                )
            mediaItem = updated
            DB.repo.update(updated)
        }
    }

    fun setProgress(
        chapter: Int,
        totalChapters: Int?,
    ) {
        val current = mediaItem ?: return
        val clamped = totalChapters?.let { chapter.coerceIn(0, it) } ?: chapter.coerceAtLeast(0)
        val updated = current.copy(currentProgress = clamped)
        mediaItem = updated
        viewModelScope.launch { DB.repo.update(updated) }
    }

    fun setReadingStartDate(date: java.util.Date) {
        val current = mediaItem ?: return
        val updated = current.copy(readingStartDate = date)
        mediaItem = updated
        viewModelScope.launch { DB.repo.update(updated) }
    }

    fun toggleFavorite() {
        val current = mediaItem ?: return
        val updated = current.copy(favorite = !current.favorite)
        mediaItem = updated
        viewModelScope.launch { DB.repo.update(updated) }
    }

    fun setPersonalNotes(text: String) {
        val current = mediaItem ?: return
        val updated = current.copy(personalNotes = text.ifBlank { null })
        mediaItem = updated
        viewModelScope.launch { DB.repo.update(updated) }
    }

    fun refreshCache() {
        val current = mediaItem ?: return
        viewModelScope.launch {
            MediaCacheService.fetchAndPersist(current)
            cache = MediaCacheService.load(current)
        }
    }

    fun savePersonal(
        rating: Double?,
        reviewTitle: String?,
        notes: String?,
    ) {
        val current = mediaItem ?: return
        val updated =
            current.copy(
                rating = rating,
                reviewTitle = reviewTitle?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() },
            )
        mediaItem = updated
        viewModelScope.launch { DB.repo.update(updated) }
    }

    fun delete(onDone: () -> Unit) {
        val current = mediaItem ?: return
        viewModelScope.launch {
            current.id?.let {
                DB.repo.delete(it)
                DB.cache.delete(it)
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    navController: NavController,
    initialItem: MediaItem,
    vm: MangaDetailViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.init(initialItem) }

    val anotacoesResult = rememberAnotacoesResult(navController)
    LaunchedEffect(anotacoesResult) { anotacoesResult?.let { vm.setPersonalNotes(it) } }

    val mediaItem = vm.mediaItem ?: initialItem
    val cache = vm.cache

    var showDelete by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var chapterInput by remember { mutableStateOf("") }
    var editingPersonal by remember { mutableStateOf(false) }
    var pendingRating by remember { mutableStateOf(0) }
    var pendingReviewTitle by remember { mutableStateOf("") }
    var pendingNotes by remember { mutableStateOf("") }
    val coverUrl = cache?.get("coverUrl") as? String ?: mediaItem.coverUrl
    val synopsis = cache?.get("synopsis") as? String
    val chapters = (cache?.get("chapters") as? Number)?.toInt() ?: mediaItem.totalProgress
    val volumes = (cache?.get("volumes") as? Number)?.toInt()
    val serializationStatus = cache?.get("serializationStatus") as? String
    val format = cache?.get("format") as? String
    val genres = (cache?.get("genres") as? List<*>)?.filterIsInstance<String>()
    val authors = (cache?.get("authors") as? List<*>)?.filterIsInstance<String>()
    val startDateMs = (cache?.get("startDateMs") as? Number)?.toLong()
    val endDateMs = (cache?.get("endDateMs") as? Number)?.toLong()

    @Suppress("UNCHECKED_CAST")
    val staffList = cache?.get("staff") as? List<Map<String, Any?>>

    @Suppress("UNCHECKED_CAST")
    val characters = cache?.get("characters") as? List<Map<String, Any?>>
    val synonyms =
        (cache?.get("synonyms") as? List<*>)
            ?.filterIsInstance<String>()
            ?.filter { it.isNotBlank() && !it.equals(mediaItem.title, ignoreCase = true) }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val coverWidth = (screenWidth * 0.58f).coerceIn(160.dp, 230.dp)
    val coverHeight = coverWidth / 0.7f

    CoverThemedSurface(coverUrl) {
        Scaffold { _ ->
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    // ── Header: blurred bg + centered cover + title ───────────────
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(coverHeight + 160.dp),
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .blur(28.dp),
                            )
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
                            Column(
                                Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .shadow(30.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp)),
                                ) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.width(coverWidth).height(coverHeight),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    mediaItem.title,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 25.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!authors.isNullOrEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        authors.joinToString(", "),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // ── Status button + "..." button (centered) ───────────────────
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                Button(
                                    onClick = { showStatusMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorManga),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.24f)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                ) {
                                    Icon(Icons.Default.UnfoldMore, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(mangaStatusLabel(mediaItem.status), fontSize = 13.sp)
                                }
                                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                    MediaStatus
                                        .forManga()
                                        .filter { it != MediaStatus.READ || serializationStatus != "Ongoing" }
                                        .forEach { s ->
                                            val selected = s == mediaItem.status
                                            DropdownMenuItem(
                                                text = { Text(mangaStatusLabel(s)) },
                                                trailingIcon = {
                                                    if (selected) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            null,
                                                            tint = ColorManga,
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    vm.setStatus(s)
                                                    showStatusMenu = false
                                                },
                                            )
                                        }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreHoriz, null)
                                }
                                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_refresh)) },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = {
                                            vm.refreshCache()
                                            showMoreMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.manga_detail_edit_progress)) },
                                        leadingIcon = { Icon(Icons.Default.Bookmark, null) },
                                        onClick = {
                                            chapterInput = (mediaItem.currentProgress ?: 0).toString()
                                            showChapterDialog = true
                                            showMoreMenu =
                                                false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.manga_detail_edit_start_date)) },
                                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                        onClick = {
                                            showStartDatePicker = true
                                            showMoreMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.label_notes)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                                        onClick = {
                                            navController.navigateToAnotacoes(mediaItem)
                                            showMoreMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (mediaItem.favorite) R.string.action_remove_favorite else R.string.action_favorite,
                                                ),
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (mediaItem.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                null,
                                            )
                                        },
                                        onClick = {
                                            vm.toggleFavorite()
                                            showMoreMenu = false
                                        },
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.manga_detail_remove_manga),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showDelete = true
                                            showMoreMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // ── Sinopse ──────────────────────────────────────────────────
                    if (synopsis != null) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                MangaSectionTitle(stringResource(R.string.label_synopsis))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    synopsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 22.sp,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 5,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(if (synopsisExpanded) R.string.action_see_less else R.string.action_see_more),
                                    color = ColorManga,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded },
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    // ── Progresso de capítulos ────────────────────────────────────
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MangaSectionTitle(stringResource(R.string.manga_detail_progress))
                                IconButton(
                                    onClick = {
                                        chapterInput = (mediaItem.currentProgress ?: 0).toString()
                                        showChapterDialog = true
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.action_edit),
                                        tint = ColorManga,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            val displayedProgress =
                                (mediaItem.currentProgress ?: 0).let { p ->
                                    if (chapters != null) p.coerceIn(0, chapters) else p
                                }
                            if (chapters != null && chapters > 0) {
                                LinearProgressIndicator(
                                    progress = { (displayedProgress.toFloat() / chapters.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ColorManga,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    strokeCap = StrokeCap.Butt,
                                )
                            }
                            Text(
                                if (chapters != null) {
                                    stringResource(R.string.manga_detail_chapter_of_total, displayedProgress, chapters)
                                } else {
                                    stringResource(R.string.manga_detail_chapter_current, displayedProgress)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Informações ───────────────────────────────────────────────
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            MangaSectionTitle(stringResource(R.string.label_information))
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (format != null) MangaInfoCard(stringResource(R.string.manga_detail_format), format)
                                if (!genres.isNullOrEmpty()) {
                                    MangaInfoCard(stringResource(R.string.label_genre), genres.take(3).joinToString(", "))
                                }
                                if (volumes != null) MangaInfoCard("Volumes", "$volumes")
                                if (chapters != null) MangaInfoCard(stringResource(R.string.manga_detail_chapters_label), "$chapters")
                                if (serializationStatus != null) MangaInfoCard(stringResource(R.string.label_status), serializationStatus)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Datas ─────────────────────────────────────────────────────
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            MangaSectionTitle(stringResource(R.string.label_dates))
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MangaInfoCard(stringResource(R.string.label_added), formatDate(mediaItem.addedDate.time))
                                if (mediaItem.readingStartDate != null) {
                                    MangaInfoCard(
                                        stringResource(R.string.manga_detail_reading_start),
                                        formatDate(mediaItem.readingStartDate.time),
                                    )
                                }
                                if (startDateMs !=
                                    null
                                ) {
                                    MangaInfoCard(
                                        stringResource(R.string.manga_detail_publication_start),
                                        formatDate(startDateMs),
                                    )
                                }
                                if (endDateMs != null) {
                                    MangaInfoCard(
                                        stringResource(R.string.manga_detail_publication_end),
                                        formatDate(endDateMs),
                                    )
                                }
                                if (mediaItem.completionDate != null) {
                                    MangaInfoCard(
                                        stringResource(R.string.manga_detail_reading_end),
                                        formatDate(mediaItem.completionDate.time),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Avaliação (só disponível quando Lido) ─────────────────────
                    if (mediaItem.status == MediaStatus.READ) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    MangaSectionTitle(stringResource(R.string.manga_detail_rating))
                                    if (!editingPersonal) {
                                        TextButton(
                                            onClick = {
                                                pendingRating = (mediaItem.rating ?: 0.0).toInt()
                                                pendingReviewTitle = mediaItem.reviewTitle ?: ""
                                                pendingNotes = mediaItem.notes ?: ""
                                                editingPersonal = true
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                        ) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Card(shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(16.dp)) {
                                        if (editingPersonal) {
                                            StarRatingPicker(rating = pendingRating, onRatingChange = { pendingRating = it })
                                        } else if (mediaItem.rating != null) {
                                            StarRatingDisplay(rating = mediaItem.rating.toInt())
                                        } else {
                                            Text(
                                                stringResource(R.string.manga_detail_no_rating),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // ── Resenha ────────────────────────────────────────────────
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                MangaSectionTitle(stringResource(R.string.manga_detail_review))
                                Spacer(Modifier.height(10.dp))
                                Card(shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (editingPersonal) {
                                            OutlinedTextField(
                                                value = pendingReviewTitle,
                                                onValueChange = { pendingReviewTitle = it },
                                                placeholder = {
                                                    Text(
                                                        stringResource(R.string.manga_detail_review_title_placeholder),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            OutlinedTextField(
                                                value = pendingNotes,
                                                onValueChange = { pendingNotes = it },
                                                placeholder = {
                                                    Text(
                                                        stringResource(R.string.manga_detail_review_text_placeholder),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                },
                                                minLines = 3,
                                                maxLines = 6,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                OutlinedButton(
                                                    onClick = { editingPersonal = false },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                ) { Text(stringResource(R.string.action_cancel)) }
                                                Button(
                                                    onClick = {
                                                        vm.savePersonal(
                                                            rating = if (pendingRating > 0) pendingRating.toDouble() else null,
                                                            reviewTitle = pendingReviewTitle,
                                                            notes = pendingNotes,
                                                        )
                                                        editingPersonal = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = ColorManga),
                                                ) { Text(stringResource(R.string.action_save)) }
                                            }
                                        } else {
                                            if (!mediaItem.reviewTitle.isNullOrBlank()) {
                                                Text(
                                                    mediaItem.reviewTitle!!,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            if (!mediaItem.notes.isNullOrBlank()) {
                                                Text(
                                                    mediaItem.notes!!,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                                    fontStyle = FontStyle.Italic,
                                                )
                                            }
                                            if (mediaItem.reviewTitle.isNullOrBlank() && mediaItem.notes.isNullOrBlank()) {
                                                Text(
                                                    stringResource(R.string.manga_detail_no_review),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ── Leituras anteriores (histórico de releituras) ─────────────
                    if (vm.reviewHistory.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                MangaSectionTitle(stringResource(R.string.manga_detail_previous_readings))
                                Spacer(Modifier.height(10.dp))
                                Card(shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        vm.reviewHistory.forEachIndexed { index, review ->
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    formatDate(review.completedAt.time),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                )
                                                if (review.rating != null) {
                                                    StarRatingDisplay(rating = review.rating.toInt())
                                                }
                                                if (!review.reviewTitle.isNullOrBlank()) {
                                                    Text(
                                                        review.reviewTitle,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                if (!review.reviewText.isNullOrBlank()) {
                                                    Text(
                                                        review.reviewText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                                        fontStyle = FontStyle.Italic,
                                                    )
                                                }
                                            }
                                            if (index != vm.reviewHistory.lastIndex) HorizontalDivider()
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ── Personagens ───────────────────────────────────────────────
                    if (!characters.isNullOrEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                MangaSectionTitle(stringResource(R.string.manga_detail_characters))
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(characters) { char ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(72.dp),
                                        ) {
                                            AsyncImage(
                                                model = char["photoUrl"] as? String,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier =
                                                    Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                char["name"] as? String ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    // ── Staff ─────────────────────────────────────────────────────
                    if (!staffList.isNullOrEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                MangaSectionTitle("Staff")
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(staffList) { member ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(72.dp),
                                        ) {
                                            AsyncImage(
                                                model = member["photoUrl"] as? String,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier =
                                                    Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                member["name"] as? String ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                member["role"] as? String ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    // ── Sinônimos (sempre por último) ───────────────────────────────
                    if (!synonyms.isNullOrEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                MangaSectionTitle(stringResource(R.string.manga_detail_synonyms))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    synonyms.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 20.sp,
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    item {
                        AnotacoesSection(mediaItem.personalNotes) { navController.navigateToAnotacoes(mediaItem) }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }

                // ── Floating nav: back ────────────────────────────────────────────
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.statusBarsPadding().padding(4.dp),
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.54f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showChapterDialog) {
        val chapterNum = chapterInput.toIntOrNull()
        val chapterError =
            when {
                chapterInput.isBlank() -> null
                chapterNum == null -> stringResource(R.string.manga_detail_error_invalid_number)
                chapterNum < 0 -> stringResource(R.string.manga_detail_error_negative)
                chapters != null && chapterNum > chapters -> stringResource(R.string.manga_detail_error_max_chapters, chapters)
                else -> null
            }
        val canSaveChapter = chapterError == null && chapterInput.isNotBlank()
        AlertDialog(
            onDismissRequest = { showChapterDialog = false },
            title = { Text(stringResource(R.string.manga_detail_current_chapter_title)) },
            text = {
                OutlinedTextField(
                    value = chapterInput,
                    onValueChange = { chapterInput = it },
                    label = { Text(stringResource(R.string.manga_detail_chapter_field_label)) },
                    isError = chapterError != null,
                    supportingText = chapterError?.let { { Text(it) } },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = canSaveChapter,
                    onClick = {
                        chapterNum?.let { vm.setProgress(it, chapters) }
                        showChapterDialog = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { showChapterDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showStartDatePicker) {
        MangaDatePickerDialog(
            initial = mediaItem.readingStartDate?.time,
            onConfirm = {
                vm.setReadingStartDate(java.util.Date(it))
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.manga_detail_remove_manga)) },
            text = { Text(stringResource(R.string.manga_detail_remove_confirm, mediaItem.title)) },
            confirmButton = {
                Button(
                    onClick = { vm.delete { navController.popBackStack() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_remove)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
fun mangaStatusLabel(status: MediaStatus): String =
    when (status) {
        MediaStatus.QUEUED -> stringResource(R.string.manga_tab_want_to_read)
        MediaStatus.ON_HOLD -> stringResource(R.string.manga_tab_on_hold)
        else -> status.label
    }

@Composable
private fun MangaSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
    )
}

@Composable
private fun MangaInfoRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MangaInfoCard(
    label: String,
    value: String,
) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            MangaInfoRow(label, value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaDatePickerDialog(
    initial: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(it) } ?: onDismiss() }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}
