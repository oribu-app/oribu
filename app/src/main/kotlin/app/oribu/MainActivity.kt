package app.oribu

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import app.oribu.ui.navigation.MainNavGraph
import app.oribu.ui.theme.AppThemeController
import app.oribu.ui.theme.OribuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lightThemeId = AppThemeController.lightThemeId
            val darkThemeId = AppThemeController.darkThemeId
            val darkMode = AppThemeController.darkMode

            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) {}
            LaunchedEffect(Unit) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            OribuTheme(lightThemeId = lightThemeId, darkThemeId = darkThemeId, darkTheme = darkMode) {
                MainNavGraph()
            }
        }
    }
}
