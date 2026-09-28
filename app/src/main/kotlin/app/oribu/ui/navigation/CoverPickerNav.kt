package app.oribu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import app.oribu.model.MediaItem

private const val KEY_ITEM = "coverPickerItem"
private const val KEY_RESULT = "coverPickerResult"

/** Sentinel result meaning "drop the custom cover and go back to the original one". */
const val COVER_PICKER_RESTORE = ""

/** Opens the cover picker for the given game. */
fun NavController.navigateToCoverPicker(item: MediaItem) {
    currentBackStackEntry?.savedStateHandle?.set(KEY_ITEM, item)
    navigate(Routes.GAMES_COVER)
}

fun NavController.consumeCoverPickerItem(): MediaItem? = previousBackStackEntry?.savedStateHandle?.get<MediaItem>(KEY_ITEM)

/** Hands the picked cover URL (or [COVER_PICKER_RESTORE]) back to the detail screen. */
fun NavController.returnCoverPickerResult(coverUrl: String) {
    previousBackStackEntry?.savedStateHandle?.set(KEY_RESULT, coverUrl)
}

/**
 * Watches the cover picked in the cover picker. Unlike Anotações, the picker doesn't persist
 * anything itself — the detail screen applies (and persists) the value it receives.
 */
@Composable
fun rememberCoverPickerResult(navController: NavController): String? {
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
