package app.oribu.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import app.oribu.data.StoragePreferences
import app.oribu.data.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Why a backup file was written — also its file-name prefix, used for retention. */
enum class BackupKind(
    val fileTag: String,
) {
    MANUAL("manual"),
    SCHEDULED("auto"),

    /** Safety snapshot taken right before an integration import/sync writes to the library. */
    PRE_SYNC("pre-sync"),
}

/**
 * Local library backups in the folder picked in onboarding / Settings → Data. Reads and writes
 * the library tables row by row through SQLite (not the DAOs), so a backup taken on an older
 * schema still restores — missing columns take their defaults. Caches (media details, the game
 * dataset) are left out: "Update all" rebuilds them from the APIs after a restore.
 */
object BackupService {
    private val LIBRARY_TABLES =
        listOf(
            "media_items",
            "filme_listas",
            "filme_lista_itens",
            "serie_episodios_assistidos",
            "manga_reviews",
            "book_quotes",
            "game_playthroughs",
        )
    private const val MEDIA_CACHE_TABLE = "media_details_cache"
    private const val MIME = "application/json"

    /** Automatic snapshots kept per kind; manual backups are never deleted by the app. */
    private const val KEEP_AUTOMATIC = 5

    private val schemaVersion get() = DB.database.openHelper.readableDatabase.version

    suspend fun snapshot(): BackupPayload =
        withContext(Dispatchers.IO) {
            val db = DB.database.openHelper.readableDatabase
            val tables =
                LIBRARY_TABLES.associateWith { table ->
                    db.query("SELECT * FROM $table").use { cursor -> cursor.readRows() }
                }
            BackupPayload(schemaVersion = db.version, createdAtMs = System.currentTimeMillis(), tables = tables)
        }

    /** Writes a backup to the chosen folder; returns the file name. */
    suspend fun backupToFolder(
        context: Context,
        kind: BackupKind,
    ): String =
        withContext(Dispatchers.IO) {
            val treeUri = StoragePreferences.folderUri.value?.let(Uri::parse) ?: throw BackupException(BackupErrorReason.NO_FOLDER)
            val payload = snapshot()
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
            if (kind != BackupKind.MANUAL) pruneOld(context, treeUri, kind)
            BackupPreferences.markBackup(payload.createdAtMs)
            name
        }

    /** Replaces the whole library with the backup's rows; returns how many items were restored. */
    suspend fun restore(
        context: Context,
        uri: Uri,
    ): Int =
        withContext(Dispatchers.IO) {
            val json =
                try {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                } catch (e: Exception) {
                    throw BackupException(BackupErrorReason.INVALID_FILE, e)
                } ?: throw BackupException(BackupErrorReason.INVALID_FILE)
            val payload = BackupCodec.decode(json, schemaVersion)
            val db = DB.database.openHelper.writableDatabase
            db.beginTransaction()
            try {
                (LIBRARY_TABLES + MEDIA_CACHE_TABLE).forEach { db.execSQL("DELETE FROM $it") }
                LIBRARY_TABLES.forEach { table ->
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
            payload.itemCount
        }

    private fun pruneOld(
        context: Context,
        treeUri: Uri,
        kind: BackupKind,
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
        ours.sortedBy { it.second }.dropLast(KEEP_AUTOMATIC).forEach { (docId, _) ->
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
