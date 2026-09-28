package app.oribu.service

import app.oribu.model.ApiSearchResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Kitsu — open anime database (JSON:API, no authentication). Fallback anime source after
 * AniList, same role MangaDex plays for manga; Tonkatsu Box uses the same pair.
 */
class KitsuService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val base = "https://kitsu.app/api/edge"

    private fun get(url: String): Map<String, Any?> {
        val req =
            Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.api+json")
                .build()
        val response = client.newCall(req).execute()
        val code = response.code
        val resp = response.use { it.body?.string() ?: "{}" }
        when (code) {
            404 -> throw ApiException(PROVIDER, ApiErrorReason.NOT_FOUND)
            429 -> throw ApiException(PROVIDER, ApiErrorReason.RATE_LIMITED)
            !in 200..299 -> throw ApiException(PROVIDER, ApiErrorReason.HTTP_ERROR, code)
        }
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(resp, type) ?: emptyMap()
    }

    fun searchAnime(query: String): List<ApiSearchResult> {
        if (query.isBlank()) return emptyList()
        val enc = URLEncoder.encode(query.trim(), "UTF-8")
        return (get("$base/anime?filter%5Btext%5D=$enc&page%5Blimit%5D=10")["data"] as? List<*>)
            ?.filterIsInstance<Map<String, Any?>>()
            ?.mapNotNull { mapItem(it) } ?: emptyList()
    }

    /** Same keys as AniListService.getAnimeDetailsById, already normalized for the detail screen. */
    fun getAnimeDetailsById(id: String): Map<String, Any?>? {
        val json = get("$base/anime/$id?include=categories")
        val attrs = (json["data"] as? Map<*, *>)?.get("attributes") as? Map<*, *> ?: return null
        val genres =
            (json["included"] as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.filter { it["type"] == "categories" }
                ?.mapNotNull { (it["attributes"] as? Map<*, *>)?.get("title") as? String }
        val synonyms =
            (
                (attrs["titles"] as? Map<*, *>)?.values?.filterIsInstance<String>().orEmpty() +
                    (attrs["abbreviatedTitles"] as? List<*>)?.filterIsInstance<String>().orEmpty()
            ).filter { it != attrs["canonicalTitle"] }
                .distinct()

        return buildMap {
            put("title", attrs["canonicalTitle"])
            put("coverUrl", posterUrl(attrs, "large"))
            put("synopsis", attrs["synopsis"])
            put("genres", genres?.ifEmpty { null })
            put("episodes", (attrs["episodeCount"] as? Double)?.toInt())
            put("episodeDurationMin", (attrs["episodeLength"] as? Double)?.toInt())
            put("serializationStatus", serializationStatus(attrs["status"] as? String))
            put("format", attrs["subtype"] as? String)
            put("synonyms", synonyms.ifEmpty { null })
            put("startDateMs", parseDate(attrs["startDate"] as? String))
            put("endDateMs", parseDate(attrs["endDate"] as? String))
        }
    }

    private fun mapItem(r: Map<String, Any?>): ApiSearchResult? {
        val id = r["id"] as? String ?: return null
        val attrs = r["attributes"] as? Map<*, *> ?: return null
        val title = attrs["canonicalTitle"] as? String ?: return null
        return ApiSearchResult(
            externalId = id,
            title = title,
            coverUrl = posterUrl(attrs, "medium"),
            synopsis = attrs["synopsis"] as? String,
            releaseDate = parseDate(attrs["startDate"] as? String)?.let { java.util.Date(it) },
            episodes = (attrs["episodeCount"] as? Double)?.toInt(),
            apiSource = "kitsu",
        )
    }

    private fun posterUrl(
        attrs: Map<*, *>,
        size: String,
    ): String? = (attrs["posterImage"] as? Map<*, *>)?.let { (it[size] ?: it["original"]) as? String }

    private fun parseDate(raw: String?): Long? =
        raw?.let { runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time }.getOrNull() }

    /** Same fixed labels AniList/MangaDex map to (they double as state keys in MediaCacheService). */
    private fun serializationStatus(status: String?): String? =
        when (status) {
            "current" -> "Ongoing"
            "finished" -> "Finished"
            "upcoming", "unreleased", "tba" -> "Coming Soon"
            else -> status
        }

    private companion object {
        const val PROVIDER = "Kitsu"
    }
}
