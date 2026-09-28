package app.oribu.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import app.oribu.R
import app.oribu.model.MediaItem
import app.oribu.ui.screens.AboutLibraryLicenseScreen
import app.oribu.ui.screens.AboutLicenseScreen
import app.oribu.ui.screens.AboutScreen
import app.oribu.ui.screens.AnotacoesScreen
import app.oribu.ui.screens.CalendarScreen
import app.oribu.ui.screens.HistoryScreen
import app.oribu.ui.screens.HomeScreen
import app.oribu.ui.screens.SearchScreen
import app.oribu.ui.screens.books.AddBookScreen
import app.oribu.ui.screens.books.AddQuoteScreen
import app.oribu.ui.screens.books.BookDetailScreen
import app.oribu.ui.screens.books.BooksScreen
import app.oribu.ui.screens.films.AddFilmScreen
import app.oribu.ui.screens.films.FilmDetailScreen
import app.oribu.ui.screens.films.FilmsScreen
import app.oribu.ui.screens.films.MoviePreviewScreen
import app.oribu.ui.screens.games.AddGameScreen
import app.oribu.ui.screens.games.CoverPickerScreen
import app.oribu.ui.screens.games.GameDetailScreen
import app.oribu.ui.screens.games.GamesScreen
import app.oribu.ui.screens.manga.AddMangaScreen
import app.oribu.ui.screens.manga.MangaDetailScreen
import app.oribu.ui.screens.manga.MangaScreen
import app.oribu.ui.screens.onboarding.OnboardingScreen
import app.oribu.ui.screens.series.AddSeriesScreen
import app.oribu.ui.screens.series.SeriesDetailScreen
import app.oribu.ui.screens.series.SeriesScreen
import app.oribu.ui.screens.settings.SettingsAppearanceScreen
import app.oribu.ui.screens.settings.SettingsDataScreen
import app.oribu.ui.screens.settings.SettingsGeneralScreen
import app.oribu.ui.screens.settings.SettingsIntegrationsScreen
import app.oribu.ui.screens.settings.SettingsNotificationsScreen
import app.oribu.ui.screens.settings.SettingsPlatformsScreen
import app.oribu.ui.screens.settings.SettingsScreen
import app.oribu.ui.screens.settings.SettingsTrackingScreen
import app.oribu.ui.screens.stats.StatsDetailsScreen
import app.oribu.ui.screens.stats.StatsFilteredListScreen
import app.oribu.ui.screens.stats.StatsScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
)

