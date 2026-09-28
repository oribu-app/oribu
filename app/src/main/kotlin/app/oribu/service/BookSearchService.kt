package app.oribu.service

import app.oribu.model.ApiSearchResult

class BookSearchService(
    private val googleBooks: GoogleBooksService,
    private val openLibrary: OpenLibraryService,
    private val hardcover: HardcoverService?,
) {
    fun searchBooks(query: String): List<ApiSearchResult> {
        val results = runCatching { googleBooks.searchBooks(query) }.getOrElse { emptyList() }
        if (results.isNotEmpty()) return results
        val openLibraryResults = runCatching { openLibrary.searchBooks(query) }.getOrElse { emptyList() }
        if (openLibraryResults.isNotEmpty()) return openLibraryResults
        return searchHardcover(query)
    }

    fun searchByAuthor(author: String): List<ApiSearchResult> {
        val results = runCatching { googleBooks.searchByAuthor(author) }.getOrElse { emptyList() }
        if (results.isNotEmpty()) return results
        val openLibraryResults = runCatching { openLibrary.searchBooks(author) }.getOrElse { emptyList() }
        if (openLibraryResults.isNotEmpty()) return openLibraryResults
        return searchHardcover(author)
    }

    fun searchByPublisher(publisher: String): List<ApiSearchResult> =
        runCatching { googleBooks.searchByPublisher(publisher) }.getOrElse { emptyList() }

    /** Optional last resort — only runs when the user configured a Hardcover token. */
    private fun searchHardcover(query: String): List<ApiSearchResult> =
        hardcover?.let { runCatching { it.searchBooks(query) }.getOrElse { emptyList() } } ?: emptyList()
}
