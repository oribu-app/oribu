package app.oribu.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.oribu.ui.components.ServiceCredentialsList

@Composable
fun ApiKeysStep() {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Opcional — sem elas o app funciona com dados limitados. Você pode configurar tudo " +
                "isso depois em Configurações → Integrações.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(16.dp))
        ServiceCredentialsList(modifier = Modifier.fillMaxWidth())
    }
}
