package app.oribu.ui.navigation

import app.oribu.model.MediaType

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GAMES = "games"
    const val GAMES_ADD = "games/add"
    const val GAMES_DETAIL = "games/detail"
    const val GAMES_COVER = "games/cover"
    const val FILMS = "films"
    const val FILMS_ADD = "films/add"
    const val FILMS_DETAIL = "films/detail"
    const val FILMS_PREVIEW = "films/preview"
    const val SERIES = "series"
    const val SERIES_ADD = "series/add"
    const val SERIES_DETAIL = "series/detail"
    const val MANGA = "manga"
    const val MANGA_ADD = "manga/add"
    const val MANGA_DETAIL = "manga/detail"
    const val ANIME_ADD = "anime/add"
    const val ANIME_DETAIL = "anime/detail"
    const val BOOKS = "books"
    const val BOOKS_ADD = "books/add"
    const val BOOKS_DETAIL = "books/detail"
    const val BOOKS_ADD_QUOTE = "books/quote"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SETTINGS_GENERAL = "settings/general"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_INTEGRATIONS = "settings/integrations"
    const val SETTINGS_TRACKING = "settings/tracking"
    const val SETTINGS_DATA = "settings/data"
    const val SETTINGS_PLATFORMS = "settings/platforms"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val STATS_DETAILS = "stats/details"
    const val STATS_FILTERED_LIST = "stats/filteredList"
    const val CALENDAR = "calendar"
    const val ABOUT = "about"
    const val ABOUT_CREDITS = "about/credits"
    const val ABOUT_LICENSES = "about/licenses"
    const val ABOUT_LICENSE_DETAIL = "about/licenses/detail"
    const val ANOTACOES = "anotacoes"
}

/** Detail screen route for an item of this type (the item itself goes in savedStateHandle "item"). */
val MediaType.detailRoute: String
    get() =
        when (this) {
            MediaType.GAME -> Routes.GAMES_DETAIL
            MediaType.MOVIE -> Routes.FILMS_DETAIL
            MediaType.SERIES -> Routes.SERIES_DETAIL
            MediaType.MANGA, MediaType.WEBTOON -> Routes.MANGA_DETAIL
            MediaType.ANIME -> Routes.ANIME_DETAIL
            MediaType.BOOK -> Routes.BOOKS_DETAIL
        }
