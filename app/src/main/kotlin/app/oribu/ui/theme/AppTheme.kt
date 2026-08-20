package app.oribu.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
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
import kotlinx.coroutines.launch

// ── Controller global de tema ────────────────────────────────────────────────

enum class ThemeMode { DARK, LIGHT, SYSTEM }

object AppThemeController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Tema claro e escuro são escolhidos de forma independente (cada um pode ser
    // qualquer uma das paletas disponíveis) — o modo de cor decide qual dos dois
    // está ativo no momento, não qual paleta usar.
    private val _lightThemeId = mutableStateOf("neko")
    private val _darkThemeId = mutableStateOf("neko")
    private val _themeMode = mutableStateOf(ThemeMode.DARK)

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

    /** Carrega o tema salvo (chamado uma vez em `OribuApp.onCreate`). */
    fun init(context: Context) {
        ThemePreferences.init(context)
        scope.launch {
            ThemePreferences.saved.collect { saved ->
                _lightThemeId.value = saved.lightThemeId
                _darkThemeId.value = saved.darkThemeId
                _themeMode.value = saved.themeMode
            }
        }
    }

    private fun persist() {
        ThemePreferences.persist(SavedTheme(lightThemeId, darkThemeId, themeMode))
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

    val colorScheme =
        if (darkTheme) {
            darkColorScheme(
                primary = def.seedDark,
                background = def.bgDark,
                surface = def.surfaceDark,
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White,
            )
        } else {
            lightColorScheme(
                primary = def.seedLight,
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
        colorScheme = colorScheme,
        shapes = shapes,
        content = content,
    )
}
