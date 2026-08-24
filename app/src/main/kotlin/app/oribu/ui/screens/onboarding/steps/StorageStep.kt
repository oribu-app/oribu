package app.oribu.ui.screens.onboarding.steps

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.oribu.data.StoragePreferences

@Composable
fun StorageStep() {
    val context = LocalContext.current
    val folderUri by StoragePreferences.folderUri.collectAsState()

    val pickFolder =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                StoragePreferences.setFolder(uri.toString())
            }
        }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "Escolha uma pasta no seu dispositivo para uso futuro em backups. Pode ser " +
                "configurada ou trocada depois em Configurações — esse passo é opcional.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = { pickFolder.launch(null) }) {
            Text(if (folderUri != null) "Trocar pasta" else "Selecionar pasta")
        }
        if (folderUri != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                folderUri.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
            )
        }
    }
}
