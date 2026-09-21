package app.oribu.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.oribu.R

// ── Cores de mídia ────────────────────────────────────────────────────────────
// Quando `AppThemeController.useThemeAccentColor` está ligado, todas seguem a cor primária do
// tema selecionado em vez da cor fixa de cada tipo — mesma opção do Rokku.
val ColorJogo: Color @Composable get() = themeAwareAccent(Color(0xFF7B1FA2))
val ColorManga: Color @Composable get() = themeAwareAccent(Color(0xFFE91E63))
val ColorWebtoon: Color @Composable get() = themeAwareAccent(Color(0xFF00BCD4))
val ColorSerie: Color @Composable get() = themeAwareAccent(Color(0xFF1976D2))
val ColorFilme: Color @Composable get() = themeAwareAccent(Color(0xFFFF6F00))
val ColorLivro: Color @Composable get() = themeAwareAccent(Color(0xFF388E3C))

@Composable
internal fun themeAwareAccent(fixed: Color): Color =
    if (AppThemeController.useThemeAccentColor) MaterialTheme.colorScheme.primary else fixed

// ── Plataformas ───────────────────────────────────────────────────────────────
val ColorSteam = Color(0xFF1B2838)
val ColorPlayStation = Color(0xFF00439C)
val ColorNintendo = Color(0xFFE4000F)
val ColorXbox = Color(0xFF107C10)

// ── Temas disponíveis ─────────────────────────────────────────────────────────
// Nomes e cores replicados diretamente do catálogo real do Rokku (`Themes.kt`/`themes.xml`,
// atributos `colorPrimary`/`colorSurface`/`background` por tema) — não paletas inventadas. Nome
// e cor de destaque mudam entre claro/escuro, exatamente como no Rokku (ex: "Tako" claro é roxo
// escuro sobre fundo lilás, "Tako" escuro é pêssego sobre fundo azulado). `colorSurface` do Rokku
// coincide com `background` em praticamente todos os temas, por isso `surface*` = `bg*` aqui.
// Excluído do catálogo do Rokku: Monet (cor dinâmica do Android 12+/Material You — depende do
// papel de parede do sistema, não é uma paleta fixa portável para uma miniatura estática; fora de
// escopo). Yotsuba e Doki são, no Rokku real, temas de um só modo (Yotsuba só aparece na lista de
// temas claros, Doki só na de escuros — filtro em `Themes.kt`/`ThemePreference.kt`); replicado
// aqui via [ThemeAvailability] em vez de forçar um par claro/escuro inventado para eles.
enum class ThemeAvailability { BOTH, LIGHT_ONLY, DARK_ONLY }

/**
 * [lightLabelRes]/[darkLabelRes] apontam pro nome real que o Rokku usa (e traduz) pra cada
 * paleta — diferente de [MediaStatus]/[app.oribu.model.GameConsole], que são chaves de lógica de
 * negócio e por isso ficam fixas em inglês, nome de tema é só cosmético e o próprio Rokku traduz
 * (`values-pt-rBR.xml`), então aqui também segue o idioma escolhido (ex: `flat_lime` vira "Neko"
 * em pt-BR, não uma tradução literal de "Flat Lime").
 */
