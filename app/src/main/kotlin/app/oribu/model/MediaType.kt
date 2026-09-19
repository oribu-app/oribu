package app.oribu.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.oribu.ui.theme.AppThemeController

enum class MediaType(
    val label: String,
    val dbValue: String,
) {
    GAME("Game", "jogo"),
    MANGA("Manga", "manga"),
    WEBTOON("Webtoon", "webtoon"),
    SERIES("Series", "serie"),
    MOVIE("Movie", "filme"),
    BOOK("Book", "livro"),
    ;

    val labelPlural: String get() =
        when (this) {
            GAME -> "Games"
            MANGA -> "Manga"
            WEBTOON -> "Webtoons"
            SERIES -> "Series"
            MOVIE -> "Movies"
            BOOK -> "Books"
        }

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
