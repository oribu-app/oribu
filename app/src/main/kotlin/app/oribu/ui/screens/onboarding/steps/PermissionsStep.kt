package app.oribu.ui.screens.onboarding.steps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private fun notificationsGranted(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun batteryIgnored(context: Context) =
    (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)

@Composable
fun PermissionsStep() {
    val context = LocalContext.current
    var notifGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var batteryIgnoredState by remember { mutableStateOf(batteryIgnored(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifGranted = notificationsGranted(context)
        batteryIgnoredState = batteryIgnored(context)
    }

    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            notifGranted = notificationsGranted(context)
        }

    Column(Modifier.fillMaxWidth()) {
        Text("Opcional, mas recomendado", style = MaterialTheme.typography.titleLarge)
        PermissionItem(
            title = "Notificações",
            subtitle = "Avisos de novidades e atualizações da biblioteca",
            granted = notifGranted,
            onButtonClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        )
        PermissionItem(
            title = "Ignorar otimização de bateria",
            subtitle = "Evita que o sistema atrase a atualização diária em segundo plano",
            granted = batteryIgnoredState,
            onButtonClick = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    },
                )
            },
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    subtitle: String,
    granted: Boolean,
    onButtonClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            OutlinedButton(enabled = !granted, onClick = onButtonClick) {
                if (granted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Conceder")
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
