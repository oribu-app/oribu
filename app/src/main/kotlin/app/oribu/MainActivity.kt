package app.oribu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.oribu.data.OnboardingPreferences
import app.oribu.ui.navigation.MainNavGraph
import app.oribu.ui.navigation.Routes
import app.oribu.ui.theme.AppThemeController
import app.oribu.ui.theme.OribuTheme

class MainActivity : ComponentActivity() {
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

            OribuTheme(lightThemeId = lightThemeId, darkThemeId = darkThemeId, darkTheme = darkMode) {
                onboardingCompleted?.let { completed ->
                    MainNavGraph(startDestination = if (completed) Routes.HOME else Routes.ONBOARDING)
                }
            }
        }
    }
}
