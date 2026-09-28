package app.oribu.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.data.SeriesScope
import app.oribu.data.SeriesScopePreferences
import app.oribu.ui.locale.AppLocaleController
import app.oribu.ui.locale.DateFormatMode
import app.oribu.ui.locale.LanguageMode
import app.oribu.ui.locale.exampleLabel
import app.oribu.ui.locale.patternLabel

@Composable
fun SettingsGeneralScreen(navController: NavController) {
    SettingsScaffold(stringResource(R.string.general_title), navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LocaleSection()
            SeriesScopeSection()
        }
    }
}

@Composable
private fun LocaleSection() {
    var languageDialogOpen by remember { mutableStateOf(false) }
    var dateFormatDialogOpen by remember { mutableStateOf(false) }

    SettingsSectionHeader(stringResource(R.string.general_locale_section), tinted = true)

    SettingsClickRow(
        title = stringResource(R.string.general_language),
        subtitle = AppLocaleController.languageMode.label,
        onClick = { languageDialogOpen = true },
    )
    SettingsClickRow(
        title = stringResource(R.string.general_date_format),
        subtitle = AppLocaleController.dateFormatMode.exampleLabel(),
        onClick = { dateFormatDialogOpen = true },
    )

    if (languageDialogOpen) {
        SingleChoiceDialog(
            title = stringResource(R.string.general_language),
            options = LanguageMode.entries.map { it to it.label },
            selected = AppLocaleController.languageMode,
            onSelect = { AppLocaleController.languageMode = it },
            onDismiss = { languageDialogOpen = false },
        )
    }
    if (dateFormatDialogOpen) {
        SingleChoiceDialog(
            title = stringResource(R.string.general_date_format),
            options = DateFormatMode.entries.map { it to it.patternLabel() },
            selected = AppLocaleController.dateFormatMode,
            onSelect = { AppLocaleController.dateFormatMode = it },
            onDismiss = { dateFormatDialogOpen = false },
        )
    }
}

/** What the Series tab covers — also asked on the tab's first access. */
@Composable
private fun SeriesScopeSection() {
    val current by SeriesScopePreferences.seriesScope.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }

    SettingsSectionHeader(stringResource(R.string.general_series_scope_section), tinted = true)
    SettingsClickRow(
        title = stringResource(R.string.general_series_scope),
        subtitle = current?.let { stringResource(it.labelRes) } ?: stringResource(R.string.general_series_scope_unset),
        onClick = { dialogOpen = true },
    )

    if (dialogOpen) {
        SingleChoiceDialog(
            title = stringResource(R.string.general_series_scope),
            options = SeriesScope.entries.map { it to stringResource(it.labelRes) },
            selected = current ?: SeriesScope.SERIES,
            onSelect = { SeriesScopePreferences.set(it) },
            onDismiss = { dialogOpen = false },
        )
    }
}