data class AppThemeDefinition(
    val id: String,
    @StringRes val lightLabelRes: Int,
    @StringRes val darkLabelRes: Int,
    val seedDark: Color,
    val seedLight: Color,
    /**
     * `colorSecondary` real do Rokku (`?attr/colorAccent` → `colorSecondary`, ver comentário em
     * `Theme.Base` no `themes.xml`) — NÃO é uma variação de [seedDark]/[seedLight]
     * (`colorPrimary`). É essa cor, não a primary, que colore a borda de seleção
     * (`theme_selected_border.xml`: `stroke color="?attr/colorSecondary"`), o cabeçalho de
     * categoria tintado e o "pill" de destaque dentro do mockup (`theme_accented_button`).
     */
    val accentDark: Color,
    val accentLight: Color,
    val bgDark: Color,
    val surfaceDark: Color,
    val bgLight: Color,
    val surfaceLight: Color,
    /**
     * Real Rokku `colorOnPrimary` per theme (`onPrimary<Name>`/`<name>_on_primary` in
     * `colors.xml`) — a per-theme *dark* shade in every dark palette so far (not a flat black),
     * used e.g. by the stock Material3 `MaterialSwitch`'s checked-thumb color (it contrasts
     * against the primary-colored track). A flat `Color.White` here looked plausible but is
     * wrong: [seedDark] is a light/pastel color in every current theme, so a white "on" color
     * has poor contrast against it wherever a component actually uses this role.
     */
    val onPrimaryDark: Color,
    val onPrimaryLight: Color,
    val availability: ThemeAvailability = ThemeAvailability.BOTH,
)

