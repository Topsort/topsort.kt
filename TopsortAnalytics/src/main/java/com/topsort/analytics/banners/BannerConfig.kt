package com.topsort.analytics.banners

sealed class BannerConfig private constructor() {
    data class LandingPage(
        val slotId: String,
        val ids: List<String>,
        val device: String? = null,
        val geoTargeting: String? = null
    ) : BannerConfig()

    data class CategorySingle(
        val slotId: String,
        val category: String,
        val device: String? = null,
        val geoTargeting: String? = null
    ) : BannerConfig()

    data class CategoryMultiple(
        val slotId: String,
        val categories: List<String>,
        val device: String? = null,
        val geoTargeting: String? = null,
    ) : BannerConfig()

    data class CategoryDisjuntions(
        val slotId: String,
        val disjunctions: List<List<String>>,
        val device: String? = null,
        val geoTargeting: String? = null,
    ) : BannerConfig()

    data class Keyword(
        val slotId: String,
        val keyword: String,
        val device: String? = null,
        val geoTargeting: String? = null,
    ) : BannerConfig()
}