@Composable
fun MainNavGraph(startDestination: String = Routes.HOME) {
    val bottomNavItems =
        listOf(
            BottomNavItem(Routes.GAMES, stringResource(R.string.games_title), Icons.Outlined.SportsEsports, Icons.Filled.SportsEsports),
            BottomNavItem(Routes.FILMS, stringResource(R.string.films_title), Icons.Outlined.Movie, Icons.Filled.Movie),
            BottomNavItem(Routes.SERIES, stringResource(R.string.series_title), Icons.Outlined.Tv, Icons.Filled.Tv),
            BottomNavItem(Routes.MANGA, stringResource(R.string.manga_title), Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
            BottomNavItem(Routes.BOOKS, stringResource(R.string.books_title), Icons.Outlined.Book, Icons.Filled.Book),
        )
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shellRoutes = bottomNavItems.map { it.route }.toSet() + Routes.HOME
    val showBottomBar = currentRoute in shellRoutes

    // On the home route, no item is selected (selectedIndex = null)
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 }

    // Swipe entre hobbies só acontece quando a tela atual já esgotou suas próprias abas
    // internas de status (ver *Screen.kt) — cada uma delas consome o gesto primeiro.
    fun onSwipeToNextHobby() {
        val next = (selectedIndex ?: 0) + 1
        if (next < bottomNavItems.size) navController.navigateToHobbyTab(bottomNavItems[next].route)
    }

    fun onSwipeToPrevHobby() {
        val prev = (selectedIndex ?: 0) - 1
        if (prev >= 0) navController.navigateToHobbyTab(bottomNavItems[prev].route)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 0.dp) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val selected = selectedIndex == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToHobbyTab(item.route) },
                            icon = {
                                Icon(
                                    if (selected) item.activeIcon else item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.GAMES) {
                GamesScreen(navController, onSwipeToNextHobby = ::onSwipeToNextHobby, onSwipeToPrevHobby = ::onSwipeToPrevHobby)
            }
            composable(Routes.FILMS) {
                FilmsScreen(navController, onSwipeToNextHobby = ::onSwipeToNextHobby, onSwipeToPrevHobby = ::onSwipeToPrevHobby)
            }
            composable(Routes.SERIES) {
                SeriesScreen(navController, onSwipeToNextHobby = ::onSwipeToNextHobby, onSwipeToPrevHobby = ::onSwipeToPrevHobby)
            }
            composable(Routes.MANGA) {
                MangaScreen(navController, onSwipeToNextHobby = ::onSwipeToNextHobby, onSwipeToPrevHobby = ::onSwipeToPrevHobby)
            }
            composable(Routes.BOOKS) {
                BooksScreen(navController, onSwipeToNextHobby = ::onSwipeToNextHobby, onSwipeToPrevHobby = ::onSwipeToPrevHobby)
            }

            composable(Routes.GAMES_ADD) { AddGameScreen(navController) }
            composable(Routes.GAMES_DETAIL) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<MediaItem>("item")
                if (item != null) GameDetailScreen(navController, item)
            }
            composable(Routes.GAMES_COVER) { CoverPickerScreen(navController) }

            composable(Routes.FILMS_ADD) { AddFilmScreen(navController) }
            composable(Routes.FILMS_DETAIL) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<MediaItem>("item")
                if (item != null) FilmDetailScreen(navController, item)
            }
            composable(Routes.FILMS_PREVIEW) {
                val tmdbId = navController.previousBackStackEntry?.savedStateHandle?.get<Int>("tmdbId")
                if (tmdbId != null) MoviePreviewScreen(navController, tmdbId)
            }

            composable(Routes.SERIES_ADD) { AddSeriesScreen(navController) }
            composable(Routes.SERIES_DETAIL) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<MediaItem>("item")
                if (item != null) SeriesDetailScreen(navController, item)
            }

            composable(Routes.MANGA_ADD) { AddMangaScreen(navController) }
            composable(Routes.MANGA_DETAIL) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<MediaItem>("item")
                if (item != null) MangaDetailScreen(navController, item)
            }

            composable(Routes.BOOKS_ADD) { AddBookScreen(navController) }
            composable(Routes.BOOKS_DETAIL) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<MediaItem>("item")
                if (item != null) BookDetailScreen(navController, item)
            }
            composable(Routes.BOOKS_ADD_QUOTE) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<MediaItem>("quoteBook")
                if (item != null) AddQuoteScreen(navController, item)
            }

            composable(Routes.SEARCH) { SearchScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.SETTINGS_GENERAL) { SettingsGeneralScreen(navController) }
            composable(Routes.SETTINGS_APPEARANCE) { SettingsAppearanceScreen(navController) }
            composable(Routes.SETTINGS_NOTIFICATIONS) { SettingsNotificationsScreen(navController) }
            composable(Routes.SETTINGS_INTEGRATIONS) { SettingsIntegrationsScreen(navController) }
            composable(Routes.SETTINGS_TRACKING) { SettingsTrackingScreen(navController) }
            composable(Routes.SETTINGS_DATA) { SettingsDataScreen(navController) }
            composable(Routes.SETTINGS_PLATFORMS) { SettingsPlatformsScreen(navController) }
            composable(Routes.HISTORY) { HistoryScreen(navController) }
            composable(Routes.STATS) { StatsScreen(navController) }
            composable(Routes.STATS_DETAILS) { StatsDetailsScreen(navController) }
            composable(Routes.STATS_FILTERED_LIST) { StatsFilteredListScreen(navController) }
            composable(Routes.CALENDAR) { CalendarScreen(navController) }
            composable(Routes.ABOUT) { AboutScreen(navController) }
            composable(Routes.ABOUT_LICENSES) { AboutLicenseScreen(navController) }
            composable(Routes.ABOUT_LICENSE_DETAIL) { AboutLibraryLicenseScreen(navController) }
            composable(Routes.ANOTACOES) { AnotacoesScreen(navController) }
        }
    }
}

private fun androidx.navigation.NavController.navigateToHobbyTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
