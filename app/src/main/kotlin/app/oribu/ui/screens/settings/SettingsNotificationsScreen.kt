package app.oribu.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.oribu.R

@Composable
fun SettingsNotificationsScreen(navController: NavController) {
    var notifEnabled by remember { mutableStateOf(true) }

    SettingsScaffold(stringResource(R.string.settings_notifications_title), navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_notif_daily_update),
                subtitle = stringResource(R.string.settings_notif_daily_update_subtitle),
                checked = notifEnabled,
                onCheckedChange = { notifEnabled = it },
            )
        }
    }
}
