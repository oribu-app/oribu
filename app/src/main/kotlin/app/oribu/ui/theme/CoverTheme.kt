package app.oribu.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import app.oribu.data.CoverThemePreferences
import app.oribu.data.SavedCoverTheme
import coil.Coil
import coil.request.ImageRequest
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Replica o "Cover theme style" do Rokku (`SettingsAppearanceController.kt`, `MangaDetailsController
 * .setPaletteColor()`): extrai a cor dominante da capa via `androidx.palette` e, se um estilo do
 * `materialkolor` estiver selecionado, gera um `ColorScheme` completo a partir dela
 * (`dynamicColorScheme`); "Legacy" (estilo nulo) só substitui a cor primária do tema ambiente pela
 * cor extraída, mantendo o resto da paleta. No Rokku isso é exclusivo da tela de mangá; aqui vale
 * para todas as telas de detalhe (jogos, mangás, séries, filmes, livros).
 */
object CoverThemeController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _enabled = mutableStateOf(false)
    private val _styleName = mutableStateOf<String?>(null)

    var enabled: Boolean
        get() = _enabled.value
        set(value) {
            _enabled.value = value
            persist()
        }

    var styleName: String?
        get() = _styleName.value
        set(value) {
            _styleName.value = value
            persist()
        }

    val style: PaletteStyle?
        get() = styleName?.let { name -> PaletteStyle.entries.firstOrNull { it.name == name } }

    fun init(context: Context) {
        CoverThemePreferences.init(context)
        scope.launch {
            val saved = CoverThemePreferences.saved.first()
            _enabled.value = saved.enabled
            _styleName.value = saved.styleName
        }
    }

    private fun persist() {
        CoverThemePreferences.persist(SavedCoverTheme(enabled, styleName))
    }
}

/** Réplica de `Palette.getBestColor()` do Rokku (`LibraryMangaImageTarget.kt`) — mesma heurística. */
private fun Palette.bestColor(): Int? {
    val vibPopulation = vibrantSwatch?.population ?: -1
    val domLum = dominantSwatch?.hsl?.get(2) ?: -1f
    val mutedPopulation = mutedSwatch?.population ?: -1
    val mutedSaturationLimit = if (mutedPopulation > vibPopulation * 3f) 0.1f else 0.25f
    return when {
        (dominantSwatch?.hsl?.get(1) ?: 0f) >= .25f && domLum <= .8f && domLum > .2f -> {
            dominantSwatch?.rgb
        }

        vibPopulation >= mutedPopulation * 0.75f -> {
            vibrantSwatch?.rgb
        }

        mutedPopulation > vibPopulation * 1.5f && (mutedSwatch?.hsl?.get(1) ?: 0f) > mutedSaturationLimit -> {
            mutedSwatch?.rgb
        }

        else -> {
            listOfNotNull(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch)
                .maxByOrNull { if (it === vibrantSwatch) it.population * 3 else it.population }
                ?.rgb
        }
    }
}

private suspend fun extractCoverSeedColor(
    context: Context,
    coverUrl: String,
): Color? =
    withContext(Dispatchers.IO) {
        runCatching {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
            val result = Coil.imageLoader(context).execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
            Palette
                .from(bitmap)
                .generate()
                .bestColor()
                ?.let { Color(it) }
        }.getOrNull()
    }

/**
 * Mesma escolha de spec do Rokku (`MangaDetailsController.setPaletteColor`, comentário original:
 * faixa de matiz escolhida empiricamente na PR #83 do Rokku, não é uma recomendação documentada
 * da lib) — sem isso, algumas cores geram esquemas com contraste ruim num dos dois specs.
 */
internal fun specFor(seed: Color): ColorSpec.SpecVersion {
    val hue = Hct.fromInt(seed.toArgb()).hue
    return if (hue in 60.0..270.0) ColorSpec.SpecVersion.SPEC_2021 else ColorSpec.SpecVersion.SPEC_2025
}

private fun legacyScheme(
    base: ColorScheme,
    seed: Color,
): ColorScheme {
    val onSeed = if (seed.luminance() > 0.5f) Color.Black else Color.White
    return base.copy(primary = seed, onPrimary = onSeed, primaryContainer = seed)
}

/**
 * Envolve o conteúdo de uma tela de detalhe com o tema derivado da capa, quando a opção está
 * ligada (Settings > Aparência > Página de detalhes). Sem capa ou com a opção desligada, repassa
 * o tema ambiente sem alterações.
 */
@Composable
fun CoverThemedSurface(
    coverUrl: String?,
    content: @Composable () -> Unit,
) {
    if (!CoverThemeController.enabled || coverUrl.isNullOrBlank()) {
        content()
        return
    }
    val context = LocalContext.current
    val isDark = AppThemeController.darkMode
    val ambientScheme = MaterialTheme.colorScheme
    var scheme by remember(coverUrl) { mutableStateOf<ColorScheme?>(null) }

    LaunchedEffect(coverUrl, isDark, CoverThemeController.styleName) {
        val seed = extractCoverSeedColor(context, coverUrl)
        scheme =
            seed?.let {
                val style = CoverThemeController.style
                if (style == null) {
                    legacyScheme(ambientScheme, it)
                } else {
                    dynamicColorScheme(
                        seedColor = it,
                        isDark = isDark,
                        isAmoled = AppThemeController.pureBlackDark,
                        style = style,
                        specVersion = specFor(it),
                    )
                }
            }
    }

    val current = scheme
    if (current != null) {
        MaterialTheme(colorScheme = current, content = content)
    } else {
        content()
    }
}
