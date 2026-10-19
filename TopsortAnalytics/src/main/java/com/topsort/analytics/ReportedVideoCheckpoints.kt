package com.topsort.analytics

import androidx.annotation.VisibleForTesting
import com.topsort.analytics.model.VideoCheckpoint

/**
 * The video checkpoints this process has already reported, per user and resolved bid.
 *
 * VAST fires each checkpoint once per ad play, so a second `firstQuartile` for the same bid is a
 * player re-firing its tracker - a seek, a rebind, a recomposition - not new engagement, and it
 * would inflate completion rates. Errors are not tracked: a player that retries can fail several
 * times with different codes, and each failure is real delivery data.
 *
 * Keyed on the event's own opaqueUserId as well as the bid, so one user's report never drops
 * another's for the same bid, and nothing has to be cleared when [Analytics.setup] switches user.
 * Tracked apart from [ReportedBids] and [ReportedRenderBids] so a video event never uses up its
 * bid's impression or render.
 *
 * In-memory and bounded exactly as [ReportedBids] is - see there for why a process restart and the
 * eviction are both acceptable trade-offs. Video events are never charged, so a lost or repeated
 * checkpoint at those edges costs less still.
 */
internal object ReportedVideoCheckpoints {

    /** Every checkpoint of [ReportedBids.MAX_TRACKED_BIDS] bids. */
    @VisibleForTesting
    internal const val MAX_TRACKED_CHECKPOINTS = 2_560

    /** LinkedHashMap's own default; named only because detekt rejects the literal. */
    private const val LOAD_FACTOR = 0.75f

    private data class Key(
        val opaqueUserId: String,
        val resolvedBidId: String,
        val checkpoint: VideoCheckpoint,
    )

    // See ReportedBids for why this is a LinkedHashMap-as-LRU guarded by its own monitor.
    private val seen = object : LinkedHashMap<Key, Unit>(MAX_TRACKED_CHECKPOINTS, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Unit>?): Boolean =
            size > MAX_TRACKED_CHECKPOINTS
    }

    /**
     * Records the checkpoint as reported and returns whether this was the first time.
     *
     * A false return means this user already reported this checkpoint for this bid and the event
     * should be dropped.
     */
    fun markReported(
        opaqueUserId: String,
        resolvedBidId: String,
        checkpoint: VideoCheckpoint,
    ): Boolean = synchronized(seen) {
        seen.put(Key(opaqueUserId, resolvedBidId, checkpoint), Unit) == null
    }

    /** Forgets every tracked checkpoint. */
    @VisibleForTesting
    fun clear(): Unit = synchronized(seen) {
        seen.clear()
    }
}
