package com.topsort.analytics.model

/**
 * Device type where an event occurred.
 *
 * Note: This enum mirrors [com.topsort.analytics.model.auctions.Device] but is
 * specific to event tracking. The auctions Device is used for auction requests,
 * while this is used for analytics events (impressions, clicks, purchases).
 */
enum class DeviceType(val value: String) {
    DESKTOP("desktop"),
    MOBILE("mobile");

    companion object {
        /**
         * Parse a device type from its string value.
         * Returns null for unrecognized values (graceful degradation).
         */
        fun fromValue(value: String?): DeviceType? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}

/**
 * Channel where an event occurred.
 */
enum class Channel(val value: String) {
    /** Event occurred on the marketplace's own website/app */
    ONSITE("onsite"),
    /** Event occurred on external platforms (social media, ads, etc.) */
    OFFSITE("offsite"),
    /** Event occurred in a physical store */
    INSTORE("instore");

    companion object {
        /**
         * Parse a channel from its string value.
         * Returns null for unrecognized values (graceful degradation).
         */
        fun fromValue(value: String?): Channel? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}

/**
 * Type of click action.
 */
enum class ClickType(val value: String) {
    /** Click on a product to view details */
    PRODUCT("product"),
    /** Like or favorite action */
    LIKE("like"),
    /** Add to cart action */
    ADD_TO_CART("add-to-cart");

    companion object {
        /**
         * Parse a click type from its string value.
         * Returns null for unrecognized values (graceful degradation).
         */
        fun fromValue(value: String?): ClickType? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}
