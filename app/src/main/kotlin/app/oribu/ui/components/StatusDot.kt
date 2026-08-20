package app.oribu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class CredentialStatus { NOT_CONFIGURED, CONFIGURED, TESTING, VALID, INVALID }

/** Indicador colorido de status de credencial (config./testando/válida/inválida), no molde do tonkatsu_box. */
@Composable
fun StatusDot(status: CredentialStatus) {
    if (status == CredentialStatus.TESTING) {
        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
        return
    }
    val color =
        when (status) {
            CredentialStatus.VALID, CredentialStatus.CONFIGURED -> Color(0xFF4CAF50)
            CredentialStatus.INVALID -> Color(0xFFE53935)
            CredentialStatus.NOT_CONFIGURED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            CredentialStatus.TESTING -> Color.Transparent
        }
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}
