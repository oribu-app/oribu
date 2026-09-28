package app.oribu.service.sync

import android.content.Context
import app.oribu.data.backup.BackupErrorReason
import app.oribu.data.backup.BackupException
import app.oribu.data.backup.BackupKind
import app.oribu.data.backup.BackupService
import app.oribu.data.db.DB
import app.oribu.service.MediaCacheService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(
    val itemsSynced: Int,
    /** False when no backup folder is set — the sync still ran, but without a safety snapshot. */
    val backupTaken: Boolean,
)

/**
 * Manual "Sync now" for one integration. Takes a pre-sync backup first (when a backup folder is
 * set), then refreshes every library item the integration covers, forcing it on even if its
 * automatic sync is off. Merging follows [SyncMerge]: nothing local is ever moved back.
 */
object SyncService {
    suspend fun syncNow(
        context: Context,
        integration: TrackingIntegration,
    ): SyncResult =
        withContext(Dispatchers.IO) {
            val backupTaken =
                try {
                    BackupService.backupToFolder(context, BackupKind.PRE_SYNC)
                    true
                } catch (e: BackupException) {
                    // Without a folder there's nowhere to write; any other failure must stop the sync.
                    if (e.reason != BackupErrorReason.NO_FOLDER) throw e
                    false
                }
            val items = DB.repo.getAll().filter { integration.covers(it) }
            items.forEach { MediaCacheService.fetchAndPersist(it, forceSync = setOf(integration)) }
            TrackingPreferences.markSynced(integration, System.currentTimeMillis())
            SyncResult(itemsSynced = items.size, backupTaken = backupTaken)
        }
}
