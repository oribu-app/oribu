package app.oribu.ui.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.oribu.data.SavedTheme
import app.oribu.data.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    // `darkColorScheme()`/`lightColorScheme()` preenchem todo papel de cor não passado
    // explicitamente com a paleta padrão roxa do Material — inclusive `surfaceTint`, que o
    // Card/Surface do M3 usa pra colorir qualquer elevação, deixando superfícies elevadas com um
    // véu roxo por cima da cor do tema. `secondary` = `def.accentDark/Light`, o `colorSecondary`
    // real do Rokku (não uma variação de `colorPrimary`) — é essa cor, não a primary, que o Rokku
    // usa pra colorir a borda de seleção e o cabeçalho de categoria tintado (ver comentário em
    // `AppThemeDefinition.accentDark`); usar `colorScheme.primary` ali (como antes) deixava tudo
    // mais claro/"sóbrio" do que o Rokku de verdade.
    val colorScheme =
        if (darkTheme) {
            val pureBlack = AppThemeController.pureBlackDark
            darkColorScheme(
                primary = def.seedDark,
                secondary = def.accentDark,
                surfaceTint = def.seedDark,
                background = if (pureBlack) Color.Black else def.bgDark,
                surface = if (pureBlack) Color.Black else def.surfaceDark,
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White,
            )
        } else {
            lightColorScheme(
                primary = def.seedLight,
                secondary = def.accentLight,
                surfaceTint = def.seedLight,
                background = def.bgLight,
                surface = def.surfaceLight,
                onPrimary = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black,
            )
        }

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
 * inclusive a barra superior) mesmo a mudança sendo tecnicamente instantânea.
 */
@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(250)
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
    )
}
