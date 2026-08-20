package app.oribu.service

import app.oribu.model.ApiSearchResult

class MangaSearchService(
    private val anilist: AniListService,
    private val mangadex: MangaDexService,
) {
    fun search(query: String): List<ApiSearchResult> {
        val results = runCatching { anilist.search(query) }.getOrElse { emptyList() }
        if (results.isNotEmpty()) return results
        return runCatching { mangadex.search(query) }.getOrElse { emptyList() }
    }
}
