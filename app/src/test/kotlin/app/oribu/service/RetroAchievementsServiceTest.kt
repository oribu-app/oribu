package app.oribu.service

import app.oribu.model.GameConsole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroAchievementsServiceTest {
    @Test
    fun `trailing article in RA title matches leading article`() {
        assertEquals(
            RetroAchievementsService.normalizeTitle("The Legend of Zelda: A Link to the Past"),
            RetroAchievementsService.normalizeTitle("Legend of Zelda, The: A Link to the Past"),
        )
    }

    @Test
    fun `punctuation and case are ignored`() {
        assertEquals(
            RetroAchievementsService.normalizeTitle("Pokémon - Emerald Version"),
            RetroAchievementsService.normalizeTitle("pokémon emerald version"),
        )
    }

    @Test
    fun `hack never matches the original game`() {
        assertNotEquals(
            RetroAchievementsService.normalizeTitle("Super Mario World"),
            RetroAchievementsService.normalizeTitle("~Hack~ Super Mario World"),
        )
    }

    @Test
    fun `only retro consoles are supported`() {
        assertTrue(RetroAchievementsService.supports(GameConsole.SNES))
        assertTrue(RetroAchievementsService.supports(GameConsole.PS1))
        assertFalse(RetroAchievementsService.supports(GameConsole.STEAM))
        assertFalse(RetroAchievementsService.supports(GameConsole.PS5))
        assertFalse(RetroAchievementsService.supports(null))
    }
}
