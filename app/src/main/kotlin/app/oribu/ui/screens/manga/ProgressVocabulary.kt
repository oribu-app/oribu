package app.oribu.ui.screens.manga

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.oribu.R
import app.oribu.model.MediaStatus
import app.oribu.model.MediaType
import app.oribu.ui.theme.ColorAnime
import app.oribu.ui.theme.ColorManga

/**
 * What differs between reading (manga/webtoon, counted in chapters) and watching (anime,
 * counted in episodes) on the shared progress-based add/detail screens — statuses, accent
 * color, the cache key for the total and the wording. Everything else is the same screen.
 */
internal data class ProgressVocabulary(
    val type: MediaType,
    val inProgress: MediaStatus,
    val redo: MediaStatus,
    val done: MediaStatus,
    val statuses: List<MediaStatus>,
    val addStatuses: List<MediaStatus>,
    /** Cache key holding the total number of units (chapters/episodes). */
    val totalKey: String,
    val inProgressIcon: ImageVector,
    @StringRes val queuedLabel: Int,
    @StringRes val addTitle: Int,
    @StringRes val searchPlaceholder: Int,
    @StringRes val inProgressSubtitle: Int,
    @StringRes val redoSubtitle: Int,
    @StringRes val queuedSubtitle: Int,
    @StringRes val removeItem: Int,
    @StringRes val unitCurrent: Int,
    @StringRes val unitOfTotal: Int,
    @StringRes val unitsLabel: Int,
    @StringRes val startLabel: Int,
    @StringRes val releaseStart: Int,
    @StringRes val releaseEnd: Int,
    @StringRes val endLabel: Int,
    @StringRes val previousRuns: Int,
    @StringRes val errorMaxUnits: Int,
    @StringRes val currentUnitTitle: Int,
    @StringRes val unitFieldLabel: Int,
) {
    val accent: Color
        @Composable get() = if (type == MediaType.ANIME) ColorAnime else ColorManga
}

private val READING =
    ProgressVocabulary(
        type = MediaType.MANGA,
        inProgress = MediaStatus.READING,
        redo = MediaStatus.REREADING,
        done = MediaStatus.READ,
        statuses = MediaStatus.forManga(),
        addStatuses = MediaStatus.forMangaAdd(),
        totalKey = "chapters",
        inProgressIcon = Icons.Default.MenuBook,
        queuedLabel = R.string.manga_tab_want_to_read,
        addTitle = R.string.manga_add_button,
        searchPlaceholder = R.string.add_manga_search_placeholder,
        inProgressSubtitle = R.string.add_manga_status_reading_subtitle,
        redoSubtitle = R.string.add_manga_status_rereading_subtitle,
        queuedSubtitle = R.string.add_manga_status_queued_subtitle,
        removeItem = R.string.manga_detail_remove_manga,
        unitCurrent = R.string.manga_detail_chapter_current,
        unitOfTotal = R.string.manga_detail_chapter_of_total,
        unitsLabel = R.string.manga_detail_chapters_label,
        startLabel = R.string.manga_detail_reading_start,
        releaseStart = R.string.manga_detail_publication_start,
        releaseEnd = R.string.manga_detail_publication_end,
        endLabel = R.string.manga_detail_reading_end,
        previousRuns = R.string.manga_detail_previous_readings,
        errorMaxUnits = R.string.manga_detail_error_max_chapters,
        currentUnitTitle = R.string.manga_detail_current_chapter_title,
        unitFieldLabel = R.string.manga_detail_chapter_field_label,
    )

private val WATCHING =
    ProgressVocabulary(
        type = MediaType.ANIME,
        inProgress = MediaStatus.WATCHING,
        redo = MediaStatus.REWATCHING,
        done = MediaStatus.WATCHED,
        statuses = MediaStatus.forAnime(),
        addStatuses = MediaStatus.forAnimeAdd(),
        totalKey = "episodes",
        inProgressIcon = Icons.Default.Tv,
        queuedLabel = R.string.films_tab_want_to_watch,
        addTitle = R.string.anime_add_button,
        searchPlaceholder = R.string.add_anime_search_placeholder,
        inProgressSubtitle = R.string.add_anime_status_watching_subtitle,
        redoSubtitle = R.string.add_anime_status_rewatching_subtitle,
        queuedSubtitle = R.string.add_anime_status_queued_subtitle,
        removeItem = R.string.anime_detail_remove,
        unitCurrent = R.string.anime_detail_episode_current,
        unitOfTotal = R.string.anime_detail_episode_of_total,
        unitsLabel = R.string.anime_detail_episodes_label,
        startLabel = R.string.anime_detail_watching_start,
        releaseStart = R.string.anime_detail_airing_start,
        releaseEnd = R.string.anime_detail_airing_end,
        endLabel = R.string.anime_detail_watching_end,
        previousRuns = R.string.anime_detail_previous_watches,
        errorMaxUnits = R.string.anime_detail_error_max_episodes,
        currentUnitTitle = R.string.anime_detail_current_episode_title,
        unitFieldLabel = R.string.anime_detail_episode_field_label,
    )

internal fun progressVocabularyFor(type: MediaType): ProgressVocabulary = if (type == MediaType.ANIME) WATCHING else READING

/** Icon + subtitle for a status option on the "add" sheet. */
@Composable
internal fun ProgressVocabulary.statusOption(status: MediaStatus): Pair<ImageVector, String> =
    when (status) {
        inProgress -> inProgressIcon to stringResource(inProgressSubtitle)
        redo -> Icons.Default.Replay to stringResource(redoSubtitle)
        MediaStatus.QUEUED -> Icons.Default.Bookmark to stringResource(queuedSubtitle)
        else -> Icons.Default.Bookmark to ""
    }
