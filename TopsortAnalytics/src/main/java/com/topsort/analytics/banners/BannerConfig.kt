package com.topsort.analytics.banners

/**
 * Class that handles different type of Banner configurations
 */
sealed class BannerConfig private constructor() {

    /**
     * Banner configuration for landing page banners
     *
     * @property slotId id of the banner slot
     * @property ids ids of the entities that are competing for the banner
     * @property device can be "desktop" or "mobile"
     * @property geoTargeting optional location for geo-targeted banners
     */
    data class LandingPage(
        val slotId: String,
        val ids: List<String>,
        val device: String? = null,
        val geoTargeting: String? = null
    ) : BannerConfig()

    /**
     * Banner configuration for single category banners
     *
     * @property slotId id of the banner slot
     * @property category category for the banner
     * @property device can be "desktop" or "mobile"
     * @property geoTargeting optional location for geo-targeted banners
     */
    data class CategorySingle(
        val slotId: String,
        val category: String,
        val device: String? = null,
        val geoTargeting: String? = null
    ) : BannerConfig()

    /**
     * Banner config for multiple category banners
     *
     * @property slotId id of the banner slot
     * @property categories list of categories for the competing banners
     * @property device can be "desktop" or "mobile"
     * @property geoTargeting optional location for geo-targeted banners
     */
    data class CategoryMultiple(
        val slotId: String,
        val categories: List<String>,
        val device: String? = null,
        val geoTargeting: String? = null,
    ) : BannerConfig()

    /**
     * Banner configuration for category disjunctions banners
     *
     * @property slotId id of the banner slot
     * @property disjunctions  category disjunctions for the competing banners
     * @property device can be "desktop" or "mobile"
     * @property geoTargeting optional location for geo-targeted banners
     */
    data class CategoryDisjunctions(
        val slotId: String,
        val disjunctions: List<List<String>>,
        val device: String? = null,
        val geoTargeting: String? = null,
    ) : BannerConfig()

    /**
     * Banner configuration for keyword banners
     *
     * @property slotId id of the banner slot
     * @property keyword keyword for the competing banners
     * @property device can be "desktop" or "mobile"
     * @property geoTargeting optional location for geo-targeted banners
     */
    data class Keyword(
        val slotId: String,
        val keyword: String,
        val device: String? = null,
        val geoTargeting: String? = null,
    ) : BannerConfig()
}