package app.oribu.data.backup

import androidx.annotation.StringRes
import app.oribu.R
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
 * What goes into a backup — same idea as Rokku's BackupOptions. Everything but the library itself
 * hangs off library items, so those entries only apply when [library] is on. Sensitive data (API
 * keys and account credentials) is opt-in, like Rokku's "Include sensitive settings".
 */
data class BackupOptions(
    val library: Boolean = true,
    val watchedEpisodes: Boolean = true,
    val reviewHistory: Boolean = true,
    val bookQuotes: Boolean = true,
    val playthroughs: Boolean = true,
    val movieLists: Boolean = true,
    val appSettings: Boolean = true,
    val sensitive: Boolean = false,
) {
    data class Entry(
        @StringRes val label: Int,
        val getter: (BackupOptions) -> Boolean,
        val setter: (BackupOptions, Boolean) -> BackupOptions,
        val enabled: (BackupOptions) -> Boolean = { true },
    )

    companion object {
        val entries =
            listOf(
                Entry(R.string.backup_option_library, BackupOptions::library, { o, v -> o.copy(library = v) }),
                Entry(
                    R.string.backup_option_episodes,
                    BackupOptions::watchedEpisodes,
                    { o, v -> o.copy(watchedEpisodes = v) },
                    { it.library },
                ),
                Entry(
                    R.string.backup_option_review_history,
                    BackupOptions::reviewHistory,
                    { o, v -> o.copy(reviewHistory = v) },
                    { it.library },
                ),
                Entry(R.string.backup_option_book_quotes, BackupOptions::bookQuotes, { o, v -> o.copy(bookQuotes = v) }, { it.library }),
                Entry(
                    R.string.backup_option_playthroughs,
                    BackupOptions::playthroughs,
                    { o, v -> o.copy(playthroughs = v) },
                    { it.library },
                ),
                Entry(R.string.backup_option_movie_lists, BackupOptions::movieLists, { o, v -> o.copy(movieLists = v) }, { it.library }),
                Entry(R.string.backup_option_app_settings, BackupOptions::appSettings, { o, v -> o.copy(appSettings = v) }),
                Entry(R.string.backup_option_sensitive, BackupOptions::sensitive, { o, v -> o.copy(sensitive = v) }),
            )
    }
}

/**
 * Backup file: library table rows as column → value maps, preference stores as base64 file
 * contents, the app language (kept by AppCompat, not in any store) and the Room schema version the
 * rows came from. Kept free of Android types so it can be unit-tested.
 */
data class BackupPayload(
    val app: String = APP_ID,
    val format: Int = BackupCodec.FORMAT,
    val schemaVersion: Int,
    val createdAtMs: Long,
    val tables: Map<String, List<Map<String, Any?>>>,
    val settings: Map<String, String> = emptyMap(),
    val languageTags: String? = null,
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
        val settings =
            (raw["settings"] as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (store, data) -> (data as? String)?.let { store.toString() to it } }
                ?.toMap()
                .orEmpty()

        return BackupPayload(
            schemaVersion = schemaVersion,
            createdAtMs = (raw["createdAtMs"] as? Double)?.toLong() ?: 0L,
            tables = tables,
            settings = settings,
            languageTags = raw["languageTags"] as? String,
        )
    }

    /** JSON has no integer type: whole numbers come back as Long so INTEGER columns keep them intact. */
    internal fun normalize(value: Any?): Any? =
        when (value) {
            is Double -> if (value % 1.0 == 0.0 && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) value.toLong() else value
            else -> value
        }
}
