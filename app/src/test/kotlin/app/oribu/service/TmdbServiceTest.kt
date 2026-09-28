package app.oribu.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbServiceTest {
    private val v3Key = "0123456789abcdef0123456789ABCDEF"
    private val v4Token = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJ4In0.c2lnbmF0dXJl"

    @Test
    fun `stray whitespace, newlines and Bearer prefix are stripped`() {
        assertEquals(v4Token, TmdbService.normalizeCredential("  Bearer $v4Token\n"))
        assertEquals(v3Key, TmdbService.normalizeCredential("$v3Key\r\n"))
    }

    @Test
    fun `short hex API key is detected as v3`() {
        assertTrue(TmdbService.isV3ApiKey(v3Key))
    }

    @Test
    fun `read access token is not treated as v3 key`() {
        assertFalse(TmdbService.isV3ApiKey(v4Token))
        assertFalse(TmdbService.isV3ApiKey(""))
    }
}
