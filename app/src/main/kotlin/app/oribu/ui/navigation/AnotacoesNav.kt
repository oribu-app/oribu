package app.oribu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import app.oribu.model.MediaItem

private const val KEY_ITEM = "anotacoesItem"
private const val KEY_RESULT = "anotacoesResult"

/** Abre a tela de Anotações para o item informado. */
fun NavController.navigateToAnotacoes(item: MediaItem) {
    currentBackStackEntry?.savedStateHandle?.set(KEY_ITEM, item)
    navigate(Routes.ANOTACOES)
}

/** Usado pela AnotacoesScreen para ler o item recebido e devolver o texto salvo ao voltar. */
fun NavController.consumeAnotacoesItem(): MediaItem? = previousBackStackEntry?.savedStateHandle?.get<MediaItem>(KEY_ITEM)

fun NavController.returnAnotacoesResult(text: String) {
    previousBackStackEntry?.savedStateHandle?.set(KEY_RESULT, text)
}

/**
 * Observa o texto de anotação devolvido pela AnotacoesScreen ao voltar. As telas de
 * detalhe chamam isso e aplicam o valor recebido ao estado local (o texto já foi
 * persistido no banco pela própria AnotacoesScreen).
 */
@Composable
fun rememberAnotacoesResult(navController: NavController): String? {
    val entry = navController.currentBackStackEntry
    var result by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(entry) {
        val handle = entry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow<String?>(KEY_RESULT, null).collect { value ->
            if (value != null) {
                result = value
                handle.remove<String>(KEY_RESULT)
            }
        }
    }
    return result
}
