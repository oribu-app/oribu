package app.oribu.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavController
import app.oribu.model.MediaStatus
import app.oribu.ui.navigation.Routes
import coil.compose.AsyncImage

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// Equivalente ao android.view.animation.DecelerateInterpolator() (factor=1) usado em
// fade_in_grow_from_top.xml/fade_out_short.xml do Rokku: 1 - (1-x)².
private val OverflowDecelerateEasing = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

// ── Overflow menu (molde do OverflowDialog + OverflowDialogTheme do Rokku):
// escurece/borra o conteúdo atrás em vez do DropdownMenu padrão sem scrim do
// Material, abrindo ancorado no canto superior direito, logo abaixo da top bar.
@Composable
fun OverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Mesma detecção do blurBehindWindow() do Rokku: blur real do conteúdo atrás do
    // diálogo só em Android 12+ com cross-window blur habilitado pelo sistema e fora
    // do modo economia de bateria; caso contrário cai no scrim escuro (0.77 vs 0.45
    // com blur — valores exatos do Rokku, em vez do scrim fixo anterior).
    val canBlur =
        remember {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.isCrossWindowBlurEnabled == true &&
                (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == false
        }

    DisposableEffect(activity, canBlur) {
        if (canBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity?.window?.decorView?.setRenderEffect(
                RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP),
            )
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.window?.decorView?.setRenderEffect(null)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // Desliga o dim padrão do próprio Dialog do Android — o scrim é desenhado à mão
        // logo abaixo, para poder usar os mesmos valores exatos do Rokku (0.77/0.45)
        // em vez de somar com o dim default do tema do diálogo.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0f)
            onDispose {}
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (canBlur) 0.45f else 0.77f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
        ) {
            Column(Modifier.align(Alignment.TopEnd)) {
                // TopAppBar (M3 "small" variant, o único usado no app) tem 64dp de altura;
                // -2dp replica o topMargin = toolbarHeight - 2dp do OverflowDialog do Rokku,
                // medido a partir do topo real da tela (ver setDecorFitsSystemWindows acima).
                Spacer(Modifier.height(62.dp))
                // Molde exato do OverflowDialogTheme do Rokku: fade_in_grow_from_top.xml
                // (escala 0.9→1.0 a partir do canto superior direito em 220ms + fade em
                // 150ms, ambos com DecelerateInterpolator) na abertura, fade_out_short.xml
                // (só fade, 150ms, mesma curva) no fechamento.
                val visibleState = remember { MutableTransitionState(false) }
                visibleState.targetState = true
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter =
                        fadeIn(tween(150, easing = OverflowDecelerateEasing)) +
                            scaleIn(
                                initialScale = 0.9f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(220, easing = OverflowDecelerateEasing),
                            ),
                    exit = fadeOut(tween(150, easing = OverflowDecelerateEasing)),
                ) {
                    // Mistura manual com a cor secundária (blendARGB a 7.5%, igual ao
                    // overflowCardView do Rokku) em vez do tonalElevation padrão do M3, que
                    // mistura com a cor primária e não bate com o visual de lá.
                    val cardColor = lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.secondary, 0.075f)
                    // width(IntrinsicSize.Max) faz o card abraçar o item mais largo do
                    // conteúdo (equivalente ao overflow_card_view "wrap_content" +
                    // constraintWidth_min do Rokku) em vez do fillMaxWidth() dos
                    // OverflowMenuItem esticar o card até a largura da tela inteira.
                    Surface(
                        modifier = Modifier.padding(start = 24.dp, end = 14.dp).width(IntrinsicSize.Max).widthIn(min = 250.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = cardColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Column(Modifier.padding(vertical = 6.dp), content = content)
                    }
                }
            }
        }
    }
}

@Composable
fun OverflowMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Menu de três pontos padrão (Configurações/Status/Histórico/Sobre/Ajuda) ─
// Usado pela Home e por todas as telas de listagem (Jogos/Filmes/Séries/Mangás/Livros),
// no molde do menu "More" do Rokku (mesmas opções em toda tela com toolbar).
@Composable
fun AppOverflowMenu(
    navController: NavController,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName =
        remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrDefault("—")
        }

    OverflowMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        OverflowMenuItem(
            text = "Configurações",
            icon = Icons.Outlined.Settings,
            onClick = {
                onDismissRequest()
                navController.navigate(Routes.SETTINGS)
            },
        )
        OverflowMenuItem(
            text = "Status",
            icon = Icons.Outlined.QueryStats,
            onClick = {
                onDismissRequest()
                navController.navigate(Routes.STATS)
            },
        )
        OverflowMenuItem(
            text = "Histórico",
            icon = Icons.Outlined.History,
            onClick = {
                onDismissRequest()
                navController.navigate(Routes.HISTORY)
            },
        )
        OverflowMenuItem(
            text = "Sobre",
            subtitle = "v$versionName",
            icon = Icons.Outlined.Info,
            onClick = {
                onDismissRequest()
                navController.navigate(Routes.ABOUT)
            },
        )
        OverflowMenuItem(
            text = "Ajuda",
            icon = Icons.Outlined.Help,
            onClick = {
                onDismissRequest()
                uriHandler.openUri("https://github.com/oribu-app/oribu-app/issues")
            },
        )
    }
}

// ── Avaliação por estrelas (0 a 5) ──────────────────────────────────────────
@Composable
fun StarRatingPicker(
    rating: Int,
    onRatingChange: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { star ->
            Icon(
                if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable { onRatingChange(star) },
            )
        }
    }
}

