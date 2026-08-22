package app.oribu.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.oribu.BuildConfig
import app.oribu.R
import app.oribu.service.AppUpdateChecker
import app.oribu.service.AppUpdateResult
import app.oribu.service.GithubRelease
import app.oribu.ui.navigation.Routes
import app.oribu.worker.AppUpdateInstallWorker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val versionName =
        remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrDefault("—")
        }
    val buildTimeLabel =
        remember {
            runCatching {
                DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(BuildConfig.BUILD_TIME))
            }.getOrDefault("—")
        }

    var checkingUpdate by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<GithubRelease?>(null) }

    fun checkForUpdate() {
        checkingUpdate = true
        scope.launch {
            when (val result = AppUpdateChecker.checkForUpdate(context, isUserPrompt = true)) {
                is AppUpdateResult.NewUpdate -> pendingUpdate = result.release
                is AppUpdateResult.NoUpdate -> snackbarHostState.showSnackbar("Você já está na versão mais recente")
                is AppUpdateResult.Error -> snackbarHostState.showSnackbar("Erro ao checar atualizações: ${result.message}")
            }
            checkingUpdate = false
        }
    }

    pendingUpdate?.let { release ->
        UpdateAvailableDialog(
            release = release,
            onDismiss = { pendingUpdate = null },
            onUpdate = {
                val asset = AppUpdateChecker.findDownloadAsset(release)
                if (asset != null) {
                    AppUpdateInstallWorker.enqueue(context, asset.downloadUrl, asset.name)
                    scope.launch { snackbarHostState.showSnackbar("Baixando atualização em segundo plano") }
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Nenhum instalador encontrado nessa release") }
                }
                pendingUpdate = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                AboutRow(
                    title = "O que há de novo neste lançamento",
                    onClick = { uriHandler.openUri(AppUpdateChecker.releasesUrl) },
                )
            }
            if (AppUpdateChecker.updateCheckEnabled) {
                item {
                    AboutRow(
                        title = "Procurar por atualizações",
                        subtitle = if (checkingUpdate) "Checando..." else null,
                        onClick = if (checkingUpdate) null else ::checkForUpdate,
                    )
                }
            }
            item {
                AboutRow(
                    title = "Versão",
                    subtitle = versionName ?: "—",
                    onClick = {
                        val debugInfo =
                            "Oribu $versionName (${BuildConfig.BUILD_TYPE})\n" +
                                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                "${Build.MANUFACTURER} ${Build.MODEL}"
                        clipboardManager.setText(AnnotatedString(debugInfo))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            scope.launch { snackbarHostState.showSnackbar("Copiado para a área de transferência") }
                        }
                    },
                )
            }
            item { AboutRow(title = "Data de compilação", subtitle = buildTimeLabel) }
            item {
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    AboutRow(title = "Ajuda", onClick = { uriHandler.openUri("https://github.com/oribu-app/oribu-app/issues") })
                }
            }
            item {
                AboutRow(
                    title = "Licenças de código aberto",
                    onClick = { navController.navigate(Routes.ABOUT_LICENSES) },
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        modifier = Modifier.padding(4.dp),
                        onClick = { uriHandler.openUri(AppUpdateChecker.repoUrl) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github_24dp),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "GitHub",
                        )
                    }
                }
            }
        }
    }
}

/**
 * Linha no molde do TextPreferenceWidget do Rokku: sem ícone, título 16sp + subtítulo opcional
 * em bodySmall meio apagado (alpha), 16dp de padding em toda volta, altura mínima de 56dp.
 */
@Composable
private fun AboutRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(title, fontSize = 16.sp)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    release: GithubRelease,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova versão disponível: ${release.tagName}") },
        text = {
            Text(
                release.body?.takeIf { it.isNotBlank() } ?: "Sem notas de versão.",
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text("Atualizar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ignorar") } },
    )
}
