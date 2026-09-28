package app.oribu.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.oribu.R

enum class MediaStatus(
    @StringRes val labelRes: Int,
    val dbValue: String,
) {
    // Games
    COMPLETED(R.string.status_completed, "completado"),
    FINISHED(R.string.status_finished, "finalizado"),
    PLAYING(R.string.status_playing, "jogando"),
    REPLAYING(R.string.status_replaying, "rejogando"),
    PLATINUM(R.string.status_platinum, "platinado"),

    // Movies
    WATCHED(R.string.status_watched, "assistido"),
    WATCHING(R.string.status_watching, "assistindo"),
    REWATCHING(R.string.status_rewatching, "reassistindo"),

    // Series
    CONCLUDED(R.string.status_concluded, "concluida"),
    HISTORY(R.string.status_history, "historico"),
    WAITING_EPISODES(R.string.status_waiting_episodes, "aguardandoEpisodios"),

    // Manga / Books
    READ(R.string.status_read, "lido"),
    READING(R.string.status_reading, "lendo"),
    REREADING(R.string.status_rereading, "relendo"),
    ON_HOLD(R.string.status_on_hold, "pausado"),

    // All
    QUEUED(R.string.status_queued, "naFila"),
    DROPPED(R.string.status_dropped, "abandonado"),
    WAITING_RELEASE(R.string.status_waiting_release, "aguardandoLancamento"),
    ;

    val color: Color get() =
        when (this) {
            COMPLETED, FINISHED, WATCHED, CONCLUDED, HISTORY, READ -> Color(0xFF4CAF50)
            PLAYING, WATCHING, READING -> Color(0xFF2196F3)
            REPLAYING, REWATCHING, REREADING -> Color(0xFF00BCD4)
            PLATINUM -> Color(0xFF9C64FE)
            WAITING_EPISODES, QUEUED -> Color(0xFFFF9800)
            ON_HOLD, WAITING_RELEASE -> Color(0xFF9E9E9E)
            DROPPED -> Color(0xFFF44336)
        }

    companion object {
        fun fromDb(value: String): MediaStatus = entries.firstOrNull { it.dbValue == value } ?: QUEUED

        fun forSteam() = listOf(PLAYING, REPLAYING, FINISHED, COMPLETED, QUEUED)

        fun forPlayStation() = listOf(PLAYING, REPLAYING, FINISHED, PLATINUM, QUEUED)

        fun forNintendo() = listOf(PLAYING, REPLAYING, FINISHED, COMPLETED, QUEUED)

        fun forOtherGames() = listOf(PLAYING, REPLAYING, FINISHED, QUEUED)

        fun forMovie() = listOf(WATCHED, REWATCHING, QUEUED, WAITING_RELEASE)

        fun forSeries() = listOf(WATCHING, REWATCHING, QUEUED, HISTORY)

        fun forSeriesAdd() = listOf(WATCHING, REWATCHING, QUEUED, HISTORY)

        fun forManga() = listOf(READING, REREADING, ON_HOLD, READ, QUEUED)

        fun forMangaAdd() = listOf(READING, REREADING, QUEUED)

        fun forAnime() = listOf(WATCHING, REWATCHING, ON_HOLD, WATCHED, QUEUED)

        fun forAnimeAdd() = listOf(WATCHING, REWATCHING, QUEUED)

        fun forBook() = listOf(READING, REREADING, READ, QUEUED, DROPPED)

        fun forBookAdd() = listOf(READING, REREADING, QUEUED)
    }
}

val MediaStatus.label: String
    @Composable get() = stringResource(labelRes)
