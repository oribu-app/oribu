package app.oribu

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.oribu.data.OnboardingPreferences
import app.oribu.ui.locale.AppLocaleController
import app.oribu.ui.navigation.MainNavGraph
import app.oribu.ui.navigation.Routes
import app.oribu.ui.theme.AppThemeController
import app.oribu.ui.theme.OribuTheme
import kotlinx.coroutines.delay

/**
 * Must extend AppCompatActivity, not ComponentActivity: this is the app's only Activity, so
 * without one AppCompatDelegate never gets a Context to call into and
 * AppCompatDelegate.setApplicationLocales() (AppLocaleController.languageMode) silently does
 * nothing — confirmed by testing, not just theory, so don't swap this back to ComponentActivity
 * to "fix" the locale-switch flicker without re-testing that language switching still works at
 * all afterwards.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { OnboardingPreferences.completed.value == null }
        enableEdgeToEdge()
        setContent {
            val lightThemeId = AppThemeController.lightThemeId
            val darkThemeId = AppThemeController.darkThemeId
            val darkMode = AppThemeController.darkMode
            val onboardingCompleted by OnboardingPreferences.completed.collectAsState()

            // MainNavGraph must stay unconditionally composed (never swapped out for the overlay
            // below) so its NavController back stack keeps the same structural position across
            // the Activity recreate and restores normally — branching it out during the language
            // change previously reset navigation back to Routes.HOME instead of staying on
            // whichever screen (e.g. Settings > General) the user was on.
            Box(Modifier.fillMaxSize()) {
                OribuTheme(lightThemeId = lightThemeId, darkThemeId = darkThemeId, darkTheme = darkMode) {
                    onboardingCompleted?.let { completed ->
                        MainNavGraph(startDestination = if (completed) Routes.HOME else Routes.ONBOARDING)
                    }
                }
                if (AppLocaleController.isLanguageChanging) {
                    LanguageChangeOverlay()
                }
            }
        }
    }
}

/**
 * Plain black screen shown across the Activity recreate that applies a language change —
 * deliberately covering the recreate instead of letting the platform's own transition for it
 * (a rough flash, not the smooth dim its per-app-language docs describe) show through.
 */
@Composable
private fun LanguageChangeOverlay() {
    LaunchedEffect(Unit) {
        delay(900)
        AppLocaleController.isLanguageChanging = false
    }
    Box(Modifier.fillMaxSize().background(Color.Black))
}
