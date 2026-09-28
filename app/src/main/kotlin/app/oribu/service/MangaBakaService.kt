package app.oribu.service

import app.oribu.model.ApiSearchResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MangaBaka — open manga/manhwa/manhua database aggregating several sources. No authentication.
 * Third-level fallback for manga search, after AniList and MangaDex.
 */
class MangaBakaService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val base = "https://api.mangabaka.org/v1"

    private fun get(url: String): Map<String, Any?> {
        val req =
            Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/json")
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

    fun search(query: String): List<ApiSearchResult> {
        if (query.isBlank()) return emptyList()
        val enc = URLEncoder.encode(query.trim(), "UTF-8")
        return (get("$base/series/search?q=$enc&limit=10")["data"] as? List<*>)
            ?.filterIsInstance<Map<String, Any?>>()
            ?.filter { it["content_rating"] != "pornographic" }
            ?.mapNotNull { mapItem(it) } ?: emptyList()
    }

    fun getDetailsById(id: String): Map<String, Any?>? {
        val series = get("$base/series/$id")["data"] as? Map<*, *> ?: return null
        val startDateMs =
            ((series["published"] as? Map<*, *>)?.get("start_date") as? String)
                ?.let { runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time }.getOrNull() }
        val authors =
            listOf("authors", "artists")
                .flatMap { key -> (series[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList() }
                .distinct()
        val synonyms =
            listOfNotNull(series["native_title"] as? String, series["romanized_title"] as? String)
                .filter { it != series["title"] }
                .distinct()

        return buildMap {
            put("title", series["title"])
            put("coverUrl", coverUrl(series, "x350"))
            put("synopsis", series["description"])
            put("synonyms", synonyms.ifEmpty { null })
            put("genres", (series["genres"] as? List<*>)?.filterIsInstance<String>()?.map(::genreLabel))
            put("authors", authors.ifEmpty { null })
            put("serializationStatus", serializationStatus(series["status"] as? String))
            put("startDateMs", startDateMs)
            (series["total_chapters"] as? String)?.toIntOrNull()?.let { put("chapters", it) }
            (series["final_volume"] as? String)?.toIntOrNull()?.let { put("volumes", it) }
        }
    }

    private fun mapItem(r: Map<String, Any?>): ApiSearchResult? {
        val id = (r["id"] as? Double)?.toLong() ?: return null
        val title = r["title"] as? String ?: return null
        val year = (r["year"] as? Double)?.toInt()
        return ApiSearchResult(
            externalId = id.toString(),
            title = title,
            coverUrl = coverUrl(r, "x250"),
            synopsis = r["description"] as? String,
            releaseDate = year?.let { runCatching { Date(it - 1900, 0, 1) }.getOrNull() },
            genre = (r["genres"] as? List<*>)?.filterIsInstance<String>()?.firstOrNull()?.let(::genreLabel),
            authors = (r["authors"] as? List<*>)?.filterIsInstance<String>()?.ifEmpty { null },
            chapters = (r["total_chapters"] as? String)?.toIntOrNull(),
            volumes = (r["final_volume"] as? String)?.toIntOrNull(),
            apiSource = "mangabaka",
        )
    }

    /** Resized CDN variant (`x150`/`x250`/`x350`), falling back to the raw upload. */
    private fun coverUrl(
        r: Map<*, *>,
        size: String,
    ): String? {
        val cover = r["cover"] as? Map<*, *> ?: return null
        return ((cover[size] as? Map<*, *>)?.get("x1") as? String)
            ?: ((cover["raw"] as? Map<*, *>)?.get("url") as? String)
    }

    private fun serializationStatus(status: String?): String? =
        when (status) {
            "releasing" -> "Ongoing"
            "completed" -> "Finished"
            "hiatus" -> "Hiatus"
            "cancelled" -> "Cancelled"
            else -> status
        }

    private fun genreLabel(genre: String): String =
        genre.split('_').joinToString(" ") { part -> part.replaceFirstChar { it.titlecase(Locale.US) } }

    private companion object {
        const val PROVIDER = "MangaBaka"
    }
}
