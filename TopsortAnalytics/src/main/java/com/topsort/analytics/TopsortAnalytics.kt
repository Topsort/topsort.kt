package com.topsort.analytics

import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.PurchasedItem

interface TopsortAnalytics {

    /**
     * Reports a single impression
     *
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportImpression(
        placement: Placement,
        opaqueUserId: String? = null,
        id: String,
        resolvedBidId: String? = null,
        occurredAt: String? = null,
    )

    /**
     * Reports a click event
     *
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the click
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportClick(
        placement: Placement,
        opaqueUserId: String? = null,
        id: String,
        resolvedBidId: String? = null,
        occurredAt: String? = null,
    )

    /**
     * Reports a purchase event.
     *  @param items the list of purchased items
     *  @param id The marketplace assigned ID for the order
     *  @param opaqueUserId The opaque user ID which allows correlating user activity.
     *  @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportPurchase(
        items: List<PurchasedItem>,
        id: String,
        opaqueUserId: String? = null,
        occurredAt: String? = null,
    )
}
