package app.oribu.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.R
import app.oribu.ui.theme.AppThemeController
import app.oribu.ui.theme.AppThemeDefinition
import app.oribu.ui.theme.CoverThemeController
import app.oribu.ui.theme.ThemeAvailability
import app.oribu.ui.theme.ThemeMode
import app.oribu.ui.theme.appThemes
import com.materialkolor.PaletteStyle

@Composable
fun SettingsAppearanceScreen(navController: NavController) {
    SettingsScaffold(stringResource(R.string.appearance_title), navController) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            ThemePickerContent()
            ThemeExtraOptions()
            DetailsPageSection()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Seletor de paletas claro/escuro. Extraído para ser reaproveitado tanto aqui quanto no passo de
 * boas-vindas do onboarding (`WelcomeThemeStep`) — que só quer a escolha de cores, sem as opções
 * extras abaixo (essas ficam só em [ThemeExtraOptions], exclusivas da tela de Configurações).
 */
@Composable
fun ThemePickerContent() {
    val lightThemeId = AppThemeController.lightThemeId
    val darkThemeId = AppThemeController.darkThemeId
    val mode = AppThemeController.themeMode
    // No Rokku (`ThemeItem.isSelected`), o check só aparece na linha do modo ativo agora —
    // quando o modo é fixo em Claro/Escuro, a outra linha guarda a escolha mas não mostra check
    // nenhum; só com "Seguir sistema" (FOLLOW_SYSTEM) as duas linhas podem mostrar check ao
    // mesmo tempo, já que qualquer uma pode estar valendo dependendo do sistema.
    val lightRowCanShowCheck = mode == ThemeMode.LIGHT || mode == ThemeMode.SYSTEM
    val darkRowCanShowCheck = mode == ThemeMode.DARK || mode == ThemeMode.SYSTEM

    Column {
        SettingsSectionHeader(stringResource(R.string.appearance_app_theme), tinted = true)

        // ── Tema claro ─────────────────────────────────────────────────
        SettingsSectionHeader(stringResource(R.string.appearance_light_theme), topSpacing = 4.dp)
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            appThemes.filter { it.availability != ThemeAvailability.DARK_ONLY }.forEach { theme ->
                ThemePreviewSwatch(
                    theme = theme,
                    isDark = false,
                    selected = lightRowCanShowCheck && theme.id == lightThemeId,
                    onClick = {
                        AppThemeController.setLightTheme(theme.id)
                        AppThemeController.themeMode = ThemeMode.LIGHT
                    },
                )
            }
        }

        // ── Tema escuro ────────────────────────────────────────────────
        SettingsSectionHeader(stringResource(R.string.appearance_dark_theme), topSpacing = 4.dp)
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            appThemes.filter { it.availability != ThemeAvailability.LIGHT_ONLY }.forEach { theme ->
                ThemePreviewSwatch(
                    theme = theme,
                    isDark = true,
                    selected = darkRowCanShowCheck && theme.id == darkThemeId,
                    onClick = {
                        AppThemeController.setDarkTheme(theme.id)
                        AppThemeController.themeMode = ThemeMode.DARK
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeExtraOptions() {
    val systemDark = isSystemInDarkTheme()
    Column {
        SettingsSwitchRow(
            title = stringResource(R.string.appearance_follow_system),
            checked = AppThemeController.themeMode == ThemeMode.SYSTEM,
            onCheckedChange = { follow ->
                AppThemeController.themeMode =
                    if (follow) {
                        ThemeMode.SYSTEM
                    } else {
                        if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
                    }
            },
        )
        SettingsSwitchRow(
            title = stringResource(R.string.appearance_pure_black),
            checked = AppThemeController.pureBlackDark,
            onCheckedChange = { AppThemeController.pureBlackDark = it },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionHeader(stringResource(R.string.appearance_accent_color))
        SettingsSwitchRow(
            title = stringResource(R.string.appearance_use_theme_accent),
            subtitle = stringResource(R.string.appearance_use_theme_accent_subtitle),
            checked = AppThemeController.useThemeAccentColor,
            onCheckedChange = { AppThemeController.useThemeAccentColor = it },
        )
    }
}

/**
 * Seção "Página de detalhes" (nome exato em inglês no Rokku: "Details page",
 * `SettingsAppearanceController.kt`): liga cores extraídas da capa nas telas de detalhe em vez do
 * tema do app, com os mesmos 9 estilos do `materialkolor` (+ "Legado", sem gerar esquema
 * completo, só troca a cor primária) que o Rokku usa em "Cover theme style" — lá é só na tela de
 * mangá; aqui vale para jogos, mangás, séries, filmes e livros.
 */
@Composable
private fun DetailsPageSection() {
    var styleDialogOpen by remember { mutableStateOf(false) }

    SettingsSectionHeader(stringResource(R.string.appearance_details_page))
    SettingsSwitchRow(
        title = stringResource(R.string.appearance_theme_based_on_cover),
        subtitle = stringResource(R.string.appearance_theme_based_on_cover_subtitle),
        checked = CoverThemeController.enabled,
        onCheckedChange = { CoverThemeController.enabled = it },
    )
    if (CoverThemeController.enabled) {
        SettingsClickRow(
            title = stringResource(R.string.appearance_cover_theme_style),
            subtitle = coverThemeStyleLabel(CoverThemeController.style),
            onClick = { styleDialogOpen = true },
        )
    }

    if (styleDialogOpen) {
        val options = listOf<PaletteStyle?>(null) + PaletteStyle.entries
        SingleChoiceDialog(
            title = stringResource(R.string.appearance_cover_theme_style),
            options = options.map { it to coverThemeStyleLabel(it) },
            selected = CoverThemeController.style,
            onSelect = { CoverThemeController.styleName = it?.name },
            onDismiss = { styleDialogOpen = false },
        )
    }
}

@Composable
private fun coverThemeStyleLabel(style: PaletteStyle?): String =
    stringResource(
        when (style) {
            null -> R.string.cover_theme_style_legacy
            PaletteStyle.TonalSpot -> R.string.cover_theme_style_tonal_spot
            PaletteStyle.Neutral -> R.string.cover_theme_style_neutral
            PaletteStyle.Vibrant -> R.string.cover_theme_style_vibrant
            PaletteStyle.Expressive -> R.string.cover_theme_style_expressive
            PaletteStyle.Rainbow -> R.string.cover_theme_style_rainbow
            PaletteStyle.FruitSalad -> R.string.cover_theme_style_fruit_salad
            PaletteStyle.Monochrome -> R.string.cover_theme_style_monochrome
            PaletteStyle.Fidelity -> R.string.cover_theme_style_fidelity
            PaletteStyle.Content -> R.string.cover_theme_style_content
        },
    )

/**
 * Miniatura de tema fiel ao `theme_item.xml`/`dimens.xml` reais do Rokku (não um mockup livre):
 * card 86×128dp (16dp de canto) dentro de um item de 110dp, barra de topo de 20dp na cor
 * `colorSurface` (não a cor de destaque — só o botão/pill dentro dela usa
 * `actionBarTintColor`/onSurface), um "hero" translúcido de 20dp, duas linhas de texto (a
 * primeira com um pill de destaque do lado, na cor `accent` = `colorSecondary` real do tema), e
 * uma barra de navegação inferior de cor neutra fixa (superfície) — não muda com o tema, só o
 * ícone selecionado usa a cor de destaque, igual ao app de verdade (confirmado por observação
 * direta: só o "highlight" da tela muda por tema, a barra em si não). O selo de check quando
 * selecionado fica recuado 4dp da borda superior/direita do card (réplica do `checkbox` real,
 * que é filho do ConstraintLayout que preenche o card, não do item de 110dp).
 */
@Composable
private fun ThemePreviewSwatch(
    theme: AppThemeDefinition,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // `colorSecondary` real do Rokku — colore a borda de seleção, o pill de destaque e o ponto
    // ativo no mockup (`theme_selected_border.xml` e `theme_accented_button` em `theme_item.xml`
    // usam `?attr/colorSecondary`, não `colorPrimary`).
    val accent = if (isDark) theme.accentDark else theme.accentLight
    val bg = if (isDark) theme.bgDark else theme.bgLight
    val surface = if (isDark) theme.surfaceDark else theme.surfaceLight
    val onSurface = if (isDark) Color.White else Color.Black
    val cardWidth = 86.dp
    val cardHeight = 128.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
        Box(Modifier.padding(top = 6.dp).size(width = cardWidth, height = cardHeight)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .then(
                        if (selected) {
                            Modifier.border(4.dp, accent, RoundedCornerShape(16.dp))
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        },
                    ).clickable(onClick = onClick),
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Barra de topo: cor de superfície, não a cor de destaque do tema. Recuada das
                    // bordas do card (pedido explícito) em vez de rente aos cantos como no Rokku.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp, start = 6.dp, end = 6.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(surface),
                    ) {
                        Box(
                            Modifier
                                .padding(start = 8.dp)
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.45f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(onSurface),
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 8.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(onSurface.copy(alpha = 0.15f)),
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(onSurface),
                            )
                            Spacer(Modifier.width(3.dp))
                            Box(
                                Modifier
                                    .width(10.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accent),
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Row {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(onSurface),
                            )
                            Spacer(Modifier.width(3.dp))
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(onSurface),
                            )
                        }
                    }
                    // Barra inferior: cor neutra fixa (não muda com o tema) — só o ícone
                    // selecionado usa a cor de destaque, igual ao app de verdade.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(surface),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(3) { i ->
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (i == 1) accent else onSurface.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
            }
            if (selected) {
                // Posição recuada 4dp da borda superior/direita replica `checkbox` do Rokku
                // (`theme_item.xml`, linhas 185-197 — filho do ConstraintLayout que preenche o
                // card, não do item de 110dp). Cores são um pedido explícito do usuário, não uma
                // réplica literal do Rokku: círculo na cor de destaque do tema, símbolo preto.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    // Desenhado à mão (não `Icons.Default.Check`) porque o vetor padrão tem uma
                    // espessura de traço fixa embutida — só aumentar o tamanho do ícone deixa o
                    // check maior, não mais grosso. Aqui a espessura é um parâmetro à parte.
                    ThickCheckmark(color = Color.Black, size = 12.dp, strokeWidth = 2.2.dp)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(if (isDark) theme.darkLabelRes else theme.lightLabelRes),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 3,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(98.dp),
        )
    }
}

/** Checkmark com espessura de traço controlável (não a do vetor padrão do Material). */
@Composable
private fun ThickCheckmark(
    color: Color,
    size: Dp,
    strokeWidth: Dp,
) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path =
            Path().apply {
                moveTo(w * 0.20f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.74f)
                lineTo(w * 0.80f, h * 0.28f)
            }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
