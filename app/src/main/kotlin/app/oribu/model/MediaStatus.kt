package app.oribu.model

import androidx.compose.ui.graphics.Color

enum class MediaStatus(
    val label: String,
    val dbValue: String,
) {
    // Games
    COMPLETED("Completed", "completado"),
    FINISHED("Finished", "finalizado"),
    PLAYING("Playing", "jogando"),
    REPLAYING("Replaying", "rejogando"),
    PLATINUM("Platinum", "platinado"),

    // Movies
    WATCHED("Watched", "assistido"),
    WATCHING("Watching", "assistindo"),
    REWATCHING("Rewatching", "reassistindo"),

    // Series
    CONCLUDED("Concluded", "concluida"),
    HISTORY("History", "historico"),
    WAITING_EPISODES("Waiting Episodes", "aguardandoEpisodios"),

    // Manga / Books
    READ("Read", "lido"),
    READING("Reading", "lendo"),
    REREADING("Rereading", "relendo"),
    ON_HOLD("On Hold", "pausado"),

    // All
    QUEUED("Queued", "naFila"),
    DROPPED("Dropped", "abandonado"),
    WAITING_RELEASE("Waiting Release", "aguardandoLancamento"),
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

        fun forBook() = listOf(READING, REREADING, READ, QUEUED, DROPPED)

        fun forBookAdd() = listOf(READING, REREADING, QUEUED)
    }
}
