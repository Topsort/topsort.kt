package com.topsort.analytics

import com.topsort.analytics.model.Channel
import com.topsort.analytics.model.ClickType
import com.topsort.analytics.model.auctions.Device
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.Page
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.PurchasedItem

interface TopsortAnalytics {

    /**
     * Reports a single promoted impression (with a resolvedBidId)
     *
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param placement Object describing the impression's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the impression occurred
     */
    fun reportImpressionPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
    )

    /**
     * Reports a single organic impression
     *
     * @param entity Refers to the object involved in the organic interaction
     * @param placement Object describing the impression's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the impression occurred
     */
    fun reportImpressionOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
    )

    /**
     * Reports a single promoted click (with a resolvedBidId)
     *
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param placement Object describing the click's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the click
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the click occurred
     * @param clickType Optional click type
     */
    fun reportClickPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
        clickType: ClickType? = null,
    )

    /**
     * Reports a single organic click
     *
     * @param entity Refers to the object involved in the organic interaction
     * @param placement Object describing the click's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the click
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the click occurred
     * @param clickType Optional click type
     */
    fun reportClickOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
        clickType: ClickType? = null,
    )

    /**
     * Reports a purchase event.
     *
     * @param items the list of purchased items
     * @param id The marketplace assigned ID for the order
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the purchase occurred
     */
    fun reportPurchase(
        items: List<PurchasedItem>,
        id: String,
        opaqueUserId: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
    )

    /**
     * Reports a page view event.
     *
     * @param page The page being viewed
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for this page view event
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     */
    fun reportPageView(
        page: Page,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
    ) {
        // Default empty implementation to maintain backward compatibility
        // for existing TopsortAnalytics interface implementors.
    }
}
