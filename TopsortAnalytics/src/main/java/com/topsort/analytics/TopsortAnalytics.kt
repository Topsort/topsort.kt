package com.topsort.analytics

import com.topsort.analytics.model.Channel
import com.topsort.analytics.model.ClickType
import com.topsort.analytics.model.auctions.Device
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.Page
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.PurchasedItem
import com.topsort.analytics.model.VideoCheckpoint

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

    /**
     * Reports a render event: a sponsored ad was inserted into the page, separately from an
     * impression, which records when the ad actually becomes visible. Renders are sponsored ads
     * only, so unlike impressions and clicks there is no organic variant.
     *
     * @param resolvedBidId Required. Must be the ID for the auction the ad won
     * @param placement Object describing the render's placement
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the render
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the render occurred
     */
    fun reportRender(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
    ) {
        // Default empty implementation to maintain backward compatibility
        // for existing TopsortAnalytics interface implementors.
    }

    /**
     * Reports that a sponsored video ad reached a playback checkpoint. Each checkpoint is reported
     * once per user and resolved bid; repeats are dropped with a warning.
     *
     * Video events are for video ads only: the API rejects a resolvedBidId from any other ad. They
     * are used for engagement and delivery reporting and are never charged or attributed.
     *
     * @param resolvedBidId Required. Must be the ID for the auction the video ad won
     * @param checkpoint The VAST playback checkpoint reached
     * @param placement Optional object describing where the video played
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the video event
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the video played
     */
    fun reportVideoCheckpoint(
        resolvedBidId: String,
        checkpoint: VideoCheckpoint,
        placement: Placement? = null,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
    ) {
        // Default empty implementation to maintain backward compatibility
        // for existing TopsortAnalytics interface implementors.
    }

    /**
     * Reports that a sponsored video ad could not be delivered or played. Unlike checkpoints,
     * errors are never dropped as repeats.
     *
     * @param resolvedBidId Required. Must be the ID for the auction the video ad won
     * @param errorCode The VAST error code describing why the video failed, for example `405` for
     * a media file that could not be displayed
     * @param placement Optional object describing where the video played
     * @param opaqueUserId The opaque user ID which allows correlating user activity.
     * @param id The marketplace's unique ID for the video event
     * @param occurredAt RFC3339 formatted timestamp including UTC offset. Defaults to DateTime() when null
     * @param deviceType Optional device type
     * @param channel Optional channel
     * @param page Optional page context where the video played
     */
    fun reportVideoError(
        resolvedBidId: String,
        errorCode: Int,
        placement: Placement? = null,
        opaqueUserId: String? = null,
        id: String? = null,
        occurredAt: String? = null,
        deviceType: Device? = null,
        channel: Channel? = null,
        page: Page? = null,
    ) {
        // Default empty implementation to maintain backward compatibility
        // for existing TopsortAnalytics interface implementors.
    }
}
