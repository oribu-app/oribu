package app.oribu.service

import app.oribu.model.ApiSearchResult

class AnimeSearchService(
    private val anilist: AniListService,
    private val kitsu: KitsuService,
) {
    fun search(query: String): List<ApiSearchResult> {
        val results = runCatching { anilist.searchAnime(query) }.getOrElse { emptyList() }
        if (results.isNotEmpty()) return results
        return runCatching { kitsu.searchAnime(query) }.getOrElse { emptyList() }
    }
}
