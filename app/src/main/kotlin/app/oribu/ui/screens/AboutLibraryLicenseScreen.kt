package app.oribu.ui.screens

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLibraryLicenseScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    val entry = navController.previousBackStackEntry
    val name = remember { entry?.savedStateHandle?.get<String>("libraryName") ?: "" }
    val website = remember { entry?.savedStateHandle?.get<String>("libraryWebsite") }
    val licenseHtml = remember { entry?.savedStateHandle?.get<String>("licenseHtml") ?: "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (!website.isNullOrBlank()) {
                        IconButton(onClick = { uriHandler.openUri(website) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = "Abrir site")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (licenseHtml.isBlank()) {
            Text(
                "Sem texto de licença disponível.",
                modifier = Modifier.padding(padding).padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            // TextView não herda o ColorScheme do Compose sozinho — sem isso o texto sai quase
            // preto (cor padrão do tema do sistema) sobre o fundo escuro do app.
            val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
            AndroidView(
                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                factory = { context ->
                    TextView(context).apply {
                        textSize = 13.sp.value
                        setLinkTextColor(textColor)
                    }
                },
                update = { textView ->
                    textView.setTextColor(textColor)
                    textView.text = HtmlCompat.fromHtml(licenseHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
                },
            )
        }
    }
}
