package app.oribu.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val payload =
        BackupPayload(
            schemaVersion = 16,
            createdAtMs = 1_759_000_000_000,
            tables =
                mapOf(
                    "media_items" to
                        listOf(
                            mapOf(
                                "id" to 7L,
                                "titulo" to "Frieren",
                                "nota" to 4.5,
                                "data_adicao_ms" to 1_759_000_000_123L,
                                "capa_url" to null,
                            ),
                        ),
                    "book_quotes" to emptyList(),
                ),
        )

    @Test
    fun `round trip keeps rows and whole numbers as Long`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(payload), currentSchemaVersion = 16)
        val row = decoded.tables.getValue("media_items").single()
        assertEquals(7L, row["id"])
        assertEquals(1_759_000_000_123L, row["data_adicao_ms"])
        assertEquals(4.5, row["nota"])
        assertEquals(null, row["capa_url"])
        assertEquals(1, decoded.itemCount)
        assertEquals(16, decoded.schemaVersion)
    }

    @Test
    fun `settings stores and language survive the round trip`() {
        val withSettings = payload.copy(settings = mapOf("theme_prefs" to "AAEC"), languageTags = "pt-BR")
        val decoded = BackupCodec.decode(BackupCodec.encode(withSettings), currentSchemaVersion = 16)
        assertEquals(mapOf("theme_prefs" to "AAEC"), decoded.settings)
        assertEquals("pt-BR", decoded.languageTags)
    }

    @Test
    fun `older schema backups restore on a newer app`() {
        val old = BackupCodec.encode(payload.copy(schemaVersion = 12))
        assertEquals(12, BackupCodec.decode(old, currentSchemaVersion = 16).schemaVersion)
    }

    @Test
    fun `backups from a newer schema are refused`() {
        val newer = BackupCodec.encode(payload.copy(schemaVersion = 17))
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode(newer, currentSchemaVersion = 16) }
        assertEquals(BackupErrorReason.NEWER_SCHEMA, e.reason)
    }

    @Test
    fun `files that are not oribu backups are refused`() {
        listOf("""{"hello":"world"}""", "not json at all", """{"app":"other","format":1,"schemaVersion":1,"tables":{}}""").forEach {
            val e = assertThrows(BackupException::class.java) { BackupCodec.decode(it, currentSchemaVersion = 16) }
            assertEquals(BackupErrorReason.INVALID_FILE, e.reason)
        }
    }
}
