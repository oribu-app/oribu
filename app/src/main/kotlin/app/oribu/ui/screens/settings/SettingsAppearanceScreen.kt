package app.oribu.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.oribu.ui.theme.AppThemeController
import app.oribu.ui.theme.AppThemeDefinition
import app.oribu.ui.theme.ThemeMode
import app.oribu.ui.theme.appThemes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aparência") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ThemePickerContent()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Seletor de modo de cor + paletas claro/escuro. Extraído para ser reaproveitado tanto aqui
 * quanto no passo de boas-vindas do onboarding (`WelcomeThemeStep`).
 */
@Composable
fun ThemePickerContent() {
    val themeMode = AppThemeController.themeMode
    val lightThemeId = AppThemeController.lightThemeId
    val darkThemeId = AppThemeController.darkThemeId

    Column {
        // ── Modo de cor ────────────────────────────────────────────────
        PreferenceGroupHeader("Modo de cor")
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeModeEntry.entries.forEach { entry ->
                val selected = themeMode == entry.mode
                OutlinedButton(
                    onClick = { AppThemeController.themeMode = entry.mode },
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                },
                            contentColor =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Icon(entry.icon, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(entry.label, fontSize = 13.sp)
                }
            }
        }

        // ── Tema claro ─────────────────────────────────────────────────
        PreferenceGroupHeader("Tema claro")
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            appThemes.forEach { theme ->
                ThemePreviewSwatch(
                    theme = theme,
                    isDark = false,
                    selected = theme.id == lightThemeId,
                    onClick = { AppThemeController.setLightTheme(theme.id) },
                )
            }
        }

        // ── Tema escuro ────────────────────────────────────────────────
        PreferenceGroupHeader("Tema escuro")
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            appThemes.forEach { theme ->
                ThemePreviewSwatch(
                    theme = theme,
                    isDark = true,
                    selected = theme.id == darkThemeId,
                    onClick = { AppThemeController.setDarkTheme(theme.id) },
                )
            }
        }
    }
}

/** Cabeçalho de seção no molde do Rokku: rótulo pequeno na cor secondary, sem card/divisor. */
@Composable
internal fun PreferenceGroupHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * Miniatura de tema no molde do onboarding do Rokku: um mockup de tela (barra de título, card
 * de superfície, linhas de texto e uma barra de navegação inferior) nas cores reais do tema,
 * com um selo de check quando selecionado — em vez de uma bolinha de cor sólida.
 */
@Composable
private fun ThemePreviewSwatch(
    theme: AppThemeDefinition,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val seed = if (isDark) theme.seedDark else theme.seedLight
    val bg = if (isDark) theme.bgDark else theme.bgLight
    val surface = if (isDark) theme.surfaceDark else theme.surfaceLight
    val onSurface = if (isDark) Color.White else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(112.dp)) {
        Box(
            Modifier
                .size(width = 112.dp, height = 168.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bg)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    },
                ).clickable(onClick = onClick),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(onSurface),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(surface),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .weight(0.65f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(onSurface),
                    )
                    Box(
                        Modifier
                            .weight(0.35f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(seed),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(onSurface),
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i == 1) seed else onSurface.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            theme.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
            modifier = Modifier.width(112.dp),
        )
    }
}

private enum class ThemeModeEntry(
    val mode: ThemeMode,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    DARK(ThemeMode.DARK, "Escuro", Icons.Default.DarkMode),
    LIGHT(ThemeMode.LIGHT, "Claro", Icons.Default.LightMode),
    SYSTEM(ThemeMode.SYSTEM, "Sistema", Icons.Default.SettingsSuggest),
}
