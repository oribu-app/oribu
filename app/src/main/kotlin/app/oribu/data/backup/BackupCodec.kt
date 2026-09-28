package app.oribu.data.backup

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/** Why a backup file can't be restored — resolved to a localized message by the UI. */
enum class BackupErrorReason { INVALID_FILE, NEWER_SCHEMA, NO_FOLDER, WRITE_FAILED }

class BackupException(
    val reason: BackupErrorReason,
    cause: Throwable? = null,
) : Exception(reason.name, cause)

/**
 * Library backup file: every row of the backed-up tables as column → value maps, plus the Room
 * schema version it was taken from. Kept free of Android types so it can be unit-tested.
 */
data class BackupPayload(
    val app: String = APP_ID,
    val format: Int = BackupCodec.FORMAT,
    val schemaVersion: Int,
    val createdAtMs: Long,
    val tables: Map<String, List<Map<String, Any?>>>,
) {
    val itemCount: Int get() = tables[MEDIA_ITEMS_TABLE]?.size ?: 0

    companion object {
        const val APP_ID = "oribu"
        const val MEDIA_ITEMS_TABLE = "media_items"
    }
}

object BackupCodec {
    const val FORMAT = 1

    private val gson = Gson()

    fun encode(payload: BackupPayload): String = gson.toJson(payload)

    /**
     * Parses a backup and refuses what can't be restored safely: other apps' files, unknown
     * formats and backups from a newer schema (their columns may not exist here yet). Older
     * schemas are fine — missing columns just take their defaults on insert.
     */
    fun decode(
        json: String,
        currentSchemaVersion: Int,
    ): BackupPayload {
        val raw: Map<String, Any?> =
            try {
                gson.fromJson(json, object : TypeToken<Map<String, Any?>>() {}.type)
            } catch (e: JsonParseException) {
                throw BackupException(BackupErrorReason.INVALID_FILE, e)
            } ?: throw BackupException(BackupErrorReason.INVALID_FILE)

        if (raw["app"] != BackupPayload.APP_ID || (raw["format"] as? Double)?.toInt() != FORMAT) {
            throw BackupException(BackupErrorReason.INVALID_FILE)
        }
        val schemaVersion = (raw["schemaVersion"] as? Double)?.toInt() ?: throw BackupException(BackupErrorReason.INVALID_FILE)
        if (schemaVersion > currentSchemaVersion) throw BackupException(BackupErrorReason.NEWER_SCHEMA)

        val tables =
            (raw["tables"] as? Map<*, *>)
                ?.entries
                ?.associate { (name, rows) ->
                    name.toString() to
                        (rows as? List<*>)
                            ?.filterIsInstance<Map<*, *>>()
                            ?.map { row -> row.entries.associate { (col, value) -> col.toString() to normalize(value) } }
                            .orEmpty()
                } ?: throw BackupException(BackupErrorReason.INVALID_FILE)

        return BackupPayload(
            schemaVersion = schemaVersion,
            createdAtMs = (raw["createdAtMs"] as? Double)?.toLong() ?: 0L,
            tables = tables,
        )
    }

    /** JSON has no integer type: whole numbers come back as Long so INTEGER columns keep them intact. */
    internal fun normalize(value: Any?): Any? =
        when (value) {
            is Double -> if (value % 1.0 == 0.0 && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) value.toLong() else value
            else -> value
        }
}
