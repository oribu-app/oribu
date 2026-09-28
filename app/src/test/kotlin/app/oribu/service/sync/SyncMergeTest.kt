package app.oribu.service.sync

import app.oribu.model.MediaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class SyncMergeTest {
    @Test
    fun `progress only advances`() {
        assertEquals(15, SyncMerge.mergeProgress(local = 10, remote = 15))
        assertNull(SyncMerge.mergeProgress(local = 10, remote = 10))
        assertNull(SyncMerge.mergeProgress(local = 20, remote = 10))
        assertEquals(1, SyncMerge.mergeProgress(local = null, remote = 1))
    }

    @Test
    fun `status moves forward`() {
        assertEquals(MediaStatus.WATCHING, SyncMerge.mergeStatus(MediaStatus.QUEUED, MediaStatus.WATCHING))
        assertEquals(MediaStatus.READ, SyncMerge.mergeStatus(MediaStatus.READING, MediaStatus.READ))
        assertEquals(MediaStatus.PLATINUM, SyncMerge.mergeStatus(MediaStatus.FINISHED, MediaStatus.PLATINUM))
    }

    @Test
    fun `status never downgrades`() {
        assertNull(SyncMerge.mergeStatus(MediaStatus.WATCHED, MediaStatus.WATCHING))
        assertNull(SyncMerge.mergeStatus(MediaStatus.READING, MediaStatus.QUEUED))
        assertNull(SyncMerge.mergeStatus(MediaStatus.PLATINUM, MediaStatus.FINISHED))
        assertNull(SyncMerge.mergeStatus(MediaStatus.WATCHING, MediaStatus.WATCHING))
    }

    @Test
    fun `user ratings and dates are never replaced`() {
        assertNull(SyncMerge.mergeRating(local = 4.0, remote = 2.0))
        assertEquals(3.5, SyncMerge.mergeRating(local = null, remote = 3.5))
        val mine = Date(1_000)
        assertNull(SyncMerge.mergeDate(local = mine, remote = Date(2_000)))
        assertEquals(Date(2_000), SyncMerge.mergeDate(local = null, remote = Date(2_000)))
    }
}
