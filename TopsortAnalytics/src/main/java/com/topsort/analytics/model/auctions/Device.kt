package com.topsort.analytics.model.auctions

/**
 * Device type for auctions and analytics events.
 *
 * Used both in auction requests ([AuctionRequest]) and analytics events
 * (impressions, clicks, purchases) to indicate the device where the
 * action occurred.
 */
enum class Device(val value: String) {
    DESKTOP("desktop"),
    MOBILE("mobile");

    companion object {
        /**
         * Parse a device from its string value.
         * Returns null for unrecognized values (graceful degradation).
         */
        fun fromValue(value: String?): Device? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}
