package app.oribu.service

import app.oribu.model.GameConsole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

data class RetroAchievement(
    val name: String,
    val description: String?,
    val badgeUrl: String?,
    val achieved: Boolean,
)

data class RetroGameProgress(
    val gameId: Int,
    val title: String,
    val achievements: List<RetroAchievement>,
) {
    val total get() = achievements.size
    val unlocked get() = achievements.count { it.achieved }
}

/**
 * RetroAchievements — achievements for retro games played on emulators. Needs the user's
 * RetroAchievements username plus their personal Web API key (retroachievements.org → Settings →
 * Authentication), the same "decoupled personal key" model as Steam.
 */
class RetroAchievementsService(
    private val username: String,
    private val apiKey: String,
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val base = "https://retroachievements.org/API"

    private fun get(
        endpoint: String,
        params: String = "",
    ): Map<String, Any?> {
        val user = URLEncoder.encode(username, "UTF-8")
        val key = URLEncoder.encode(apiKey, "UTF-8")
        val req = Request.Builder().url("$base/$endpoint?u=$user&y=$key$params").build()
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

    /** Validates both the key (401 otherwise) and the username (empty profile otherwise). */
    fun testConnection() {
        if (get("API_GetUserProfile.php")["User"] == null) {
            throw ApiException(PROVIDER, ApiErrorReason.NOT_FOUND)
        }
    }

    /**
     * Progress for the game matching [title] on [console], looked up among the games the user
     * has actually played on RetroAchievements — null when the console isn't supported or the
     * user never played a matching game.
     */
    fun findProgress(
        title: String,
        console: GameConsole?,
    ): RetroGameProgress? {
        val consoleId = consoleIds[console] ?: return null
        val wanted = normalizeTitle(title)
        val gameId =
            playedGames()
                .filter { (it["ConsoleID"] as? Double)?.toInt() == consoleId }
                .firstOrNull { normalizeTitle(it["Title"] as? String ?: "") == wanted }
                ?.let { (it["GameID"] as? Double)?.toInt() }
                ?: return null
        return getGameProgress(gameId)
    }

    private fun playedGames(): List<Map<String, Any?>> {
        val games = mutableListOf<Map<String, Any?>>()
        var offset = 0
        while (true) {
            val page = get("API_GetUserCompletionProgress.php", "&c=$PAGE_SIZE&o=$offset")
            val results = (page["Results"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            games += results
            val total = (page["Total"] as? Double)?.toInt() ?: 0
            offset += PAGE_SIZE
            if (results.isEmpty() || offset >= total) return games
        }
    }

    fun getGameProgress(gameId: Int): RetroGameProgress? {
        val game = get("API_GetGameInfoAndUserProgress.php", "&g=$gameId")
        val title = game["Title"] as? String ?: return null
        val achievements =
            (game["Achievements"] as? Map<*, *>)
                ?.values
                ?.filterIsInstance<Map<*, *>>()
                ?.sortedBy { (it["DisplayOrder"] as? Double) ?: 0.0 }
                ?.map { a ->
                    RetroAchievement(
                        name = a["Title"] as? String ?: "",
                        description = a["Description"] as? String,
                        badgeUrl = (a["BadgeName"] as? String)?.let { "https://media.retroachievements.org/Badge/$it.png" },
                        achieved = a["DateEarned"] != null || a["DateEarnedHardcore"] != null,
                    )
                } ?: emptyList()
        return RetroGameProgress(gameId = gameId, title = title, achievements = achievements)
    }

    companion object {
        private const val PROVIDER = "RetroAchievements"
        private const val PAGE_SIZE = 500

        /** RetroAchievements console ids for the consoles Oribu tracks that RA supports. */
        private val consoleIds =
            mapOf(
                GameConsole.NES to 7,
                GameConsole.SNES to 3,
                GameConsole.N64 to 2,
                GameConsole.GBA to 5,
                GameConsole.DS to 18,
                GameConsole.GCN to 16,
                GameConsole.PS1 to 12,
                GameConsole.PS2 to 21,
                GameConsole.PSP to 41,
            )

        fun supports(console: GameConsole?): Boolean = console in consoleIds

        /**
         * RA lists titles as "Legend of Zelda, The: A Link to the Past" — drop articles and
         * punctuation so it matches "The Legend of Zelda: A Link to the Past". "~Hack~"-style
         * tags are kept on purpose so a hack never matches the original game.
         */
        internal fun normalizeTitle(title: String): String =
            title
                .lowercase()
                .replace(Regex("\\bthe\\b"), "")
                .filter { it.isLetterOrDigit() }
    }
}
