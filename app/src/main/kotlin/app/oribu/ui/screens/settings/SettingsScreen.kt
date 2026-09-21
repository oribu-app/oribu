package app.oribu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.ui.components.EmptyState
import app.oribu.ui.navigation.Routes

// ── Hub de Configurações ────────────────────────────────────────────────────
// Cada categoria abre sua própria sub-tela, no molde do menu principal de
// Configurações do Rokku: uma lista simples de linhas ícone + título + chevron.

// Mesmo link usado pelo botão de ajuda (`action_help`) do SettingsMainController do Rokku.
private const val URL_TROUBLESHOOTING = "https://oribu-app.github.io/troubleshooting/"

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
                Icons.Default.Tune,
                Routes.SETTINGS_GENERAL,
            ),
            SettingsCategory(
                stringResource(R.string.appearance_title),
                stringResource(R.string.settings_appearance_subtitle),
                Icons.Outlined.Palette,
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

    val uriHandler = LocalUriHandler.current
    val helpContentDescription = stringResource(R.string.settings_help_content_description)

    SettingsScaffold(
        stringResource(R.string.settings_title),
        navController,
        actions = {
            IconButton(onClick = { uriHandler.openUri(URL_TROUBLESHOOTING) }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = helpContentDescription)
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Hand-rolled instead of M3's `TextField`: that component defaults to a 56dp-tall box
            // via padding baked into its own decoration, and forcing a smaller `Modifier.height`
            // on top of it doesn't make that internal padding shrink to fit — it just clips the
            // icon/text against the smaller box. Building the row directly (like
            // `SettingsCategoryRow` already does for the same reason) gives real control instead.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_search_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                }
            }
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

// Linha manual em vez de [SettingsClickRow] (que usa o `ListItem` do Material3, cujo piso de
// altura interno pra um item de uma linha só ignora qualquer `heightIn(min = X)` menor). Valores
// abaixo portados diretamente de `preference_material.xml` + `image_frame.xml` reais do
// androidx.preference:preference:1.2.1 (dependência transitiva do `conductor-support-preference`
// que o Rokku usa) — não estimados: `paddingStart`/`paddingEnd` da linha =
// `?attr/listPreferredItemPaddingStart/End` (16dp no tema Material padrão), moldura do ícone com
// `minWidth = 56dp`, texto com `paddingTop`/`paddingBottom` de 16dp cada. A altura final nasce
// desse padding em volta do texto, não de um valor fixo — assim ela acompanha o tamanho de fonte
// do sistema como a linha real do Preference também acompanha.
@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(56.dp), contentAlignment = Alignment.CenterStart) {
            Icon(
                category.icon,
                null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            category.label,
            // Compose's text layout drops the legacy Android "font padding" (the small top/bottom
            // leading `TextView` adds by default via `Paint.getFontMetricsInt()`) unless asked for
            // explicitly. The real Preference row's title IS a `TextView` (no opt-out), so without
            // `includeFontPadding = true` here Compose's text draws measurably shorter than
            // Rokku's real row for the exact same 16dp/56dp/16dp spacing — this isn't a spacing
            // difference at all, it's the two text-rendering engines measuring the same font
            // differently. `lineHeight = Unspecified` lets that font-padding-inclusive metric
            // drive the line height instead of `bodyLarge`'s fixed 24sp.
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = TextUnit.Unspecified,
                    platformStyle = PlatformTextStyle(includeFontPadding = true),
                ),
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}
