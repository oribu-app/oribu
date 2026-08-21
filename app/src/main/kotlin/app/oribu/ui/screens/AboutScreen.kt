package app.oribu.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SystemUpdate
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.BuildConfig
import app.oribu.service.AppUpdateChecker
import app.oribu.service.AppUpdateResult
import app.oribu.service.GithubRelease
import app.oribu.ui.navigation.Routes
import app.oribu.worker.AppUpdateInstallWorker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
                ListItem(
                    headlineContent = { Text("Oribu") },
                    supportingContent = { Text("Versão $versionName") },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Desenvolvedor") },
                    supportingContent = { Text("Thiago Rocha") },
                    leadingContent = { Icon(Icons.Default.Person, null) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Novidades desta versão") },
                    leadingContent = { Icon(Icons.Outlined.Article, null) },
                    modifier = Modifier.clickable { uriHandler.openUri(AppUpdateChecker.releasesUrl) },
                )
            }
            if (AppUpdateChecker.updateCheckEnabled) {
                item {
                    ListItem(
                        headlineContent = { Text("Verificar atualizações") },
                        supportingContent = { if (checkingUpdate) Text("Checando...") },
                        leadingContent = {
                            if (checkingUpdate) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.SystemUpdate, null)
                            }
                        },
                        modifier = Modifier.clickable(enabled = !checkingUpdate) { checkForUpdate() },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Versão") },
                    supportingContent = { Text(versionName ?: "—") },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    modifier =
                        Modifier.clickable {
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
            item {
                ListItem(
                    headlineContent = { Text("Data do build") },
                    supportingContent = { Text(buildTimeLabel) },
                    leadingContent = { Icon(Icons.Outlined.Schedule, null) },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("Ajuda") },
                    leadingContent = { Icon(Icons.Outlined.Help, null) },
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/oribu-app/oribu-app/issues") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Licenças de código aberto") },
                    leadingContent = { Icon(Icons.Outlined.Code, null) },
                    modifier = Modifier.clickable { navController.navigate(Routes.ABOUT_LICENSES) },
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = { uriHandler.openUri(AppUpdateChecker.repoUrl) }) {
                        Icon(Icons.Outlined.Code, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GitHub")
                    }
                }
            }
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
