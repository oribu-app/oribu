package app.oribu.service

import app.oribu.model.ApiSearchResult

class MangaSearchService(
    private val anilist: AniListService,
    private val mangadex: MangaDexService,
    private val mangabaka: MangaBakaService,
) {
    fun search(query: String): List<ApiSearchResult> {
        val results = runCatching { anilist.search(query) }.getOrElse { emptyList() }
        if (results.isNotEmpty()) return results
        val mangaDexResults = runCatching { mangadex.search(query) }.getOrElse { emptyList() }
        if (mangaDexResults.isNotEmpty()) return mangaDexResults
        return runCatching { mangabaka.search(query) }.getOrElse { emptyList() }
    }
}
