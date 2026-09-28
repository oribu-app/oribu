package app.oribu.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.oribu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service logo shown on credential and credits cards. Like Rokku's tracker logos, each one sits
 * on a tile in the brand color so logos drawn for dark backgrounds (AniList, IGDB) stay readable
 * in any theme. Logos come from Tonkatsu Box (MIT) and Rokku (AniList, MangaBaka); services with
 * no logo in either (ITAD, HowLongToBeat) use an icon instead.
 */
enum class ServiceLogo(
    @DrawableRes val drawable: Int?,
    val background: Color,
    val fallbackIcon: ImageVector = Icons.Default.Sell,
) {
    TMDB(R.drawable.ic_service_tmdb, Color(0xFF0D253F)),
    IGDB(R.drawable.ic_service_igdb, Color(0xFF9147FF)),
    HLTB(null, Color(0xFF2A3F5F), Icons.Default.Timer),
    STEAMGRIDDB(R.drawable.ic_service_steamgriddb, Color.White),
    ITAD(null, Color(0xFF046EB4)),
    GOOGLE_BOOKS(R.drawable.ic_service_google_books, Color.White),
    OPEN_LIBRARY(R.drawable.ic_service_open_library, Color.White),
    HARDCOVER(R.drawable.ic_service_hardcover, Color.White),
    STEAM(R.drawable.ic_service_steam, Color(0xFF171A21)),
    RETROACHIEVEMENTS(R.drawable.ic_service_retroachievements, Color(0xFF161B22)),
    ANILIST(R.drawable.ic_service_anilist, Color(0xFF121923)),
    MANGADEX(R.drawable.ic_service_mangadex, Color(0xFF2C2C2C)),
    MANGABAKA(R.drawable.ic_service_mangabaka, Color.White),
    KITSU(R.drawable.ic_service_kitsu, Color.White),
}

@Composable
internal fun ServiceLogoBadge(
    logo: ServiceLogo,
    size: Dp = 40.dp,
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(logo.background)
            .padding(size * 0.15f),
        contentAlignment = Alignment.Center,
    ) {
        if (logo.drawable != null) {
            Image(painterResource(logo.drawable), contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Icon(logo.fallbackIcon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun builtInPlaceholder(usingBuiltIn: Boolean): String? =
    if (usingBuiltIn) stringResource(R.string.credential_built_in_placeholder) else null

@Composable
internal fun CredentialSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
internal fun ServiceCredentialCard(
    name: String,
    description: String,
    logo: ServiceLogo,
    status: CredentialStatus,
    onTest: () -> Unit,
    onReset: (() -> Unit)? = null,
    usingBuiltIn: Boolean = false,
    testAlwaysAvailable: Boolean = false,
    /** Extra controls under the status line (Tracking: automatic sync switch, "Sync now"). */
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    fields: @Composable ColumnScope.(usingBuiltIn: Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceLogoBadge(logo)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            fields(usingBuiltIn)
            if (usingBuiltIn) OwnKeyHint()
            Spacer(Modifier.height(8.dp))
            CredentialStatusRow(
                status = status,
                testEnabled = testAlwaysAvailable || status != CredentialStatus.NOT_CONFIGURED,
                onTest = onTest,
                onReset = onReset,
            )
            if (footer != null) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                footer()
            }
        }
    }
}

/** "For better rate limits we recommend using your own API key" — same hint as Tonkatsu Box. */
@Composable
private fun OwnKeyHint() {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.credential_own_key_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun CredentialStatusRow(
    status: CredentialStatus,
    testEnabled: Boolean,
    onTest: () -> Unit,
    onReset: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(status)
        Text(
            stringResource(status.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        if (onReset != null) {
            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.credential_reset_to_built_in),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(
            onClick = onTest,
            enabled = testEnabled && status != CredentialStatus.TESTING,
            modifier = Modifier.size(32.dp),
        ) {
            if (status == CredentialStatus.TESTING) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = stringResource(R.string.service_test_connection),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** [own] = the user's credential(s); [bundled] = built-in key(s) that stand in when [own] is incomplete. */
internal fun credentialStatusFor(
    own: List<String?>,
    bundled: List<String?> = emptyList(),
): CredentialStatus =
    when {
        own.none { it.isNullOrBlank() } -> CredentialStatus.CONFIGURED
        bundled.isNotEmpty() && bundled.none { it.isNullOrBlank() } -> CredentialStatus.BUILT_IN
        else -> CredentialStatus.NOT_CONFIGURED
    }

/** Runs [check] off the main thread, reporting TESTING and then VALID/INVALID through [setStatus]. */
internal fun CoroutineScope.testCredential(
    setStatus: (CredentialStatus) -> Unit,
    check: () -> Unit,
) {
    setStatus(CredentialStatus.TESTING)
    launch {
        val ok = withContext(Dispatchers.IO) { runCatching { check() } }.isSuccess
        setStatus(if (ok) CredentialStatus.VALID else CredentialStatus.INVALID)
    }
}