@Composable
fun StarRatingDisplay(rating: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { star ->
            Icon(
                if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Avaliação por estrelas com meia-estrela (0 a 5, passo 0.5) ──────────────
// Usado pela avaliação de Livros: cada metade de estrela tem uma frase curta
// associada (ver bookRatingPhrase), então o rating precisa granularidade de 0.5.
private val halfStarPhrases =
    mapOf(
        0.5 to "Péssimo",
        1.0 to "Muito ruim",
        1.5 to "Ruim",
        2.0 to "Fraco",
        2.5 to "Mediano",
        3.0 to "Ok",
        3.5 to "Bom",
        4.0 to "Muito bom",
        4.5 to "Excelente",
        5.0 to "Obra-prima",
    )

fun bookRatingPhrase(rating: Double?): String? = rating?.let { r -> halfStarPhrases[(kotlin.math.round(r * 2) / 2.0)] }

@Composable
fun HalfStarRatingPicker(
    rating: Double,
    onRatingChange: (Double) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 5) {
            val starValue = i + 1
            val icon =
                when {
                    rating >= starValue -> Icons.Default.Star
                    rating >= starValue - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                    else -> Icons.Default.StarBorder
                }
            Box(
                Modifier
                    .size(32.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val half = offset.x < size.width / 2f
                            onRatingChange(if (half) starValue - 0.5 else starValue.toDouble())
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun HalfStarRatingDisplay(rating: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until 5) {
            val starValue = i + 1
            val icon =
                when {
                    rating >= starValue -> Icons.Default.Star
                    rating >= starValue - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                    else -> Icons.Default.StarBorder
                }
            Icon(icon, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Filtro por gênero (tela geral de Filmes/Séries) ─────────────────────────
@Composable
fun GenreFilterRow(
    genres: List<String>,
    selected: String?,
    color: Color,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(genres) { genre ->
            val isSelected = genre == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else genre) },
                label = { Text(genre) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.18f),
                        selectedLabelColor = color,
                    ),
            )
        }
    }
}

// ── Anotações (preview na tela de detalhe) ──────────────────────────────────
// Só aparece quando existe conteúdo salvo; tocar no lápis abre a AnotacoesScreen.
@Composable
fun AnotacoesSection(
    text: String?,
    onEditClick: () -> Unit,
) {
    if (text.isNullOrBlank()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Anotações", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Editar anotação", modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Card(shape = RoundedCornerShape(12.dp), onClick = onEditClick) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Proportional Tab Row ──────────────────────────────────────────────────────
// Cada aba recebe largura proporcional ao texto medido, preenchendo a tela sem scroll.
// Conversão fiel do ProportionalTabBar do Flutter (shared_widgets.dart).
@Composable
fun ProportionalTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    selectedColor: Color,
    onTabSelected: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val boldStyle = remember { TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
    val unselColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val screenWidthPx =
        with(density) {
            LocalConfiguration.current.screenWidthDp.dp
                .toPx()
        }

    // Mede a largura de cada label (bold = variante mais larga)
    val textWidthsPx =
        remember(tabs) {
            tabs.map {
                textMeasurer
                    .measure(it, boldStyle)
                    .size.width
                    .toFloat()
            }
        }
    val totalTextPx = textWidthsPx.sum()

    // Padding horizontal uniforme: distribui o espaço restante igualmente
    val hPaddingPx = ((screenWidthPx - totalTextPx) / (2 * tabs.size)).coerceAtLeast(0f)

    val tabWidths = textWidthsPx.map { with(density) { (it + 2 * hPaddingPx).toDp() } }
    val textWidths = textWidthsPx.map { with(density) { it.toDp() } }

    Column(Modifier.fillMaxWidth()) {
        // Labels
        Row(Modifier.fillMaxWidth().height(44.dp)) {
            tabs.forEachIndexed { i, label ->
                val selected = i == selectedTabIndex
                Box(
                    Modifier
                        .width(tabWidths[i])
                        .fillMaxHeight()
                        .clickable { onTabSelected(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) selectedColor else unselColor,
                    )
                }
            }
        }
        // Indicator
        Row(Modifier.fillMaxWidth().height(2.dp)) {
            tabs.forEachIndexed { i, _ ->
                Box(Modifier.width(tabWidths[i]).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (i == selectedTabIndex) {
                        Box(Modifier.width(textWidths[i]).fillMaxHeight().background(selectedColor))
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Transparent,
) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        val bg =
            if (accentColor == Color.Transparent) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                accentColor.copy(alpha = 0.15f)
            }
        Box(modifier.background(bg), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = null,
                tint =
                    if (accentColor == Color.Transparent) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    } else {
                        accentColor.copy(alpha = 0.4f)
                    },
            )
        }
    }
}

@Composable
fun MediaGridCard(
    title: String,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Transparent,
    inLibrary: Boolean = false,
    onAddClick: (() -> Unit)? = null,
) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.56f)) {
            CoverImage(url = coverUrl, modifier = Modifier.fillMaxSize(), accentColor = accentColor)
            if (inLibrary) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Na biblioteca",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            } else if (onAddClick != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onAddClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
fun StatusChip(
    status: MediaStatus,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = status.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = status.label,
            color = status.color,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    buttonLabel: String? = null,
    onButton: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
        if (buttonLabel != null && onButton != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onButton) { Text(buttonLabel) }
        }
    }
}

// ── Status option tile ───────────────────────────────────────────────────────
// Usado nas telas de "Adicionar" (Filmes, Jogos, Séries, Livros, Mangás) para
// listar as opções de status com ícone + descrição curta, em vez de uma lista simples.
@Composable
fun StatusOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(150),
        label = "bg",
    )
    val borderColor by animateColorAsState(
        if (selected) color else Color.Transparent,
        animationSpec = tween(150),
        label = "border",
    )
    val iconColor = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) color else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}
