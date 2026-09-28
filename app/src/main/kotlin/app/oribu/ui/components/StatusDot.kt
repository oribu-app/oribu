package app.oribu.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.oribu.R

enum class CredentialStatus(
    @StringRes val labelRes: Int,
) {
    NOT_CONFIGURED(R.string.credential_status_not_configured),
    CONFIGURED(R.string.credential_status_saved),
    BUILT_IN(R.string.credential_status_built_in),
    TESTING(R.string.credential_status_testing),
    VALID(R.string.credential_status_connected),
    INVALID(R.string.credential_status_error),
}

/** Indicador colorido de status de credencial (config./testando/válida/inválida), no molde do tonkatsu_box. */
@Composable
fun StatusDot(status: CredentialStatus) {
    val color =
        when (status) {
            CredentialStatus.VALID, CredentialStatus.CONFIGURED, CredentialStatus.BUILT_IN -> Color(0xFF4CAF50)
            CredentialStatus.INVALID -> Color(0xFFE53935)
            CredentialStatus.NOT_CONFIGURED, CredentialStatus.TESTING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        }
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}
