package app.oribu.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/**
 * Detecta um arraste predominantemente horizontal (com viés de 1.5x sobre o vertical antes de
 * decidir) para não competir com o scroll vertical das grades — só passa a consumir o gesto
 * depois de confirmar que é uma navegação por swipe, então listas verticais continuam intactas.
 */
fun Modifier.swipeNavigation(
    enabled: Boolean = true,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier {
    if (!enabled) return this
    return this.pointerInput(onSwipeLeft, onSwipeRight) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var isHorizontal: Boolean? = null
            var totalDx = 0f
            var totalDy = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) {
                    if (isHorizontal == true) {
                        if (totalDx < -120f) {
                            onSwipeLeft()
                        } else if (totalDx > 120f) {
                            onSwipeRight()
                        }
                    }
                    break
                }
                val delta = change.positionChange()
                totalDx += delta.x
                totalDy += delta.y
                if (isHorizontal == null && (abs(totalDx) > 16f || abs(totalDy) > 16f)) {
                    isHorizontal = abs(totalDx) > abs(totalDy) * 1.5f
                }
                if (isHorizontal == true) change.consume()
            }
        }
    }
}
