package app.oribu.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamGridDbServiceTest {
    @Test
    fun `steamgriddb cdn urls are recognized as custom covers`() {
        assertTrue(SteamGridDbService.isSteamGridDbUrl("https://cdn2.steamgriddb.com/grid/abc123.png"))
        assertTrue(SteamGridDbService.isSteamGridDbUrl("https://steamgriddb.com/file/grid.png"))
    }

    @Test
    fun `other hosts and lookalikes are not custom covers`() {
        assertFalse(SteamGridDbService.isSteamGridDbUrl("https://images.igdb.com/igdb/image/upload/t_cover_big/co1.jpg"))
        assertFalse(SteamGridDbService.isSteamGridDbUrl("https://evilsteamgriddb.com/grid.png"))
        assertFalse(SteamGridDbService.isSteamGridDbUrl("not a url"))
        assertFalse(SteamGridDbService.isSteamGridDbUrl(null))
    }
}
