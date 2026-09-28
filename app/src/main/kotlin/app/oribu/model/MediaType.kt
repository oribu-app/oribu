package app.oribu.model

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.oribu.R
import app.oribu.ui.theme.AppThemeController

enum class MediaType(
    @StringRes val labelRes: Int,
    @StringRes val labelPluralRes: Int,
    val dbValue: String,
) {
    GAME(R.string.media_type_game, R.string.games_title, "jogo"),
    MANGA(R.string.media_type_manga, R.string.manga_title, "manga"),
    WEBTOON(R.string.media_type_webtoon, R.string.media_type_webtoon_plural, "webtoon"),
    SERIES(R.string.media_type_series, R.string.series_title, "serie"),
    MOVIE(R.string.media_type_movie, R.string.films_title, "filme"),
    BOOK(R.string.media_type_book, R.string.books_title, "livro"),
    ;

    private val fixedColor: Color get() =
        when (this) {
            GAME -> Color(0xFF7B1FA2)
            MANGA, WEBTOON -> Color(0xFFE91E63)
            SERIES -> Color(0xFF1976D2)
            MOVIE -> Color(0xFFFF6F00)
            BOOK -> Color(0xFF388E3C)
        }

    val color: Color
        @Composable get() =
            if (AppThemeController.useThemeAccentColor) MaterialTheme.colorScheme.primary else fixedColor

    companion object {
        fun fromDb(value: String) = entries.firstOrNull { it.dbValue == value } ?: GAME
    }
}

val MediaType.label: String
    @Composable get() = stringResource(labelRes)

val MediaType.labelPlural: String
    @Composable get() = stringResource(labelPluralRes)
