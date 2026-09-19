package app.oribu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.R

/**
 * Shared building blocks for every Settings screen, in Rokku's mold: since Rokku's actual
 * Settings UI (legacy AndroidX Preference) has no bespoke styling beyond stock Material
 * defaults (no bold row titles, no custom row height/typography), these compose the same look —
 * a large in-body title over a borderless top bar, non-bold rows, and a small tinted/secondary
 * section header — in one place, instead of each screen inventing its own variant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    title: String,
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Vão de 52dp acima do título e 12dp abaixo, como o `big_title`/`big_toolbar` reais
            // do Rokku (`main_activity.xml`) — não um valor arbitrário.
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 12.dp),
            )
            content(PaddingValues(0.dp))
        }
    }
}

/**
 * Section header, matching Rokku's real text styles: the plain (non-[tinted]) variant is
 * `?textAppearanceListItem` (Material3 `titleMedium` — 16sp, Medium weight — used verbatim by
 * `light_theme`/`dark_theme` in `themes_preference.xml`, with no color override, so it just uses
 * the default list-item text color, not an accent), while [tinted] highlights the one main
 * header of a screen (e.g. "App theme"/a `preferenceCategory` title) via AndroidX Preference's
 * `?attr/colorAccent`, which Rokku's own `themes.xml` maps to `colorSecondary` (`colorScheme
 * .secondary` here) — not `colorPrimary`, a common mix-up since both often look similar.
 * [topSpacing] defaults to the gap after unrelated content (a switch row, a divider); pass 4.dp
 * for a header that follows another header/list directly (e.g. "Tema claro" right under "Tema do
 * aplicativo", or "Tema escuro" right under the light-theme row — both use
 * `layout_marginTop="4dp"` in Rokku's real layout).
 */
@Composable
internal fun SettingsSectionHeader(
    title: String,
    tinted: Boolean = false,
    topSpacing: Dp = 20.dp,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = if (tinted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        fontWeight = if (tinted) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, top = topSpacing, bottom = 8.dp),
    )
}

/** Toggle row. Title/subtitle are never bold — matches Rokku's stock Preference row styling. */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = if (subtitle == null) 4.dp else 12.dp),
        verticalAlignment = if (subtitle == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Click-through row (navigates, opens a dialog, triggers an action). Same non-bold typography as
 * [SettingsSwitchRow]; [minHeight] defaults to a plain row, pass 64.dp for a top-level category
 * row with a leading icon (matching the Settings hub).
 */
@Composable
internal fun SettingsClickRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    minHeight: Dp = 56.dp,
    titleColor: Color = Color.Unspecified,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        leadingContent = leadingIcon,
        trailingContent = trailing,
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .heightIn(min = minHeight),
    )
}

/** Single-choice radio-button list dialog, shared by every Settings screen that picks one of N options. */
@Composable
internal fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                            ).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
