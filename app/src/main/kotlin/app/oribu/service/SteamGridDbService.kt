package app.oribu.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLEncoder
import java.util.Calendar
import java.util.TimeZone

data class SteamGridDbGame(
    val id: Int,
    val name: String,
    val releaseYear: Int?,
)

data class SteamGridDbCover(
    val id: Int,
    val url: String,
    val thumbUrl: String,
    val author: String?,
)

/**
 * SteamGridDB — community-made cover art (grids). Used as a fallback when neither IGDB nor the
 * local game cache has a cover for a game, and as the source of the "Change cover" picker.
 * Requires a free personal API key (steamgriddb.com → Preferences → API).
 */
class SteamGridDbService(
    private val apiKey: String,
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val base = "https://www.steamgriddb.com/api/v2"

    private fun get(url: String): Map<String, Any?> {
        val req =
            Request
                .Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
        val response = client.newCall(req).execute()
        val code = response.code
        val resp = response.use { it.body?.string() ?: "{}" }
        when (code) {
            401, 403 -> throw ApiException(PROVIDER, ApiErrorReason.UNAUTHORIZED)
            404 -> throw ApiException(PROVIDER, ApiErrorReason.NOT_FOUND)
            429 -> throw ApiException(PROVIDER, ApiErrorReason.RATE_LIMITED)
            !in 200..299 -> throw ApiException(PROVIDER, ApiErrorReason.HTTP_ERROR, code)
        }
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(resp, type) ?: emptyMap()
    }

    private fun data(url: String): List<Map<String, Any?>> =
        (get(url)["data"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()

    private fun encodePath(value: String) = URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")

    /** Any authenticated call works for validation — a bad key answers 401. */
    fun testConnection() {
        get("$base/search/autocomplete/${encodePath("portal")}")
    }

    /** Games matching [title], best match first. */
    fun searchGames(title: String): List<SteamGridDbGame> {
        if (title.isBlank()) return emptyList()
        return data("$base/search/autocomplete/${encodePath(title)}").mapNotNull { g ->
            val id = (g["id"] as? Double)?.toInt() ?: return@mapNotNull null
            val name = g["name"] as? String ?: return@mapNotNull null
            val releaseYear =
                (g["release_date"] as? Double)?.toLong()?.let { seconds ->
                    Calendar
                        .getInstance(TimeZone.getTimeZone("UTC"))
                        .apply { timeInMillis = seconds * 1000 }
                        .get(Calendar.YEAR)
                }
            SteamGridDbGame(id = id, name = name, releaseYear = releaseYear)
        }
    }

    /** Portrait static covers for a SteamGridDB game id, highest-voted first. */
    fun getCovers(gameId: Int): List<SteamGridDbCover> = covers("$base/grids/game/$gameId")

    /** Same as [getCovers], but looked up straight from a Steam app id (no title matching). */
    fun getCoversBySteamAppId(appId: Int): List<SteamGridDbCover> = covers("$base/grids/steam/$appId")

    private fun covers(endpoint: String): List<SteamGridDbCover> =
        data("$endpoint?dimensions=$PORTRAIT_DIMENSIONS&types=static&nsfw=false&humor=false").mapNotNull { c ->
            val id = (c["id"] as? Double)?.toInt() ?: return@mapNotNull null
            val url = c["url"] as? String ?: return@mapNotNull null
            SteamGridDbCover(
                id = id,
                url = url,
                thumbUrl = c["thumb"] as? String ?: url,
                author = (c["author"] as? Map<*, *>)?.get("name") as? String,
            )
        }

    fun findCoverByTitle(title: String): String? = searchGames(title).firstOrNull()?.let { getCovers(it.id).firstOrNull()?.url }

    companion object {
        private const val PROVIDER = "SteamGridDB"

        // 2:3 portrait grids — the same shape as IGDB covers used everywhere else in the app.
        private const val PORTRAIT_DIMENSIONS = "600x900,342x482,660x930"

        /**
         * A SteamGridDB-hosted cover on an item was either picked by the user or filled in as the
         * no-cover fallback — either way it must win over the IGDB cover cached for the game.
         */
        fun isSteamGridDbUrl(url: String?): Boolean =
            url != null &&
                runCatching { URI(url).host }
                    .getOrNull()
                    ?.let { it == "steamgriddb.com" || it.endsWith(".steamgriddb.com") } == true
    }
}