val appThemes =
    listOf(
        AppThemeDefinition(
            id = "default",
            lightLabelRes = R.string.theme_white_theme,
            darkLabelRes = R.string.theme_dark,
            seedDark = Color(0xFF78BCFF),
            seedLight = Color(0xFF54759E),
            accentDark = Color(0xFF3399FF),
            accentLight = Color(0xFF2979FF),
            bgDark = Color(0xFF1C1C1D),
            surfaceDark = Color(0xFF1C1C1D),
            bgLight = Color(0xFFFAFAFA),
            surfaceLight = Color(0xFFFAFAFA),
            onPrimaryDark = Color(0xFF071D39),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        AppThemeDefinition(
            id = "spring_dusk",
            lightLabelRes = R.string.theme_spring_blossom,
            darkLabelRes = R.string.theme_midnight_dusk,
            seedDark = Color(0xFFE570A0),
            seedLight = Color(0xFFA149BF),
            accentDark = Color(0xFFF02475),
            accentLight = Color(0xFFC43C97),
            bgDark = Color(0xFF16151D),
            surfaceDark = Color(0xFF16151D),
            bgLight = Color(0xFFF7F4F8),
            surfaceLight = Color(0xFFF7F4F8),
            onPrimaryDark = Color(0xFF370318),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        AppThemeDefinition(
            id = "strawberries",
            lightLabelRes = R.string.theme_strawberry_daiquiri,
            darkLabelRes = R.string.theme_chocolate_strawberries,
            seedDark = Color(0xFFE14E4E),
            seedLight = Color(0xFFD31D3B),
            accentDark = Color(0xFFCC4444),
            accentLight = Color(0xFFED4A65),
            bgDark = Color(0xFF1A1716),
            surfaceDark = Color(0xFF1A1716),
            bgLight = Color(0xFFFAFAFA),
            surfaceLight = Color(0xFFFAFAFA),
            onPrimaryDark = Color(0xFF330303),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        AppThemeDefinition(
            id = "teal_sapphire",
            lightLabelRes = R.string.theme_teal_ocean,
            darkLabelRes = R.string.theme_sapphire_dusk,
            seedDark = Color(0xFF80B8D1),
            seedLight = Color(0xFF5F9C96),
            accentDark = Color(0xFF589AB8),
            accentLight = Color(0xFF05B1A4),
            bgDark = Color(0xFF14191B),
            surfaceDark = Color(0xFF14191B),
            bgLight = Color(0xFFF0F7F7),
            surfaceLight = Color(0xFFF0F7F7),
            onPrimaryDark = Color(0xFF02212C),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        AppThemeDefinition(
            id = "lavender",
            lightLabelRes = R.string.theme_lavender,
            darkLabelRes = R.string.theme_violet,
            seedDark = Color(0xFFA177FF),
            seedLight = Color(0xFF9C64D3),
            accentDark = Color(0xFFA177FF),
            accentLight = Color(0xFF7B46AF),
            bgDark = Color(0xFF111129),
            surfaceDark = Color(0xFF111129),
            bgLight = Color(0xFFEDE2FF),
            surfaceLight = Color(0xFFEDE2FF),
            onPrimaryDark = Color(0xFF111129),
            onPrimaryLight = Color(0xFFEDE2FF),
        ),
        AppThemeDefinition(
            id = "tako",
            lightLabelRes = R.string.theme_tako,
            darkLabelRes = R.string.theme_tako,
            seedDark = Color(0xFFFCCD9F),
            seedLight = Color(0xFF463561),
            accentDark = Color(0xFFF3B375),
            accentLight = Color(0xFF66577E),
            bgDark = Color(0xFF21212E),
            surfaceDark = Color(0xFF21212E),
            bgLight = Color(0xFFF2EDF7),
            surfaceLight = Color(0xFFF2EDF7),
            onPrimaryDark = Color(0xFF3C2004),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        AppThemeDefinition(
            id = "yin_yang",
            lightLabelRes = R.string.theme_yang,
            darkLabelRes = R.string.theme_yin,
            seedDark = Color(0xFFDCDCDC),
            seedLight = Color(0xFF333333),
            accentDark = Color(0xFFFFFFFF),
            accentLight = Color(0xFF000000),
            bgDark = Color(0xFF1C1C1D),
            surfaceDark = Color(0xFF1C1C1D),
            bgLight = Color(0xFFFAFAFA),
            surfaceLight = Color(0xFFFAFAFA),
            onPrimaryDark = Color(0xFF191919),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        AppThemeDefinition(
            id = "lime",
            lightLabelRes = R.string.theme_lime_time,
            darkLabelRes = R.string.theme_flat_lime,
            seedDark = Color(0xFF7CF7A5),
            seedLight = Color(0xFF57BD79),
            accentDark = Color(0xFF4AF88A),
            accentLight = Color(0xFF1DA750),
            bgDark = Color(0xFF202125),
            surfaceDark = Color(0xFF202125),
            bgLight = Color(0xFFE9EFEB),
            surfaceLight = Color(0xFFE9EFEB),
            onPrimaryDark = Color(0xFF043314),
            onPrimaryLight = Color(0xFFFFFFFF),
        ),
        // Só claro no Rokku — mesma cor usada nos dois campos porque a variante escura nunca é lida.
        AppThemeDefinition(
            id = "yotsuba",
            lightLabelRes = R.string.theme_yotsuba,
            darkLabelRes = R.string.theme_yotsuba,
            seedDark = Color(0xFFBA5427),
            seedLight = Color(0xFFBA5427),
            accentDark = Color(0xFFDC6D3D),
            accentLight = Color(0xFFDC6D3D),
            bgDark = Color(0xFFFAFAFA),
            surfaceDark = Color(0xFFFAFAFA),
            bgLight = Color(0xFFFAFAFA),
            surfaceLight = Color(0xFFFAFAFA),
            onPrimaryDark = Color(0xFFFFFFFF),
            onPrimaryLight = Color(0xFFFFFFFF),
            availability = ThemeAvailability.LIGHT_ONLY,
        ),
        // Só escuro no Rokku — mesma cor usada nos dois campos porque a variante clara nunca é lida.
        AppThemeDefinition(
            id = "doki",
            lightLabelRes = R.string.theme_doki,
            darkLabelRes = R.string.theme_doki,
            seedDark = Color(0xFFFDE289),
            seedLight = Color(0xFFFDE289),
            accentDark = Color(0xFFEABD62),
            accentLight = Color(0xFFEABD62),
            bgDark = Color(0xFF040716),
            surfaceDark = Color(0xFF040716),
            bgLight = Color(0xFF040716),
            surfaceLight = Color(0xFF040716),
            onPrimaryDark = Color(0xFF070F2C),
            onPrimaryLight = Color(0xFF070F2C),
            availability = ThemeAvailability.DARK_ONLY,
        ),
    )

fun appThemeById(id: String) = appThemes.firstOrNull { it.id == id } ?: appThemes.first()
