package app.oribu.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.oribu.data.StoragePreferences
import app.oribu.data.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

/** Why a backup file was written — also its file-name prefix, used for retention. */
enum class BackupKind(
    val fileTag: String,
    val automatic: Boolean,
) {
    MANUAL("manual", automatic = false),
    SCHEDULED("auto", automatic = true),

    /** Safety snapshot taken right before an integration sync writes to the library. */
    PRE_SYNC("pre-sync", automatic = true),
}

/** What a backup file holds, shown before the user confirms a restore. */
data class BackupSummary(
    val createdAtMs: Long,
    val itemCount: Int,
    val includesSettings: Boolean,
    val includesSensitive: Boolean,
)

data class RestoreResult(
    val itemCount: Int,
    /** Preference stores were replaced — the app must restart to load them. */
    val settingsRestored: Boolean,
)

/**
 * Local backups in the folder picked in onboarding / Settings → Data, modelled on Rokku's
 * backups: the user picks what goes in (BackupOptions), automatic backups include everything but
 * sensitive data. Library tables are read/written row by row through SQLite (not the DAOs), so a
 * backup from an older schema still restores; preference stores are copied as whole DataStore
 * files. Caches (media details, the game dataset) are never included — "Update all" rebuilds them.
 */
object BackupService {
    /** Library table(s) behind each option; media_items is the one everything else points to. */
    private fun tablesFor(options: BackupOptions): List<String> =
        if (!options.library) {
            emptyList()
        } else {
            buildList {
                add(BackupPayload.MEDIA_ITEMS_TABLE)
                if (options.watchedEpisodes) add("serie_episodios_assistidos")
                if (options.reviewHistory) add("manga_reviews")
                if (options.bookQuotes) add("book_quotes")
                if (options.playthroughs) add("game_playthroughs")
                if (options.movieLists) addAll(listOf("filme_listas", "filme_lista_itens"))
            }
        }

    /**
     * DataStore files per option. Onboarding progress, the backup folder (a per-device permission)
     * and the update check timestamp are deliberately never restored onto another install.
     */
    private val SETTINGS_STORES =
        listOf("theme_prefs", "cover_theme_prefs", "locale_prefs", "platform_prefs", "series_scope_prefs", "tracking_prefs", "backup_prefs")
    private val SENSITIVE_STORES = listOf("api_key_prefs")

    private const val MEDIA_CACHE_TABLE = "media_details_cache"
    private const val MIME = "application/json"

    private val schemaVersion get() = DB.database.openHelper.readableDatabase.version

    private fun storeFile(
        context: Context,
        store: String,
    ) = File(context.filesDir, "datastore/$store.preferences_pb")

    suspend fun snapshot(
        context: Context,
        options: BackupOptions,
    ): BackupPayload =
        withContext(Dispatchers.IO) {
            val db = DB.database.openHelper.readableDatabase
            val tables =
                tablesFor(options).associateWith { table ->
                    db.query("SELECT * FROM $table").use { cursor -> cursor.readRows() }
                }
            val stores =
                (if (options.appSettings) SETTINGS_STORES else emptyList()) +
                    (if (options.sensitive) SENSITIVE_STORES else emptyList())
            val settings =
                stores
                    .mapNotNull { store ->
                        storeFile(
                            context,
                            store,
                        ).takeIf { it.exists() }?.let { store to Base64.getEncoder().encodeToString(it.readBytes()) }
                    }.toMap()
            BackupPayload(
                schemaVersion = db.version,
                createdAtMs = System.currentTimeMillis(),
                tables = tables,
                settings = settings,
                languageTags = if (options.appSettings) AppCompatDelegate.getApplicationLocales().toLanguageTags() else null,
            )
        }

