package app.oribu.ui.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.oribu.data.SavedTheme
import app.oribu.data.ThemePreferences
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.ln

// ── Controller global de tema ────────────────────────────────────────────────

enum class ThemeMode { DARK, LIGHT, SYSTEM }

object AppThemeController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Tema claro e escuro são escolhidos de forma independente (cada um pode ser
    // qualquer uma das paletas disponíveis) — o modo de cor decide qual dos dois
    // está ativo no momento, não qual paleta usar.
    private val _lightThemeId = mutableStateOf("lime")
    private val _darkThemeId = mutableStateOf("lime")
    private val _themeMode = mutableStateOf(ThemeMode.DARK)
    private val _useThemeAccentColor = mutableStateOf(false)
    private val _pureBlackDark = mutableStateOf(false)

    var lightThemeId: String
        get() = _lightThemeId.value
        set(value) {
            _lightThemeId.value = value
            persist()
        }
    var darkThemeId: String
        get() = _darkThemeId.value
        set(value) {
            _darkThemeId.value = value
            persist()
        }
    var themeMode: ThemeMode
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
            persist()
        }

    /**
     * Quando ligado, as cores de destaque por hobby (jogos, mangás, filmes...) deixam de usar a
     * cor fixa de cada tipo e passam a usar a cor primária do tema selecionado — igual à opção
     * equivalente do Rokku.
     */
    var useThemeAccentColor: Boolean
        get() = _useThemeAccentColor.value
        set(value) {
            _useThemeAccentColor.value = value
            persist()
        }

    /** Fundo/superfície totalmente pretos no tema escuro (economia de bateria em telas OLED). */
    var pureBlackDark: Boolean
        get() = _pureBlackDark.value
        set(value) {
            _pureBlackDark.value = value
            persist()
        }

    /**
     * Carrega o tema salvo (chamado uma vez em `OribuApp.onCreate`). Lê só o primeiro valor
     * emitido pelo DataStore (`.first()`), em vez de ficar coletando para sempre — depois da
     * carga inicial, este objeto em memória é a única fonte de verdade; `persist()` só escreve,
     * nunca lê de volta. Um `.collect` contínuo aqui reabriria uma corrida real: qualquer escrita
     * (inclusive as nossas) reemite pelo Flow do DataStore, e se essa reemissão chegasse depois
     * de o usuário already ter trocado o modo na tela, o valor escolhido seria sobrescrito de
     * volta pelo estado antigo em disco.
     */
    fun init(context: Context) {
        ThemePreferences.init(context)
        scope.launch {
            val saved = ThemePreferences.saved.first()
            _lightThemeId.value = saved.lightThemeId
            _darkThemeId.value = saved.darkThemeId
            _themeMode.value = saved.themeMode
            _useThemeAccentColor.value = saved.useThemeAccentColor
            _pureBlackDark.value = saved.pureBlackDark
        }
    }

    private fun persist() {
        ThemePreferences.persist(
            SavedTheme(
                lightThemeId,
                darkThemeId,
                themeMode,
                useThemeAccentColor,
                pureBlackDark,
            ),
        )
    }

    val darkMode: Boolean
        @Composable get() =
            when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

    fun setLightTheme(id: String) {
        lightThemeId = id
    }

    fun setDarkTheme(id: String) {
        darkThemeId = id
    }
}

// ── Composable principal ──────────────────────────────────────────────────────

