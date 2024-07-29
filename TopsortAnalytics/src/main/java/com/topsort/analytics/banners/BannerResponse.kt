package com.topsort.analytics.banners

data class BannerResponse(
    val id: String,
    val url: String,
    val resolvedBidId: String,
) {}
