package com.topsort.analytics

import androidx.annotation.VisibleForTesting

/**
 * The resolved bids this process has already reported a render for.
 *
 * A resolved bid identifies one ad placed in one slot, so it earns exactly one render - the same
 * "one bid, one report" rule [ReportedBids] enforces for impressions. Tracked separately, in its
 * own set: a render fires as soon as the ad enters the view, well before the impression fires once
 * it is actually on screen, and the two must never consume the same tracked-bid slot. Sharing one
 * set would mean whichever of the two fires first marks the bid reported, silently dropping the
 * other - most likely the impression, which is the one billed on a CPM campaign.
 *
 * Renders have no organic counterpart, so unlike impressions there is no carve-out here for a bid
 * that does not exist.
 *
 * The set is in-memory and bounded exactly as [ReportedBids]'s is - see there for why a process
 * restart and the [MAX_TRACKED_BIDS] eviction are both acceptable trade-offs.
 */
internal object ReportedRenderBids {

    @VisibleForTesting
    internal const val MAX_TRACKED_BIDS = 512

    /** LinkedHashMap's own default; named only because detekt rejects the literal. */
    private const val LOAD_FACTOR = 0.75f

    // See ReportedBids for why this is a LinkedHashMap-as-LRU guarded by its own monitor.
    private val seen = object : LinkedHashMap<String, Unit>(MAX_TRACKED_BIDS, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean =
            size > MAX_TRACKED_BIDS
    }

    /**
     * Records [resolvedBidId] as reported and returns whether this was the first time.
     *
     * A false return means the caller has already reported a render for this bid and it should be
     * dropped.
     */
    fun markReported(resolvedBidId: String): Boolean = synchronized(seen) {
        seen.put(resolvedBidId, Unit) == null
    }

    /**
     * Forgets every tracked bid.
     *
     * Called from [Analytics.setup] alongside [ReportedBids.clear], for the same reason: a new
     * session may be a different user, and that user's renders are their own.
     */
    fun clear(): Unit = synchronized(seen) {
        seen.clear()
    }
}
