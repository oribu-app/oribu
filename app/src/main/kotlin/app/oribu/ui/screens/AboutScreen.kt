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
import androidx.compose.ui.res.stringResource
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
import java.time.temporal.ChronoUnit

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
                Instant.ofEpochMilli(BuildConfig.BUILD_TIME).truncatedTo(ChronoUnit.SECONDS).toString()
            }.getOrDefault("—")
        }

    var checkingUpdate by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<GithubRelease?>(null) }

    val alreadyLatestMessage = stringResource(R.string.about_already_latest)
    val checkErrorMessage = stringResource(R.string.about_check_error)
    val downloadingMessage = stringResource(R.string.about_downloading)
    val noInstallerMessage = stringResource(R.string.about_no_installer)
    val copiedMessage = stringResource(R.string.about_copied)

    fun checkForUpdate() {
        checkingUpdate = true
        scope.launch {
            when (val result = AppUpdateChecker.checkForUpdate(context, isUserPrompt = true)) {
                is AppUpdateResult.NewUpdate -> pendingUpdate = result.release
                is AppUpdateResult.NoUpdate -> snackbarHostState.showSnackbar(alreadyLatestMessage)
                is AppUpdateResult.Error -> snackbarHostState.showSnackbar(checkErrorMessage.format(result.message))
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
                    scope.launch { snackbarHostState.showSnackbar(downloadingMessage) }
                } else {
                    scope.launch { snackbarHostState.showSnackbar(noInstallerMessage) }
                }
                pendingUpdate = null
            },
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors =
                    TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                AboutRow(
                    title = stringResource(R.string.about_whats_new),
                    onClick = { uriHandler.openUri(AppUpdateChecker.releasesUrl) },
                )
            }
            if (AppUpdateChecker.updateCheckEnabled) {
                item {
                    AboutRow(
                        title = stringResource(R.string.about_check_updates),
                        subtitle = if (checkingUpdate) stringResource(R.string.about_checking) else null,
                        onClick = if (checkingUpdate) null else ::checkForUpdate,
                    )
                }
            }
            item {
                AboutRow(
                    title = stringResource(R.string.about_version),
                    subtitle = versionName ?: "—",
                    onClick = {
                        val debugInfo =
                            "Oribu $versionName (${BuildConfig.BUILD_TYPE})\n" +
                                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                "${Build.MANUFACTURER} ${Build.MODEL}"
                        clipboardManager.setText(AnnotatedString(debugInfo))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        }
                    },
                )
            }
            item { AboutRow(title = stringResource(R.string.about_build_date), subtitle = buildTimeLabel) }
            item {
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    AboutRow(title = stringResource(R.string.about_help_translate))
                }
            }
            item {
                AboutRow(
                    title = stringResource(R.string.about_open_source_licenses),
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
        title = { Text(stringResource(R.string.about_new_version_available, release.tagName)) },
        text = {
            Text(
                release.body?.takeIf { it.isNotBlank() } ?: stringResource(R.string.about_no_release_notes),
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.action_update)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ignore)) } },
    )
}
