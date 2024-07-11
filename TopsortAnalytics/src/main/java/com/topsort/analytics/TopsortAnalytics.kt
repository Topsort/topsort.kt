package com.topsort.analytics

import com.topsort.analytics.model.events.Entity
import com.topsort.analytics.model.events.Placement
import com.topsort.analytics.model.events.PurchasedItem

interface TopsortAnalytics {

    /**
     * Reports a single promoted impression (with a resolvedBidId)
     *
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param placement Object describing the impression's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportImpressionPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
    )

    /**
     * Reports a single organic impression
     *
     * @param entity Refers to the object involved in the organic interaction
     * @param placement Object describing the impression's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportImpressionOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
    )

    /**
     * Reports a single promoted click (with a resolvedBidId)
     *
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param placement Object describing the click's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportClickPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
    )

    /**
     * Reports a single organic click
     *
     * @param entity Refers to the object involved in the organic interaction
     * @param placement Object describing the click's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     */
    fun reportClickOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
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
