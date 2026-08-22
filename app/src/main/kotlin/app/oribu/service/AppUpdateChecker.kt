package app.oribu.service

import android.content.Context
import android.os.Build
import app.oribu.BuildConfig
import app.oribu.data.AppUpdatePreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GithubAsset(
    val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
)

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String?,
    val body: String?,
    @SerializedName("html_url") val htmlUrl: String,
    val assets: List<GithubAsset> = emptyList(),
)

sealed interface AppUpdateResult {
    data class NewUpdate(
        val release: GithubRelease,
    ) : AppUpdateResult

    data object NoUpdate : AppUpdateResult

    data class Error(
        val message: String,
    ) : AppUpdateResult
}

/**
 * Checa a última release do repositório certo pro tipo de build atual, comparando com a versão
 * instalada. `release`/`beta` e `nightly` são publicados em repositórios GitHub separados (ver
 * build_push.yml) — nightly nunca aparece em `/releases/latest` porque esse endpoint ignora
 * prereleases, e toda release nightly é marcada prerelease.
 */
object AppUpdateChecker {
    private val client = OkHttpClient()
    private val gson = Gson()

    val updateCheckEnabled = BuildConfig.BUILD_TYPE in setOf("release", "nightly")

    private val isNightly = BuildConfig.BUILD_TYPE == "nightly"

    val releasesUrl: String
        get() = if (isNightly) "https://github.com/oribu-app/oribu-nightly/releases" else "https://github.com/oribu-app/oribu-app/releases"

    val repoUrl = "https://github.com/oribu-app/oribu-app"

    suspend fun checkForUpdate(
        context: Context,
        isUserPrompt: Boolean,
    ): AppUpdateResult =
        withContext(Dispatchers.IO) {
            if (!updateCheckEnabled) return@withContext AppUpdateResult.NoUpdate

            if (!isUserPrompt) {
                val lastCheck = AppUpdatePreferences.getLastCheck(context)
                if (System.currentTimeMillis() - lastCheck < TimeUnit.HOURS.toMillis(24)) {
                    return@withContext AppUpdateResult.NoUpdate
                }
            }

            try {
                val release = fetchLatestRelease()
                AppUpdatePreferences.setLastCheck(context, System.currentTimeMillis())
                when {
                    release == null -> AppUpdateResult.NoUpdate
                    isNewer(release.tagName) -> AppUpdateResult.NewUpdate(release)
                    else -> AppUpdateResult.NoUpdate
                }
            } catch (e: Exception) {
                AppUpdateResult.Error(e.message ?: "Erro desconhecido")
            }
        }

    /** Escolhe o APK do ABI do aparelho, caindo pro universal se não achar um específico. */
    fun findDownloadAsset(release: GithubRelease): GithubAsset? {
        val abi = Build.SUPPORTED_ABIS.firstOrNull()
        val abiAsset = release.assets.firstOrNull { abi != null && it.name.contains(abi) }
        return abiAsset ?: release.assets.firstOrNull { it.name.startsWith("oribu-") }
    }

    private fun fetchLatestRelease(): GithubRelease? =
        if (isNightly) {
            val body = get("https://api.github.com/repos/oribu-app/oribu-nightly/releases?per_page=100")
            val type = object : TypeToken<List<GithubRelease>>() {}.type
            val releases: List<GithubRelease> = gson.fromJson(body, type) ?: emptyList()
            // Todo tag rN do oribu-nightly aponta pro mesmo commit estático (o repo não recebe
            // pushes de código), então o "created_at" que a API usa pra ordenar /releases fica
            // idêntico em todas — a ordem retornada não é confiável. Escolhe pelo maior N do
            // tag_name em vez de confiar na ordem da API.
            releases.maxByOrNull { it.tagName.removePrefix("r").toIntOrNull() ?: -1 }
        } else {
            val body = get("https://api.github.com/repos/oribu-app/oribu-app/releases/latest")
            gson.fromJson(body, GithubRelease::class.java)
        }

    private fun get(url: String): String {
        val req =
            Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github+json")
                .build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw Exception("GitHub HTTP ${response.code}")
            return response.body?.string() ?: "{}"
        }
    }

    private fun isNewer(remoteTag: String): Boolean =
        if (isNightly) {
            val remoteCount = remoteTag.removePrefix("r").toIntOrNull() ?: return false
            remoteCount > BuildConfig.COMMIT_COUNT
        } else {
            compareVersions(remoteTag.removePrefix("v"), BuildConfig.VERSION_NAME) > 0
        }

    private fun compareVersions(
        a: String,
        b: String,
    ): Int {
        val partsA = a.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val diff = partsA.getOrElse(i) { 0 } - partsB.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }
}
