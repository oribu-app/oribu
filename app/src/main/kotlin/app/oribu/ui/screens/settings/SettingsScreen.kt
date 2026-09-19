package app.oribu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.ui.components.EmptyState
import app.oribu.ui.navigation.Routes

// ── Hub de Configurações ────────────────────────────────────────────────────
// Cada categoria abre sua própria sub-tela, no molde do menu principal de
// Configurações do Rokku: uma lista simples de linhas ícone + título + chevron.

private data class SettingsCategory(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val categories =
        listOf(
            SettingsCategory(
                stringResource(R.string.settings_general_title),
                stringResource(R.string.settings_general_subtitle),
                Icons.Default.Language,
                Routes.SETTINGS_GENERAL,
            ),
            SettingsCategory(
                stringResource(R.string.appearance_title),
                stringResource(R.string.settings_appearance_subtitle),
                Icons.Default.Palette,
                Routes.SETTINGS_APPEARANCE,
            ),
            SettingsCategory(
                stringResource(R.string.settings_notifications_title),
                stringResource(R.string.settings_notifications_subtitle),
                Icons.Default.Notifications,
                Routes.SETTINGS_NOTIFICATIONS,
            ),
            SettingsCategory(
                stringResource(R.string.settings_integrations_title),
                stringResource(R.string.settings_integrations_subtitle),
                Icons.Default.Cable,
                Routes.SETTINGS_INTEGRATIONS,
            ),
            SettingsCategory(
                stringResource(R.string.settings_platforms_title),
                stringResource(R.string.settings_platforms_subtitle),
                Icons.Default.SportsEsports,
                Routes.SETTINGS_PLATFORMS,
            ),
            SettingsCategory(
                stringResource(R.string.settings_data_title),
                stringResource(R.string.settings_data_subtitle),
                Icons.Default.Storage,
                Routes.SETTINGS_DATA,
            ),
        )
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query) {
            if (query.isBlank()) {
                categories
            } else {
                categories.filter {
                    it.label.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                }
            }
        }

    SettingsScaffold(stringResource(R.string.settings_title), navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (filtered.isEmpty()) {
                EmptyState(stringResource(R.string.settings_no_results_title), stringResource(R.string.settings_no_results_subtitle))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered) { category ->
                        SettingsCategoryRow(category, onClick = { navController.navigate(category.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    SettingsClickRow(
        title = category.label,
        onClick = onClick,
        minHeight = 64.dp,
        leadingIcon = { Icon(category.icon, null, tint = MaterialTheme.colorScheme.primary) },
    )
}
