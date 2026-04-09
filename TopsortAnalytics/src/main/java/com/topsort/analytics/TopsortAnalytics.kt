package com.topsort.analytics

import com.topsort.analytics.model.Click
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
     * @param deviceType Optional device type. Use [Page.DEVICE_TYPE_DESKTOP] or [Page.DEVICE_TYPE_MOBILE]
     * @param channel Optional channel. Use [Page.CHANNEL_ONSITE], [Page.CHANNEL_OFFSITE], or [Page.CHANNEL_INSTORE]
     * @param page Optional page context where the impression occurred
     */
    @JvmOverloads
    fun reportImpressionPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: String? = null,
        channel: String? = null,
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
     * @param deviceType Optional device type. Use [Page.DEVICE_TYPE_DESKTOP] or [Page.DEVICE_TYPE_MOBILE]
     * @param channel Optional channel. Use [Page.CHANNEL_ONSITE], [Page.CHANNEL_OFFSITE], or [Page.CHANNEL_INSTORE]
     * @param page Optional page context where the impression occurred
     */
    @JvmOverloads
    fun reportImpressionOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: String? = null,
        channel: String? = null,
        page: Page? = null,
    )

    /**
     * Reports a single promoted click (with a resolvedBidId)
     *
     * @param resolvedBidId Required for promoted products. Must be the ID for the auction the product won
     * @param placement Object describing the click's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type. Use [Page.DEVICE_TYPE_DESKTOP] or [Page.DEVICE_TYPE_MOBILE]
     * @param channel Optional channel. Use [Page.CHANNEL_ONSITE], [Page.CHANNEL_OFFSITE], or [Page.CHANNEL_INSTORE]
     * @param page Optional page context where the click occurred
     * @param clickType Optional click type. Use [Click.CLICK_TYPE_PRODUCT], [Click.CLICK_TYPE_LIKE], or [Click.CLICK_TYPE_ADD_TO_CART]
     */
    @JvmOverloads
    fun reportClickPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: String? = null,
        channel: String? = null,
        page: Page? = null,
        clickType: String? = null,
    )

    /**
     * Reports a single organic click
     *
     * @param entity Refers to the object involved in the organic interaction
     * @param placement Object describing the click's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the impression
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type. Use [Page.DEVICE_TYPE_DESKTOP] or [Page.DEVICE_TYPE_MOBILE]
     * @param channel Optional channel. Use [Page.CHANNEL_ONSITE], [Page.CHANNEL_OFFSITE], or [Page.CHANNEL_INSTORE]
     * @param page Optional page context where the click occurred
     * @param clickType Optional click type. Use [Click.CLICK_TYPE_PRODUCT], [Click.CLICK_TYPE_LIKE], or [Click.CLICK_TYPE_ADD_TO_CART]
     */
    @JvmOverloads
    fun reportClickOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: String? = null,
        channel: String? = null,
        page: Page? = null,
        clickType: String? = null,
    )

    /**
     * Reports a purchase event.
     *
     * @param items the list of purchased items
     * @param id The marketplace assigned ID for the order
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type. Use [Page.DEVICE_TYPE_DESKTOP] or [Page.DEVICE_TYPE_MOBILE]
     * @param channel Optional channel. Use [Page.CHANNEL_ONSITE], [Page.CHANNEL_OFFSITE], or [Page.CHANNEL_INSTORE]
     */
    @JvmOverloads
    fun reportPurchase(
        items: List<PurchasedItem>,
        id: String,
        opaqueUserId: String? = null,
        occurredAt: String? = null,
        deviceType: String? = null,
        channel: String? = null,
    )

    /**
     * Reports a page view event.
     *
     * @param page The page being viewed
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for this page view event
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type ("desktop" or "mobile")
     * @param channel Optional channel ("onsite", "offsite", or "instore")
     */
    fun reportPageView(
        page: Page,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: String? = null,
        channel: String? = null,
    ) {
        // Default empty implementation to maintain backward compatibility
        // for existing TopsortAnalytics interface implementors.
    }
}
