package app.oribu.service

import app.oribu.model.ApiSearchResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hardcover — book database with a GraphQL API. Needs the user's personal API token
 * (hardcover.app → Settings → API). Third-level fallback for book search, after Google Books
 * and Open Library.
 */
class HardcoverService(
    apiToken: String,
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val endpoint = "https://api.hardcover.app/v1/graphql"

    // The settings page shows the token already prefixed with "Bearer " — accept it either way.
    private val authorization = "Bearer ${apiToken.trim().removePrefix("Bearer ").trim()}"

    private fun query(
        query: String,
        variables: Map<String, Any?> = emptyMap(),
    ): Map<String, Any?> {
        val body = gson.toJson(mapOf("query" to query, "variables" to variables))
        val req =
            Request
                .Builder()
                .url(endpoint)
                .addHeader("Authorization", authorization)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
        val response = client.newCall(req).execute()
        val code = response.code
        val resp = response.use { it.body?.string() ?: "{}" }
        when (code) {
            401, 403 -> throw ApiException(PROVIDER, ApiErrorReason.UNAUTHORIZED)
            429 -> throw ApiException(PROVIDER, ApiErrorReason.RATE_LIMITED)
            !in 200..299 -> throw ApiException(PROVIDER, ApiErrorReason.HTTP_ERROR, code)
        }
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        val json: Map<String, Any?> = gson.fromJson(resp, type) ?: emptyMap()
        // Hasura answers auth failures with HTTP 200 + an "errors" array instead of a 401.
        val errors = (json["errors"] as? List<*>)?.filterIsInstance<Map<*, *>>()
        if (!errors.isNullOrEmpty()) {
            val invalidToken =
                errors.any {
                    val code = (it["extensions"] as? Map<*, *>)?.get("code") as? String
                    code == "invalid-jwt" || code == "access-denied" || code == "invalid-headers"
                }
            throw ApiException(PROVIDER, if (invalidToken) ApiErrorReason.UNAUTHORIZED else ApiErrorReason.HTTP_ERROR, code)
        }
        return json["data"] as? Map<String, Any?> ?: emptyMap()
    }

    fun testConnection() {
        if ((query("{ me { id } }")["me"] as? List<*>).isNullOrEmpty()) {
            throw ApiException(PROVIDER, ApiErrorReason.UNAUTHORIZED)
        }
    }

    fun searchBooks(query: String): List<ApiSearchResult> {
        if (query.isBlank()) return emptyList()
        val data =
            query(
                "query(\$q: String!) { search(query: \$q, query_type: \"Book\", per_page: 10, page: 1) { results } }",
                mapOf("q" to query.trim()),
            )
        val results = (data["search"] as? Map<*, *>)?.get("results") as? Map<*, *> ?: return emptyList()
        return (results["hits"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            ?.mapNotNull { (it["document"] as? Map<*, *>)?.let(::mapBook) } ?: emptyList()
    }

    fun getDetails(bookId: String): Map<String, Any?>? {
        val id = bookId.toIntOrNull() ?: return null
        val data =
            query(
                """
                query(${'$'}id: Int!) {
                  books_by_pk(id: ${'$'}id) {
                    title description pages release_date
                    image { url }
                    contributions { author { name } }
                    default_physical_edition { publisher { name } }
                  }
                }
                """.trimIndent(),
                mapOf("id" to id),
            )
        val book = data["books_by_pk"] as? Map<*, *> ?: return null
        val author =
            (book["contributions"] as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.firstNotNullOfOrNull { (it["author"] as? Map<*, *>)?.get("name") as? String }
        val publisher =
            ((book["default_physical_edition"] as? Map<*, *>)?.get("publisher") as? Map<*, *>)?.get("name") as? String
        val releaseDate =
            (book["release_date"] as? String)
                ?.let { runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it) }.getOrNull() }

        return buildMap {
            put("title", book["title"])
            put("synopsis", book["description"])
            put("coverUrl", (book["image"] as? Map<*, *>)?.get("url"))
            put("author", author)
            put("publisher", publisher)
            put("pages", (book["pages"] as? Double)?.toInt())
            put("releaseDate", releaseDate?.time)
        }
    }

    private fun mapBook(doc: Map<*, *>): ApiSearchResult? {
        val id = doc["id"]?.let { (it as? Double)?.toLong()?.toString() ?: it.toString() } ?: return null
        val title = doc["title"] as? String ?: return null
        val year = (doc["release_year"] as? Double)?.toInt()
        val isbns = (doc["isbns"] as? List<*>)?.filterIsInstance<String>()
        return ApiSearchResult(
            externalId = id,
            title = title,
            coverUrl = (doc["image"] as? Map<*, *>)?.get("url") as? String,
            synopsis = doc["description"] as? String,
            releaseDate = year?.let { runCatching { Date(it - 1900, 0, 1) }.getOrNull() },
            genre = (doc["genres"] as? List<*>)?.filterIsInstance<String>()?.firstOrNull(),
            authors = (doc["author_names"] as? List<*>)?.filterIsInstance<String>()?.ifEmpty { null },
            pages = (doc["pages"] as? Double)?.toInt(),
            isbn = isbns?.firstOrNull { it.length == 13 } ?: isbns?.firstOrNull(),
            apiSource = "hardcover",
        )
    }

    private companion object {
        const val PROVIDER = "Hardcover"
    }
}
