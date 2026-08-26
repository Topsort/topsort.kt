package com.topsort.analytics

import androidx.annotation.VisibleForTesting

/**
 * The resolved bids this process has already reported an impression for.
 *
 * A resolved bid identifies one ad placed in one slot, so it earns exactly one impression. The
 * SDK cannot see why a caller reports the same bid twice - a recomposition, a recycled view, a
 * retried setup - and it does not need to: the second report is wrong whatever produced it, and
 * on a CPM campaign it is billed.
 *
 * Only promoted impressions are deduplicated. Organic impressions carry no bid, are not billable,
 * and legitimately repeat as the same entity appears on other screens. Clicks are left alone too:
 * a user really can click the same banner twice, and dropping the second one would lose intent
 * the marketplace is entitled to.
 *
 * The set is in-memory and bounded. It is not persisted - a process restart that re-reports one
 * impression is not the failure this guards against - and the oldest entries are evicted past
 * [MAX_TRACKED_BIDS] so a long session cannot grow it without limit. A bid evicted and then
 * reported again is allowed through, which is the right trade: the cap is far above the number of
 * distinct bids a real session sees, so reaching it at all means something is already looping.
 */
internal object ReportedBids {

    @VisibleForTesting
    internal const val MAX_TRACKED_BIDS = 512

    /** LinkedHashMap's own default; named only because detekt rejects the literal. */
    private const val LOAD_FACTOR = 0.75f

    // LinkedHashMap in access order with removeEldestEntry is the LRU; the value is unused.
    // Guarded by its own monitor rather than a concurrent map because eviction has to happen
    // under the same lock as the insert that triggers it.
    private val seen = object : LinkedHashMap<String, Unit>(MAX_TRACKED_BIDS, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean =
            size > MAX_TRACKED_BIDS
    }

    /**
     * Records [resolvedBidId] as reported and returns whether this was the first time.
     *
     * A false return means the caller has already reported this bid and the impression should be
     * dropped.
     */
    fun markReported(resolvedBidId: String): Boolean = synchronized(seen) {
        seen.put(resolvedBidId, Unit) == null
    }

    /**
     * Forgets every tracked bid.
     *
     * Called from [Analytics.setup], because a new session may be a different user, and that
     * user's impressions must not be dropped as duplicates of the previous one's.
     */
    fun clear(): Unit = synchronized(seen) {
        seen.clear()
    }
}