@Composable
fun OribuTheme(
    lightThemeId: String = AppThemeController.lightThemeId,
    darkThemeId: String = AppThemeController.darkThemeId,
    darkTheme: Boolean = AppThemeController.darkMode,
    content: @Composable () -> Unit,
) {
    val def = appThemeById(if (darkTheme) darkThemeId else lightThemeId)
    val pureBlack = AppThemeController.pureBlackDark

    // `darkColorScheme()`/`lightColorScheme()` only fill the roles passed explicitly and leave
    // every other one (`surfaceContainerHigh` and friends — what `AlertDialog`/`Card`/chips
    // actually paint their background with) at Compose's flat neutral-gray default, instead of
    // toned from the theme's seed color like Rokku's dialogs. Building the full scheme via
    // `dynamicColorScheme` (same generator `CoverThemedSurface` already uses per-cover) fixes
    // that everywhere, then `.copy()` re-applies the same explicit overrides as before —
    // `surfaceTint`, so Card/Surface elevation doesn't get Material's default purple veil over
    // the theme color, and `secondary` = `def.accentDark/Light`, the real Rokku `colorSecondary`
    // (not a variation of `colorPrimary`) that colors the selection border and tinted category
    // header (see `AppThemeDefinition.accentDark`) — using `colorScheme.primary` there (like
    // before) made everything lighter/"more sober" than the real Rokku.
    val seed = if (darkTheme) def.seedDark else def.seedLight
    val background = if (darkTheme) (if (pureBlack) Color.Black else def.bgDark) else def.bgLight
    val surface = if (darkTheme) (if (pureBlack) Color.Black else def.surfaceDark) else def.surfaceLight

    // Rokku's own dialog/card tint (confirmed from its real `themes.xml` + how AndroidX
    // Compose's `Surface` resolves it: `ColorSchemeKt.applyTonalElevation`, decompiled from the
    // actual dependency jar) comes from Material3's tonal-elevation overlay — blending
    // `surfaceTint` (the theme's seed here) onto `surface` at an alpha that grows
    // logarithmically with a component's elevation: `(4.5 * ln(elevationDp + 1) + 2) / 100`.
    // That overlay is normally automatic, but only kicks in when a role's color is *exactly*
    // `colorScheme.surface`, which didn't reliably reach `AlertDialog` here (see
    // `SingleChoiceDialog`) — so it's precomputed directly instead, at the real M3 elevation
    // level each container role nominally represents (1/3/6/8/12dp). Two earlier attempts
    // guessed instead of replicating this exactly: blending the raw seed via linear RGB `lerp()`
    // was barely visible, and building each tone in HCT at a boosted chroma read as an
    // over-saturated pure green — both before this was decompiled.
    fun tonalSurface(elevationDp: Double): Color {
        val alpha = ((4.5 * ln(elevationDp + 1) + 2) / 100).toFloat().coerceIn(0f, 1f)
        return seed.copy(alpha = alpha).compositeOver(surface)
    }

    val colorScheme =
        remember(seed, darkTheme, pureBlack) {
            dynamicColorScheme(
                seedColor = seed,
                isDark = darkTheme,
                isAmoled = pureBlack,
                style = PaletteStyle.TonalSpot,
                specVersion = specFor(seed),
            )
        }.copy(
            // `dynamicColorScheme` maps the seed to a canonical HCT tone for the primary role
            // instead of using its exact RGB, which drifted primary away from the real
            // Rokku-matched color the seeds were picked for — pin it back explicitly.
            primary = seed,
            secondary = if (darkTheme) def.accentDark else def.accentLight,
            surfaceTint = seed,
            background = background,
            surface = surface,
            onPrimary = if (darkTheme) def.onPrimaryDark else def.onPrimaryLight,
            onBackground = if (darkTheme) Color.White else Color.Black,
            onSurface = if (darkTheme) Color.White else Color.Black,
            surfaceContainerLowest = surface,
            surfaceContainerLow = tonalSurface(1.0),
            surfaceContainer = tonalSurface(3.0),
            surfaceContainerHigh = tonalSurface(6.0),
            surfaceContainerHighest = tonalSurface(8.0),
            surfaceVariant = tonalSurface(6.0),
            surfaceBright = tonalSurface(12.0),
            surfaceDim = surface,
        )

    // Escala de cantos M3: capas de mídia seguem retas (extraSmall/small), containers
    // de card e chips ganham arredondamento (medium/large) para uma leitura mais atual.
    val shapes =
        Shapes(
            extraSmall =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(4.dp),
            small =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(8.dp),
            medium =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(12.dp),
            large =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(16.dp),
            extraLarge =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(28.dp),
        )

    MaterialTheme(
        colorScheme = animatedColorScheme(colorScheme),
        shapes = shapes,
        content = content,
    )
}

/**
 * Anima a troca de cores em vez de trocar tudo de uma vez num único frame — sem isso, alternar
 * tema podia parecer "pular" (uma parte da tela recompunha com a cor nova antes da outra,
 * inclusive a barra superior) mesmo a mudança sendo tecnicamente instantânea. Duração longa
 * (1.500ms) de propósito, replicando a troca de tema real do Rokku — que não pisca nem mostra
 * spinner, só demora alguns segundos e assenta de forma suave — em vez do "salto" instantâneo que
 * uma troca de cor típica do Compose (rápida, ~250ms) daria aqui.
 */
@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(1500)
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        surfaceTint = animateColorAsState(target.surfaceTint, spec, label = "surfaceTint").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, spec, label = "surfaceContainerLowest").value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec, label = "surfaceContainerLow").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec, label = "surfaceContainerHighest").value,
        surfaceBright = animateColorAsState(target.surfaceBright, spec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(target.surfaceDim, spec, label = "surfaceDim").value,
    )
}
