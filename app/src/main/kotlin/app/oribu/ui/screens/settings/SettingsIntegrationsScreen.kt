package app.oribu.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.ui.components.ServiceCredentialsList

@Composable
fun SettingsIntegrationsScreen(navController: NavController) {
    SettingsScaffold(stringResource(R.string.settings_integrations_title), navController) { padding ->
        ServiceCredentialsList(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        )
    }
}
