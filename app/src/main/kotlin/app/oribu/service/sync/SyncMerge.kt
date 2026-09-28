package app.oribu.service.sync

import app.oribu.model.MediaStatus
import java.util.Date

/**
 * How data coming from an integration (import or automatic sync) merges into the local library.
 * The local library is the source of truth — same rule as Tonkatsu Box's imports: a remote value
 * only ever moves an item forward, never back, and never overwrites something the user set.
 * Each function returns the value to store, or null when the local one should stay.
 */
object SyncMerge {
    /** Progress (chapters/episodes) only advances. */
    fun mergeProgress(
        local: Int?,
        remote: Int,
    ): Int? = if (remote > (local ?: 0)) remote else null

    /** Status only moves forward (queued → in progress → finished → 100%); never downgrades. */
    fun mergeStatus(
        local: MediaStatus,
        remote: MediaStatus,
    ): MediaStatus? = if (rank(remote) > rank(local)) remote else null

    /** A rating the user already gave is never replaced. */
    fun mergeRating(
        local: Double?,
        remote: Double?,
    ): Double? = if (local == null && remote != null) remote else null

    /** A date the user already has (start, completion) is never replaced. */
    fun mergeDate(
        local: Date?,
        remote: Date?,
    ): Date? = if (local == null && remote != null) remote else null

    private fun rank(status: MediaStatus): Int =
        when (status) {
            MediaStatus.QUEUED, MediaStatus.WAITING_RELEASE -> 0

            MediaStatus.PLAYING, MediaStatus.WATCHING, MediaStatus.READING,
            MediaStatus.REPLAYING, MediaStatus.REWATCHING, MediaStatus.REREADING,
            MediaStatus.ON_HOLD, MediaStatus.DROPPED, MediaStatus.WAITING_EPISODES,
            -> 1

            MediaStatus.FINISHED, MediaStatus.WATCHED, MediaStatus.READ,
            MediaStatus.HISTORY, MediaStatus.CONCLUDED,
            -> 2

            MediaStatus.COMPLETED, MediaStatus.PLATINUM -> 3
        }
}
