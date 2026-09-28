package app.oribu.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WesternAnimationTest {
    private fun details(
        vararg genreIds: Int,
        language: String,
    ): Map<String, Any?> =
        mapOf(
            "genres" to genreIds.map { mapOf("id" to it.toDouble(), "name" to "whatever") },
            "original_language" to language,
        )

    @Test
    fun `animation genre in a non-japanese title is western animation`() {
        assertTrue(isWesternAnimation(details(16, 10751, language = "en")))
    }

    @Test
    fun `japanese animation is anime, not western animation`() {
        assertFalse(isWesternAnimation(details(16, language = "ja")))
    }

    @Test
    fun `live action is not animation`() {
        assertFalse(isWesternAnimation(details(18, 80, language = "en")))
        assertFalse(isWesternAnimation(emptyMap()))
    }
}