    /** Writes a backup to the chosen folder; returns the file name. */
    suspend fun backupToFolder(
        context: Context,
        kind: BackupKind,
        options: BackupOptions = BackupOptions(),
    ): String =
        withContext(Dispatchers.IO) {
            val treeUri = StoragePreferences.folderUri.value?.let(Uri::parse) ?: throw BackupException(BackupErrorReason.NO_FOLDER)
            val payload = snapshot(context, options)
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(payload.createdAtMs))
            val name = "oribu-backup-${kind.fileTag}-$stamp.json"
            val resolver = context.contentResolver
            try {
                val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
                val file =
                    DocumentsContract.createDocument(resolver, parent, MIME, name) ?: throw BackupException(BackupErrorReason.WRITE_FAILED)
                resolver.openOutputStream(file)?.bufferedWriter()?.use { it.write(BackupCodec.encode(payload)) }
                    ?: throw BackupException(BackupErrorReason.WRITE_FAILED)
            } catch (e: BackupException) {
                throw e
            } catch (e: Exception) {
                throw BackupException(BackupErrorReason.WRITE_FAILED, e)
            }
            if (kind.automatic) pruneOld(context, treeUri, kind, keep = BackupPreferences.maxAutomatic.value)
            BackupPreferences.markBackup(payload.createdAtMs, automatic = kind.automatic)
            name
        }

    suspend fun inspect(
        context: Context,
        uri: Uri,
    ): BackupSummary =
        withContext(Dispatchers.IO) {
            val payload = read(context, uri)
            BackupSummary(
                createdAtMs = payload.createdAtMs,
                itemCount = payload.itemCount,
                includesSettings = payload.settings.keys.any { it in SETTINGS_STORES },
                includesSensitive = payload.settings.keys.any { it in SENSITIVE_STORES },
            )
        }

    /**
     * Replaces what the backup holds: each library table in it (tables left out of the backup stay
     * as they are) and each preference store in it. Settings only take effect after [restartApp].
     */
    suspend fun restore(
        context: Context,
        uri: Uri,
    ): RestoreResult =
        withContext(Dispatchers.IO) {
            val payload = read(context, uri)
            val db = DB.database.openHelper.writableDatabase
            val tables = payload.tables.keys.filter { it in tablesFor(BackupOptions()) }
            db.beginTransaction()
            try {
                if (BackupPayload.MEDIA_ITEMS_TABLE in tables) db.execSQL("DELETE FROM $MEDIA_CACHE_TABLE")
                tables.forEach { table ->
                    db.execSQL("DELETE FROM $table")
                    payload.tables[table].orEmpty().forEach { row ->
                        db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, row.toContentValues())
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            // Writes above bypass Room's own transactions, so tell observers (list screens) to reload.
            DB.database.invalidationTracker.refreshVersionsAsync()

            val stores = payload.settings.filterKeys { it in SETTINGS_STORES || it in SENSITIVE_STORES }
            stores.forEach { (store, data) ->
                storeFile(context, store).apply { parentFile?.mkdirs() }.writeBytes(Base64.getDecoder().decode(data))
            }
            payload.languageTags?.let { tags ->
                withContext(Dispatchers.Main) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
                }
            }
            RestoreResult(itemCount = payload.itemCount, settingsRestored = stores.isNotEmpty())
        }

    /**
     * DataStore keeps the stores it already loaded in memory, so restored preference files only
     * take effect on a fresh process — relaunch the app right away (nothing may write them before).
     */
    fun restartApp(context: Context) {
        val launch =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } ?: return
        context.startActivity(launch)
        Runtime.getRuntime().exit(0)
    }

    private fun read(
        context: Context,
        uri: Uri,
    ): BackupPayload {
        val json =
            try {
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            } catch (e: Exception) {
                throw BackupException(BackupErrorReason.INVALID_FILE, e)
            } ?: throw BackupException(BackupErrorReason.INVALID_FILE)
        return BackupCodec.decode(json, schemaVersion)
    }

    private fun pruneOld(
        context: Context,
        treeUri: Uri,
        kind: BackupKind,
        keep: Int,
    ) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val prefix = "oribu-backup-${kind.fileTag}-"
        val ours = mutableListOf<Pair<String, String>>()
        runCatching {
            resolver
                .query(
                    children,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1) ?: continue
                        if (name.startsWith(prefix)) ours += cursor.getString(0) to name
                    }
                }
        }
        // The timestamp in the name sorts chronologically, so the newest are last.
        ours.sortedBy { it.second }.dropLast(keep).forEach { (docId, _) ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)) }
        }
    }

    private fun Cursor.readRows(): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        while (moveToNext()) {
            rows +=
                (0 until columnCount).associate { i ->
                    getColumnName(i) to
                        when (getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> getString(i)
                            else -> null
                        }
                }
        }
        return rows
    }

    private fun Map<String, Any?>.toContentValues() =
        ContentValues().apply {
            forEach { (column, value) ->
                when (value) {
                    null -> putNull(column)
                    is Long -> put(column, value)
                    is Double -> put(column, value)
                    is Boolean -> put(column, if (value) 1 else 0)
                    else -> put(column, value.toString())
                }
            }
        }
}
